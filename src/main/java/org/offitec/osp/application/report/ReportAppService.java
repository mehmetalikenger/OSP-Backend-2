package org.offitec.osp.application.report;

import org.offitec.osp.domain.entity.Project;
import org.offitec.osp.domain.entity.Unit;
import org.offitec.osp.domain.entity.User;
import org.offitec.osp.domain.enums.Mod;
import org.offitec.osp.domain.exception.UnitDoesntExistException;
import org.offitec.osp.infrastructure.repository.UnitJpaRepository;
import org.offitec.osp.infrastructure.repository.UserRepository;
import org.offitec.osp.infrastructure.storage.S3Service;
import org.offitec.osp.presentation.dto.ReportRequestDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates report generation: load the unit + current user, build the view
 * model, render the PDF. Read-only transaction so the lazy unit graph (details,
 * tech specs, components) can be navigated by the assembler.
 *
 * Plain report downloads ({@link #generate}) are NOT stored — only the add-to-project
 * flow persists a report, via {@link #renderAndStore}.
 */
@Service
public class ReportAppService {

    private final UnitJpaRepository unitRepository;
    private final UserRepository userRepository;
    private final ReportDataAssembler assembler;
    private final PdfReportService pdfReportService;
    private final S3Service s3Service;

    public ReportAppService(UnitJpaRepository unitRepository,
                            UserRepository userRepository,
                            ReportDataAssembler assembler,
                            PdfReportService pdfReportService,
                            S3Service s3Service) {
        this.unitRepository = unitRepository;
        this.userRepository = userRepository;
        this.assembler = assembler;
        this.pdfReportService = pdfReportService;
        this.s3Service = s3Service;
    }

    @Transactional(readOnly = true)
    public byte[] generate(Long unitId, ReportRequestDTO dto) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new UnitDoesntExistException("Unit not found."));

        Mod mod = Mod.valueOf(dto.getMod() == null ? "COOLING" : dto.getMod().trim().toUpperCase());
        User user = currentUser();

        UnitReportModel model = assembler.assemble(
                unit, mod, dto.getAmbient(), dto.getEvapIn(), dto.getEvapOut(), null, user,
                dto.getGlycolType(), dto.getGlycolPercentage());

        return pdfReportService.render(model);
    }

    /**
     * Renders the report for an already-loaded unit and stores it in R2, returning the
     * public URL. Used by the add-to-project flow (the only place a report is persisted);
     * runs in the caller's transaction so the URL can be saved on the ProjectDetails row.
     * The project (when present) supplies the contact/address block printed on the report.
     */
    public String renderAndStore(Unit unit, Mod mod, double ambient, double evapIn, double evapOut, Project project, User user,
                                 String glycolType, Integer glycolPercentage) {
        UnitReportModel model = assembler.assemble(unit, mod, ambient, evapIn, evapOut, project, user,
                glycolType, glycolPercentage);
        byte[] pdf = pdfReportService.render(model);

        String key = "unit-" + unit.getId() + "/" + java.util.UUID.randomUUID() + ".pdf";
        return s3Service.uploadReport(key, pdf);
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }
}
