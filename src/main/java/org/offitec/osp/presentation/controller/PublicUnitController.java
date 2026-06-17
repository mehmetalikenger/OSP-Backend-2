package org.offitec.osp.presentation.controller;

import org.offitec.osp.application.service.PublicUnitAppService;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.enums.UnitTypeEnum;
import org.offitec.osp.presentation.dto.UnitCalcDataDTO;
import org.offitec.osp.presentation.dto.UnitCardDTO;
import org.offitec.osp.presentation.dto.UnitDetailPublicDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/units")
public class PublicUnitController {

    private final PublicUnitAppService service;

    public PublicUnitController(PublicUnitAppService service) {
        this.service = service;
    }

    @GetMapping("/chillers")
    public List<UnitCardDTO> getChillers(@RequestParam String type) {
        UnitTypeEnum unitType = UnitTypeEnum.valueOf(type.trim().toUpperCase());
        return service.getUnitsByType(UnitCategory.CHILLER, unitType);
    }

    @GetMapping("/heat-pumps")
    public List<UnitCardDTO> getHeatPumps(@RequestParam String type) {
        UnitTypeEnum unitType = UnitTypeEnum.valueOf(type.trim().toUpperCase());
        return service.getUnitsByType(UnitCategory.HEAT_PUMP, unitType);
    }

    @GetMapping("/{id}")
    public UnitDetailPublicDTO getUnitDetail(@PathVariable Long id) {
        return service.getUnitDetail(id);
    }

    @GetMapping("/{id}/calc-data")
    public UnitCalcDataDTO getUnitCalcData(@PathVariable Long id) {
        return service.getUnitCalcData(id);
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<Map<String, Boolean>> toggleSave(@PathVariable Long id) {
        boolean nowSaved = service.toggleSave(id);
        return ResponseEntity.ok(Map.of("saved", nowSaved));
    }

    @GetMapping("/saved")
    public List<UnitCardDTO> getSavedUnits(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type) {
        UnitCategory cat = category != null ? UnitCategory.valueOf(category.trim().toUpperCase()) : null;
        UnitTypeEnum unitType = type != null ? UnitTypeEnum.valueOf(type.trim().toUpperCase()) : null;
        return service.getSavedUnits(cat, unitType);
    }

}
