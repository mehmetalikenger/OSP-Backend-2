package org.offitec.osp.application.service;

import org.offitec.osp.application.report.ReportAppService;
import org.offitec.osp.domain.entity.*;
import org.offitec.osp.domain.enums.Mod;
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
    private final UnitDetailsRepository unitDetailsRepository;
    private final UserRepository userRepository;
    private final UnitCalculationEngine engine;
    private final ReportAppService reportAppService;
    private final S3Service s3Service;

    public ProjectAppService(ProjectRepository projectRepository,
                             ProjectDetailsRepository projectDetailsRepository,
                             UnitJpaRepository unitRepository,
                             UnitDetailsRepository unitDetailsRepository,
                             UserRepository userRepository,
                             UnitCalculationEngine engine,
                             ReportAppService reportAppService,
                             S3Service s3Service) {
        this.projectRepository = projectRepository;
        this.projectDetailsRepository = projectDetailsRepository;
        this.unitRepository = unitRepository;
        this.unitDetailsRepository = unitDetailsRepository;
        this.userRepository = userRepository;
        this.engine = engine;
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

        Mod mod = Mod.valueOf(dto.getMod() == null ? "COOLING" : dto.getMod().trim().toUpperCase());
        UnitDetails details = unitDetailsRepository.findByUnitIdAndMod(unit.getId(), mod)
                .orElseThrow(() -> new IllegalStateException("Unit details not found for mod: " + mod));
        TechSpecs ts = details.getTechSpecs();
        if (ts == null || ts.getCompressorSpecs() == null) {
            throw new IllegalStateException("Compressor specs not configured for this unit.");
        }

        UnitCalculationEngine.Result r =
                engine.compute(ts.getCompressorSpecs(), unit.getCompressorQty(), dto.getAmbient(), dto.getEvapOut());

        CustomCalculationValues inputs = new CustomCalculationValues(
                null, dto.getAmbient(), dto.getEvapIn(), dto.getEvapOut(), dto.getCondIn(), dto.getCondOut());

        CalculationOutputValues outputs = new CalculationOutputValues(
                null,
                r.capacityKw(),   // refrigerantCapacity
                r.capacityKw(),   // evaporatorCapacity
                r.powerKw(),      // powerInput
                r.capacityKw() + r.powerKw(), // condenserCapacity
                0,                // current
                r.copEer(),       // copEer
                0,                // massFlow
                0);               // operatingFrequency

        // Adding to a project is the only flow that persists a report: render it,
        // store it in R2, and keep the URL on the ProjectDetails row.
        String pdfUrl = reportAppService.renderAndStore(
                unit, mod, dto.getAmbient(), dto.getEvapIn(), dto.getEvapOut(), project, project.getUser());

        ProjectDetails pd = new ProjectDetails();
        pd.setProject(project);
        pd.setUnit(unit);
        pd.setCustomCalculationValues(inputs);   // cascade ALL persists these
        pd.setCalculationOutputValues(outputs);
        pd.setMod(mod);                          // remembered so the report can be regenerated
        pd.setPdfUrl(pdfUrl);
        projectDetailsRepository.save(pd);

        return toDTO(project, projectDetailsRepository.findByProjectId(project.getId()));
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

    // Re-renders one detail's report with the project's current info, reusing the saved
    // inputs, and swaps the stored PDF (deleting the old object only after the new one is up).
    private void regenerateReport(Project project, ProjectDetails d) {
        Unit unit = d.getUnit();
        CustomCalculationValues in = d.getCustomCalculationValues();
        if (unit == null || in == null) return;

        Mod mod = d.getMod() == null ? Mod.COOLING : d.getMod();
        String oldUrl = d.getPdfUrl();
        String newUrl = reportAppService.renderAndStore(
                unit, mod, in.getAmbient(), in.getEvapIn(), in.getEvapOut(), project, project.getUser());
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
            CustomCalculationValues in = d.getCustomCalculationValues();
            CalculationOutputValues out = d.getCalculationOutputValues();
            Unit u = d.getUnit();
            detailDTOs.add(new ProjectDTO.ProjectDetailDTO(
                    d.getId(),
                    u != null ? u.getId() : null,
                    u != null ? u.getName() : null,
                    u != null ? u.getModel() : null,
                    u != null ? imageByUnit.get(u.getId()) : null,
                    in != null ? in.getAmbient() : 0,
                    in != null ? in.getEvapIn() : 0,
                    in != null ? in.getEvapOut() : 0,
                    out != null ? out.getRefrigerantCapacity() : 0,
                    out != null ? out.getPowerInput() : 0,
                    out != null ? out.getCopEer() : 0,
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
