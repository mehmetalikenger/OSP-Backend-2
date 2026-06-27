package org.offitec.osp.application.service;

import org.offitec.osp.application.report.ReportAppService;
import org.offitec.osp.application.report.ReportDataAssembler;
import org.offitec.osp.domain.entity.*;
import org.offitec.osp.domain.enums.Mod;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.exception.UnitDoesntExistException;
import org.offitec.osp.infrastructure.repository.*;
import org.offitec.osp.infrastructure.storage.S3Service;
import org.offitec.osp.presentation.dto.AddToProjectDTO;
import org.offitec.osp.presentation.dto.CreateProjectDTO;
import org.offitec.osp.presentation.dto.ProjectDTO;
import org.offitec.osp.presentation.dto.UpdateProjectDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Project management: create projects, list the current user's projects, and add
 * a unit-calculation to a project. Each added calculation becomes a ProjectDetails
 * row that links the Project, the Unit, the user's custom inputs, and the computed
 * outputs — so one project can hold many units and each knows which unit it is for.
 */
@Service
public class ProjectAppService {

    private final ProjectRepository projectRepository;
    private final ProjectDetailsRepository projectDetailsRepository;
    private final UnitJpaRepository unitRepository;
    private final UserRepository userRepository;
    private final ReportDataAssembler assembler;
    private final ReportAppService reportAppService;
    private final S3Service s3Service;

    public ProjectAppService(ProjectRepository projectRepository,
                             ProjectDetailsRepository projectDetailsRepository,
                             UnitJpaRepository unitRepository,
                             UserRepository userRepository,
                             ReportDataAssembler assembler,
                             ReportAppService reportAppService,
                             S3Service s3Service) {
        this.projectRepository = projectRepository;
        this.projectDetailsRepository = projectDetailsRepository;
        this.unitRepository = unitRepository;
        this.userRepository = userRepository;
        this.assembler = assembler;
        this.reportAppService = reportAppService;
        this.s3Service = s3Service;
    }

