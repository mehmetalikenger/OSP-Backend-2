package org.offitec.osp.presentation.controller;

import jakarta.validation.Valid;
import org.offitec.osp.application.service.ProjectAppService;
import org.offitec.osp.presentation.dto.AddToProjectDTO;
import org.offitec.osp.presentation.dto.CreateProjectDTO;
import org.offitec.osp.presentation.dto.ProjectDTO;
import org.offitec.osp.presentation.dto.UpdateProjectDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectAppService service;

    public ProjectController(ProjectAppService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProjectDTO> listMine() {
        return service.listMine();
    }

    @GetMapping("/{id}")
    public ProjectDTO get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<ProjectDTO> create(@Valid @RequestBody CreateProjectDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDTO> update(@PathVariable Long id,
                                             @RequestBody UpdateProjectDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/details")
    public ResponseEntity<ProjectDTO> addUnit(@PathVariable Long id,
                                              @Valid @RequestBody AddToProjectDTO dto) {
        return ResponseEntity.ok(service.addUnit(id, dto));
    }

    @DeleteMapping("/{id}/details/{detailId}")
    public ResponseEntity<ProjectDTO> removeDetail(@PathVariable Long id, @PathVariable Long detailId) {
        return ResponseEntity.ok(service.removeDetail(id, detailId));
    }
}
