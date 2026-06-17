package org.offitec.osp.presentation.controller;

import jakarta.validation.Valid;
import org.offitec.osp.application.service.UnitAppService;
import org.offitec.osp.presentation.dto.ChillerResponseDTO;
import org.offitec.osp.presentation.dto.ChillerSummaryDTO;
import org.offitec.osp.presentation.dto.ChillerWrapperDTO;
import org.offitec.osp.presentation.dto.AssetUploadDTO;
import org.offitec.osp.presentation.dto.HeatPumpDetailsWrapperDTO;
import org.offitec.osp.presentation.dto.HeatPumpModelWrapperDTO;
import org.offitec.osp.presentation.dto.HeatPumpResponseDTO;
import org.offitec.osp.presentation.dto.HeatPumpSummaryDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/unit")
public class UnitController {

    private final UnitAppService unitAppService;

    public UnitController(UnitAppService unitAppService) {
        this.unitAppService = unitAppService;
    }

    @PostMapping("/addChiller")
    public ResponseEntity<Long> addChiller(@Valid @RequestBody ChillerWrapperDTO dto) {
        return ResponseEntity.ok(unitAppService.addChiller(dto));
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
    public ResponseEntity<Long> addHeatPump(@Valid @RequestBody HeatPumpModelWrapperDTO dto) {
        return ResponseEntity.ok(unitAppService.addHeatPump(dto));
    }

    @PostMapping(value = "/{unitId}/upload-assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public HttpStatus uploadAssets(@PathVariable Long unitId, @ModelAttribute AssetUploadDTO dto) {
        unitAppService.uploadAssets(unitId, dto);
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

    @GetMapping("/heat-pump/{id}")
    public ResponseEntity<HeatPumpResponseDTO> getHeatPump(@PathVariable Long id) {
        return ResponseEntity.ok(unitAppService.getHeatPump(id));
    }

    @PutMapping("/heat-pump/{id}")
    public HttpStatus editHeatPump(@PathVariable Long id, @Valid @RequestBody HeatPumpModelWrapperDTO dto) {

        unitAppService.editHeatPump(id, dto);

        return HttpStatus.OK;
    }

    @PutMapping("/heat-pump/details")
    public HttpStatus editHeatPumpDetails(@Valid @RequestBody HeatPumpDetailsWrapperDTO dto) {

        unitAppService.editHeatPumpDetails(dto);

        return HttpStatus.OK;
    }

    // --- Asset management ---

    @DeleteMapping("/asset/{assetId}")
    public HttpStatus deleteAsset(@PathVariable Long assetId) {
        unitAppService.deleteAsset(assetId);
        return HttpStatus.OK;
    }

    @PutMapping("/asset/{assetId}/primary")
    public HttpStatus setPrimaryAsset(@PathVariable Long assetId) {
        unitAppService.setPrimaryAsset(assetId);
        return HttpStatus.OK;
    }
}
