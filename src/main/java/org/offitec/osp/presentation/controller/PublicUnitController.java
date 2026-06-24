package org.offitec.osp.presentation.controller;

import jakarta.validation.Valid;
import org.offitec.osp.application.service.PublicUnitAppService;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.enums.UnitTypeEnum;
import org.offitec.osp.presentation.dto.CalculationRequestDTO;
import org.offitec.osp.presentation.dto.CalculationResultDTO;
import org.offitec.osp.presentation.dto.PageResponse;
import org.offitec.osp.presentation.dto.UnitCalcDataDTO;
import org.offitec.osp.presentation.dto.UnitCardDTO;
import org.offitec.osp.presentation.dto.UnitDetailPublicDTO;
import org.offitec.osp.presentation.dto.UnitMatchRequestDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    // Default page size for the catalog/saved lists. The frontend appends ?page=N to
    // load more; size is fixed server-side to keep payloads bounded.
    private static final int DEFAULT_PAGE_SIZE = 24;

    private Pageable pageOf(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0 || size > 100) ? DEFAULT_PAGE_SIZE : size;
        return PageRequest.of(safePage, safeSize);
    }

    @GetMapping("/chillers")
    public PageResponse<UnitCardDTO> getChillers(@RequestParam String type,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "24") int size) {
        UnitTypeEnum unitType = UnitTypeEnum.valueOf(type.trim().toUpperCase());
        return service.getUnitsByType(UnitCategory.CHILLER, unitType, pageOf(page, size));
    }

    @GetMapping("/heat-pumps")
    public PageResponse<UnitCardDTO> getHeatPumps(@RequestParam String type,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "24") int size) {
        UnitTypeEnum unitType = UnitTypeEnum.valueOf(type.trim().toUpperCase());
        return service.getUnitsByType(UnitCategory.HEAT_PUMP, unitType, pageOf(page, size));
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

    @PostMapping("/calculate")
    public ResponseEntity<CalculationResultDTO> calculate(@Valid @RequestBody CalculationRequestDTO dto) {
        return ResponseEntity.ok(service.calculate(dto));
    }

    // Products-page capacity match: returns the units whose computed capacity (from the
    // posted conditions + the unit's compressor polynomial) is within targetCapacity ± %.
    @PostMapping("/match")
    public List<UnitCardDTO> match(@Valid @RequestBody UnitMatchRequestDTO dto) {
        return service.matchUnits(dto);
    }

    @GetMapping("/saved")
    public PageResponse<UnitCardDTO> getSavedUnits(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        UnitCategory cat = category != null ? UnitCategory.valueOf(category.trim().toUpperCase()) : null;
        UnitTypeEnum unitType = type != null ? UnitTypeEnum.valueOf(type.trim().toUpperCase()) : null;
        return service.getSavedUnits(cat, unitType, pageOf(page, size));
    }

}
