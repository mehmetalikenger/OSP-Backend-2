package org.offitec.osp.presentation.controller;

import jakarta.validation.Valid;
import org.offitec.osp.application.service.UnitAppService;
import org.offitec.osp.presentation.dto.ChillerResponseDTO;
import org.offitec.osp.presentation.dto.ChillerSummaryDTO;
import org.offitec.osp.presentation.dto.ChillerWrapperDTO;
import org.offitec.osp.presentation.dto.HeatPumpDetailsWrapperDTO;
import org.offitec.osp.presentation.dto.HeatPumpModelWrapperDTO;
import org.offitec.osp.presentation.dto.HeatPumpSummaryDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/unit")
public class UnitController {

    private final UnitAppService unitAppService;

    public UnitController(UnitAppService unitAppService) {
        this.unitAppService = unitAppService;
    }

    @PostMapping("/addChiller")
    public HttpStatus addChiller(@Valid @RequestBody ChillerWrapperDTO dto){

        unitAppService.addChiller(dto);

        return HttpStatus.OK;
    }

    @GetMapping("/chillers")
    public ResponseEntity<List<ChillerSummaryDTO>> getAllChillers() {
        return ResponseEntity.ok(unitAppService.getAllChillers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChillerResponseDTO> getChiller(@PathVariable Long id) {
        return ResponseEntity.ok(unitAppService.getChiller(id));
    }

    @PutMapping("/{id}")
    public HttpStatus editChiller(@PathVariable Long id, @Valid @RequestBody ChillerWrapperDTO dto) {

        unitAppService.editUnit(id, dto);

        return HttpStatus.OK;
    }

    // --- Heat pump ---

    @PostMapping("/heat-pump")
    public HttpStatus addHeatPump(@Valid @RequestBody HeatPumpModelWrapperDTO dto) {

        unitAppService.addHeatPump(dto);

        return HttpStatus.OK;
    }

    @PostMapping("/heat-pump/details")
    public HttpStatus addHeatPumpDetails(@Valid @RequestBody HeatPumpDetailsWrapperDTO dto) {

        unitAppService.addHeatPumpDetails(dto);

        return HttpStatus.OK;
    }

    @GetMapping("/heat-pumps")
    public ResponseEntity<List<HeatPumpSummaryDTO>> getAllHeatPumps() {
        return ResponseEntity.ok(unitAppService.getAllHeatPumps());
    }
}