    @Transactional
    public ProjectDTO create(CreateProjectDTO dto) {
        User user = requireCurrentUser();
        Project project = new Project();
        project.setName(dto.getName());
        project.setCompany(dto.getCompany());
        project.setAddress(dto.getAddress());
        project.setCountry(dto.getCountry());
        project.setCity(dto.getCity());
        project.setPhone(dto.getPhone());
        project.setUser(user);
        project = projectRepository.save(project);
        return toDTO(project, List.of());
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> listMine() {
        User user = requireCurrentUser();
        List<ProjectDTO> out = new ArrayList<>();
        for (Project p : projectRepository.findByUserIdOrderByCreatedAtDesc(user.getId())) {
            out.add(toDTO(p, projectDetailsRepository.findByProjectId(p.getId())));
        }
        return out;
    }

    // Admin-only: every project across all users, flattened for the admin table. No
    // ownership scoping (the /admin/** route is already restricted to the ADMIN authority).
    @Transactional(readOnly = true)
    public List<org.offitec.osp.presentation.dto.AdminProjectRowDTO> listAllForAdmin() {
        return projectRepository.findAllAdminRows();
    }

    @Transactional(readOnly = true)
    public ProjectDTO get(Long projectId) {
        Project project = requireOwnedProject(projectId);
        return toDTO(project, projectDetailsRepository.findByProjectId(project.getId()));
    }

    @Transactional
    public ProjectDTO addUnit(Long projectId, AddToProjectDTO dto) {
        Project project = requireOwnedProject(projectId);

        Unit unit = unitRepository.findById(dto.getUnitId())
                .orElseThrow(() -> new UnitDoesntExistException("Unit not found."));

        // Not idempotent by design: the same unit may be added to one project more than
        // once (e.g. evaluated under different operating conditions), each a distinct
        // ProjectDetails row with its own inputs, outputs, and report.

        // Heat pumps are dual-mode: one detail holds both a COOLING and a HEATING row and renders
        // a single PDF covering both. Chillers hold a single COOLING row. The DTO flag must also be
        // set (the form sends the heating inputs) — otherwise we'd compute heating from zeros.
        boolean dualMode = unit.getCategory() == UnitCategory.HEAT_PUMP && dto.isDualMode();

        ProjectDetails pd = new ProjectDetails();
        pd.setProject(project);
        pd.setUnit(unit);

        // Cooling point (always present). Glycol correction applies on the chilled-water side.
        ReportDataAssembler.OpInputs coolingOp = ReportDataAssembler.OpInputs.of(
                dto.getFrequencyHz(), dto.getSubcooling(), dto.getSuperheat(), dto.getSuctionGasTemp());
        GlycolCorrection.Factors coolGf = GlycolCorrection.lookup(dto.getGlycolType(), dto.getGlycolPercentage());
        addModeRow(pd, unit, Mod.COOLING,
                dto.getAmbient(), dto.getEvapIn(), dto.getEvapOut(), 0, 0,
                dto.getEvapOut(), coolingOp, coolGf, dto.getGlycolType(), dto.getGlycolPercentage());

        // Heating point (heat pumps only). Heating heats the water — no glycol correction.
        ReportDataAssembler.OpInputs heatingOp = ReportDataAssembler.OpInputs.of(
                dto.getHeatingFrequencyHz(), dto.getHeatingSubcooling(), dto.getHeatingSuperheat(), dto.getHeatingSuctionGasTemp());
        if (dualMode) {
            addModeRow(pd, unit, Mod.HEATING,
                    dto.getHeatingAmbient(), 0, 0, dto.getHeatingWaterInlet(), dto.getHeatingWaterOutlet(),
                    dto.getHeatingWaterOutlet(), heatingOp, GlycolCorrection.Factors.NONE, null, null);
        }

        // Adding to a project is the only flow that persists a report: render it (dual-mode for heat
        // pumps), store it in R2, and keep the URL on the ProjectDetails row. The chosen language
        // drives the PDF and is stored so a later regeneration keeps the same language.
        String language = dto.getLanguage() == null ? "en" : dto.getLanguage();
        java.util.Locale locale = org.offitec.osp.application.report.ReportMessages.toLocale(language);
        String pdfUrl = dualMode
                ? reportAppService.renderAndStore(unit, dto.getAmbient(), dto.getEvapIn(), dto.getEvapOut(),
                        project, project.getUser(), dto.getGlycolType(), dto.getGlycolPercentage(), locale,
                        dto.getHeatingAmbient(), dto.getHeatingWaterInlet(), dto.getHeatingWaterOutlet(), coolingOp, heatingOp)
                : reportAppService.renderAndStore(unit, Mod.COOLING, dto.getAmbient(), dto.getEvapIn(), dto.getEvapOut(),
                        project, project.getUser(), dto.getGlycolType(), dto.getGlycolPercentage(), locale);

        pd.setLanguage(language);                // remembered so the report keeps its language
        pd.setPdfUrl(pdfUrl);
        projectDetailsRepository.save(pd);       // cascade ALL persists the value rows

        return toDTO(project, projectDetailsRepository.findByProjectId(project.getId()));
    }

    // Computes one mode's operating point the same way the PDF does and appends its input/output
    // rows to the detail. Glycol correction is applied here (NONE for heating).
    private void addModeRow(ProjectDetails pd, Unit unit, Mod mod,
                            double ambient, double evapIn, double evapOut, double condIn, double condOut,
                            double waterTemp, ReportDataAssembler.OpInputs op, GlycolCorrection.Factors gf,
                            String glycolType, Integer glycolPercentage) {
        ReportDataAssembler.OperatingPoint pt = assembler.computeOperatingPoint(unit, mod, ambient, waterTemp, op);
        double capKw = pt.capacityKw() * gf.capacity();   // heating: condenser duty; cooling: refrigerating capacity
        double powKw = pt.powerKw() * gf.power();
        double cop = powKw > 0 ? capKw / powKw : pt.copEer();
        double pressureDrop = 50.0 * gf.pressureDrop();

        CustomCalculationValues in = new CustomCalculationValues(
                null, mod, ambient, evapIn, evapOut, condIn, condOut, glycolType, glycolPercentage);
        CalculationOutputValues out = new CalculationOutputValues(
                null, mod,
                capKw,            // refrigerantCapacity (the mode's headline capacity)
                capKw,            // evaporatorCapacity
                powKw,            // powerInput
                capKw + powKw,    // condenserCapacity
                0,                // current
                cop,              // copEer
                0,                // massFlow
                0,                // operatingFrequency
                pressureDrop);    // pressureDrop (base 50 * glycol factor)

        pd.getCustomCalculationValues().add(in);
        pd.getCalculationOutputValues().add(out);
    }

    /**
     * Updates a project's name and contact/address info, then regenerates every stored
     * report so each PDF reflects the new project details. Each report is re-rendered
     * from its detail's saved custom calculation values, reproducing the exact same
     * numbers — only the project info on the report changes.
     */
    @Transactional
    public ProjectDTO update(Long projectId, UpdateProjectDTO dto) {
        Project project = requireOwnedProject(projectId);

        // Ignore a blank name so the project is never left unnamed; everything else is
        // taken as-is (an empty value clears that field).
        if (dto.getName() != null && !dto.getName().isBlank()) {
            project.setName(dto.getName().trim());
        }
        project.setAddress(dto.getAddress());
        project.setCountry(dto.getCountry());
        project.setCity(dto.getCity());
        project.setPhone(dto.getPhone());
        project = projectRepository.save(project); // @PreUpdate bumps updatedAt

        for (ProjectDetails d : projectDetailsRepository.findByProjectId(projectId)) {
            regenerateReport(project, d);
        }
        return toDTO(project, projectDetailsRepository.findByProjectId(projectId));
    }

    /**
     * Deletes a project and everything under it: each detail's stored report is removed
     * from R2, the detail rows are deleted (there's no Project->details cascade, so this is
     * explicit), then the project itself.
     */
    @Transactional
    public void delete(Long projectId) {
        Project project = requireOwnedProject(projectId);
        List<ProjectDetails> details = projectDetailsRepository.findByProjectId(projectId);
        for (ProjectDetails d : details) {
            String pdfUrl = d.getPdfUrl();
            if (pdfUrl != null && !pdfUrl.isBlank()) {
                s3Service.deleteReport(pdfUrl);
            }
        }
        projectDetailsRepository.deleteAll(details);
        projectRepository.delete(project);
    }

    // Re-renders one detail's report with the project's current info, reusing the saved per-mode
    // inputs, and swaps the stored PDF (deleting the old object only after the new one is up).
    // Heat-pump details (a COOLING + a HEATING row) regenerate the single dual-mode PDF.
    private void regenerateReport(Project project, ProjectDetails d) {
        Unit unit = d.getUnit();
        if (unit == null || d.getCustomCalculationValues().isEmpty()) return;

        // Split the stored inputs by mode (a null mod is treated as the cooling/primary row).
        CustomCalculationValues cool = null, heat = null;
        for (CustomCalculationValues in : d.getCustomCalculationValues()) {
            if (in.getMod() == Mod.HEATING) heat = in; else cool = in;
        }
        if (cool == null) cool = heat; // safety: a heating-only detail still renders

        String oldUrl = d.getPdfUrl();
        java.util.Locale locale = org.offitec.osp.application.report.ReportMessages.toLocale(d.getLanguage());
        // Reuse the stored glycol selection and language so the regenerated report recalculates with
        // the same correction and language. The faithful-engine op inputs aren't persisted, so they
        // fall back to defaults here (50 Hz / 0 K subcooling / 10 K superheat).
        String newUrl;
        if (heat != null && cool != null && cool != heat) {
            newUrl = reportAppService.renderAndStore(unit,
                    cool.getAmbient(), cool.getEvapIn(), cool.getEvapOut(), project, project.getUser(),
                    cool.getMixtureType(), cool.getMixtureRatio(), locale,
                    heat.getAmbient(), heat.getCondIn(), heat.getCondOut(),
                    ReportDataAssembler.OpInputs.of(null, null, null, null),
                    ReportDataAssembler.OpInputs.of(null, null, null, null));
        } else {
            Mod mod = cool.getMod() == null ? Mod.COOLING : cool.getMod();
            newUrl = reportAppService.renderAndStore(unit, mod,
                    cool.getAmbient(), cool.getEvapIn(), cool.getEvapOut(), project, project.getUser(),
                    cool.getMixtureType(), cool.getMixtureRatio(), locale);
        }
        d.setPdfUrl(newUrl);
        projectDetailsRepository.save(d);

        if (oldUrl != null && !oldUrl.isBlank()) {
            s3Service.deleteReport(oldUrl);
        }
    }

    // Removes a single detail row (one unit-evaluation). Because a unit can appear in a
    // project more than once, removal is by ProjectDetails id, not by unit id.
    @Transactional
    public ProjectDTO removeDetail(Long projectId, Long detailId) {
        Project project = requireOwnedProject(projectId);
        boolean[] removed = {false};
        // Scope the delete to this project so a detail id from another project can't be removed.
        projectDetailsRepository.findById(detailId)
                .filter(d -> d.getProject().getId().equals(projectId))
                .ifPresent(d -> {
                    String pdfUrl = d.getPdfUrl();
                    projectDetailsRepository.delete(d);
                    // Remove the stored report too, so it doesn't orphan in R2. Done in the
                    // transaction: if the R2 delete fails the row deletion rolls back, keeping
                    // the row and its object consistent.
                    if (pdfUrl != null && !pdfUrl.isBlank()) {
                        s3Service.deleteReport(pdfUrl);
                    }
                    removed[0] = true;
                });
        // Removing a unit changes the project, so reflect that in its update date.
        if (removed[0]) {
            project.setUpdatedAt(LocalDateTime.now());
            projectRepository.save(project);
        }
        return toDTO(project, projectDetailsRepository.findByProjectId(projectId));
    }

    // --- mapping & helpers ---

    private static final DateTimeFormatter PROJECT_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ProjectDTO toDTO(Project p, List<ProjectDetails> details) {
        Map<Long, String> imageByUnit = primaryImagesByUnit(details);

        List<ProjectDTO.ProjectDetailDTO> detailDTOs = new ArrayList<>();
        for (ProjectDetails d : details) {
            Unit u = d.getUnit();

            // Pair each mode's inputs with its outputs by mod, then emit one ModeResultDTO per mode
            // (COOLING first). A heat pump yields two; a chiller one.
            Map<Mod, CalculationOutputValues> outByMod = new HashMap<>();
            for (CalculationOutputValues o : d.getCalculationOutputValues()) outByMod.put(o.getMod(), o);

            List<ProjectDTO.ModeResultDTO> modes = new ArrayList<>();
            d.getCustomCalculationValues().stream()
                    .sorted((a, b) -> Integer.compare(modOrder(a.getMod()), modOrder(b.getMod())))
                    .forEach(in -> {
                        CalculationOutputValues out = outByMod.get(in.getMod());
                        modes.add(new ProjectDTO.ModeResultDTO(
                                in.getMod() != null ? in.getMod().name() : null,
                                in.getAmbient(), in.getEvapIn(), in.getEvapOut(), in.getCondIn(), in.getCondOut(),
                                out != null ? out.getRefrigerantCapacity() : 0,
                                out != null ? out.getPowerInput() : 0,
                                out != null ? out.getCopEer() : 0));
                    });

            detailDTOs.add(new ProjectDTO.ProjectDetailDTO(
                    d.getId(),
                    u != null ? u.getId() : null,
                    u != null ? u.getName() : null,
                    u != null ? u.getModel() : null,
                    u != null ? imageByUnit.get(u.getId()) : null,
                    modes,
                    d.getPdfUrl()));
        }
        return new ProjectDTO(
                p.getId(), p.getName(), p.getCompany(), p.getAddress(),
                p.getCountry(), p.getCity(), p.getPhone(),
                fmtDate(p.getCreatedAt()), fmtDate(p.getUpdatedAt()),
                detailDTOs);
    }

    private String fmtDate(LocalDateTime dt) {
        return dt == null ? null : dt.format(PROJECT_DATE_FMT);
    }

    // Orders modes for display: COOLING first, then HEATING, then anything unset.
    private static int modOrder(Mod mod) {
        if (mod == Mod.COOLING) return 0;
        if (mod == Mod.HEATING) return 1;
        return 2;
    }

    // Maps each detail's unit to its primary image URL (the same image the catalog/saved
    // cards show), resolved in one batched query. unit.getId() on a lazy proxy is free,
    // so this never forces the unit graph to load.
    private Map<Long, String> primaryImagesByUnit(List<ProjectDetails> details) {
        List<Long> unitIds = details.stream()
                .map(d -> d.getUnit() != null ? d.getUnit().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (unitIds.isEmpty()) return Map.of();

        Map<Long, String> byUnit = new HashMap<>();
        for (Object[] row : unitRepository.findPrimaryImageUrls(unitIds)) {
            byUnit.put((Long) row[0], (String) row[1]);
        }
        return byUnit;
    }

    private Project requireOwnedProject(Long projectId) {
        User user = requireCurrentUser();
        return projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found."));
    }

    private User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Not authenticated.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found."));
    }
}
