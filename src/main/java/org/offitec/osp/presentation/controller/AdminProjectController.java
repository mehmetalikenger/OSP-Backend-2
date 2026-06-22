package org.offitec.osp.presentation.controller;

import org.offitec.osp.application.service.ProjectAppService;
import org.offitec.osp.presentation.dto.AdminProjectRowDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin view of all users' projects. Lives under /admin/** so it's restricted to the
 * ADMIN authority by SecurityConfig.
 */
@RestController
@RequestMapping("/admin/projects")
public class AdminProjectController {

    private final ProjectAppService service;

    public AdminProjectController(ProjectAppService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminProjectRowDTO> listAll() {
        return service.listAllForAdmin();
    }
}
