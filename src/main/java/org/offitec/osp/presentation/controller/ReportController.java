package org.offitec.osp.presentation.controller;

import org.offitec.osp.application.report.ReportAppService;
import org.offitec.osp.presentation.dto.ReportRequestDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Generates and streams the unit selection PDF report. The browser sends the
 * calculation inputs and receives a finished PDF — all rendering happens here.
 */
@RestController
@RequestMapping("/units")
public class ReportController {

    private final ReportAppService reportAppService;

    public ReportController(ReportAppService reportAppService) {
        this.reportAppService = reportAppService;
    }

    @PostMapping(value = "/{id}/report", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateReport(@PathVariable Long id,
                                                 @RequestBody ReportRequestDTO dto) {
        byte[] pdf = reportAppService.generate(id, dto);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"unit-report.pdf\"")
                .body(pdf);
    }
}
