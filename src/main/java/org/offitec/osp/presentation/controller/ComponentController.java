package org.offitec.osp.presentation.controller;

import jakarta.validation.Valid;
import org.offitec.osp.application.service.ComponentAppService;
import org.offitec.osp.presentation.dto.*;
import org.offitec.osp.domain.entity.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("admin/component")
public class ComponentController {

    private final ComponentAppService componentAppService;

    public ComponentController(ComponentAppService componentAppService) {
        this.componentAppService = componentAppService;
    }

    @PostMapping("/addCompressor")
    public HttpStatus addCompressor(@Valid @RequestBody CompressorDTO dto){

        componentAppService.addCompressor(dto);

        return HttpStatus.OK;
    }

    @GetMapping("/compressors")
    public ResponseEntity<List<Compressor>> getAllCompressors() {
        return ResponseEntity.ok(componentAppService.getAllCompressors());
    }

    @PutMapping("/editCompressor/{id}")
    public HttpStatus editCompressor(@PathVariable Long id, @Valid @RequestBody CompressorDTO dto){

        componentAppService.editCompressor(id, dto);

        return HttpStatus.OK;
    }

    // --- COMPRESSOR RATING (compressor + refrigerant coefficient set) ---

    @PostMapping("/addCompressorRating")
    public HttpStatus addCompressorRating(@Valid @RequestBody CompressorRatingDTO dto){
        componentAppService.addCompressorRating(dto);
        return HttpStatus.OK;
    }

    @PutMapping("/editCompressorRating/{ratingId}")
    public HttpStatus editCompressorRating(@PathVariable Long ratingId, @Valid @RequestBody CompressorRatingDTO dto){
        componentAppService.editCompressorRating(ratingId, dto);
        return HttpStatus.OK;
    }

    @GetMapping("/allCompressorRatings")
    public ResponseEntity<List<CompressorRatingResponseDTO>> getAllCompressorRatings() {
        return ResponseEntity.ok(componentAppService.getAllCompressorRatings());
    }

    // Upsert a rating's per-mode nominal capacity (keyed by rating + mod); used by both brands.
    @PostMapping("/addCompressorModeCapacity")
    public HttpStatus addCompressorModeCapacity(@Valid @RequestBody CompressorModeCapacityDTO dto){
        componentAppService.addCompressorModeCapacity(dto);
        return HttpStatus.OK;
    }

    @PostMapping("/addEvaporator")
    public HttpStatus addEvaporator(@Valid @RequestBody EvaporatorDTO dto){

        componentAppService.addEvaporator(dto);

        return HttpStatus.CREATED;
    }

    @PostMapping("/addEvaporatorSpecs")
    public HttpStatus addEvaporatorSpecs(@Valid @RequestBody EvaporatorSpecsDTO dto){

        componentAppService.addEvaporatorSpecs(dto);

        return HttpStatus.CREATED;
    }

    @GetMapping("/evaporators")
    public ResponseEntity<List<Evaporator>> getAllEvaporators() {
        return ResponseEntity.ok(componentAppService.getAllEvaporators());
    }

    @PutMapping("/editEvaporator/{id}")
    public HttpStatus editEvaporator(@PathVariable Long id, @Valid @RequestBody EvaporatorDTO dto){

        componentAppService.editEvaporator(id, dto);

        return HttpStatus.OK;
    }

    @PutMapping("/editEvaporatorSpecs/{specId}")
    public HttpStatus editEvaporatorSpecs(@PathVariable Long specId, @Valid @RequestBody EvaporatorSpecsDTO dto){

        componentAppService.editEvaporatorSpecs(specId, dto);

        return HttpStatus.OK;
    }

    @GetMapping("/allEvaporatorSpecs")
    public ResponseEntity<List<EvaporatorSpecsResponseDTO>> getAllEvaporatorSpecs() {
        return ResponseEntity.ok(componentAppService.getAllEvaporatorSpecs());
    }

    // --- CONDENSER ---
    @PostMapping("/addCondenser")
    public HttpStatus addCondenser(@Valid @RequestBody CondenserDTO dto){
        componentAppService.addCondenser(dto);
        return HttpStatus.CREATED;
    }

    @PostMapping("/addCondenserSpecs")
    public HttpStatus addCondenserSpecs(@Valid @RequestBody CondenserSpecsDTO dto){
        componentAppService.addCondenserSpecs(dto);
        return HttpStatus.CREATED;
    }

    @GetMapping("/condensers")
    public ResponseEntity<List<Condenser>> getAllCondensers() {
        return ResponseEntity.ok(componentAppService.getAllCondensers());
    }

    @PutMapping("/editCondenser/{id}")
    public HttpStatus editCondenser(@PathVariable Long id, @Valid @RequestBody CondenserDTO dto){
        componentAppService.editCondenser(id, dto);
        return HttpStatus.OK;
    }

    @PutMapping("/editCondenserSpecs/{specId}")
    public HttpStatus editCondenserSpecs(@PathVariable Long specId, @Valid @RequestBody CondenserSpecsDTO dto){
        componentAppService.editCondenserSpecs(specId, dto);
        return HttpStatus.OK;
    }

    @GetMapping("/allCondenserSpecs")
    public ResponseEntity<List<CondenserSpecsResponseDTO>> getAllCondenserSpecs() {
        return ResponseEntity.ok(componentAppService.getAllCondenserSpecs());
    }

    // --- EXPANSION VALVE ---
    @PostMapping("/addExpansionValve")
    public HttpStatus addExpansionValve(@Valid @RequestBody ExpansionValveDTO dto){
        componentAppService.addExpansionValve(dto);
        return HttpStatus.CREATED;
    }

    @PostMapping("/addExpansionValveSpecs")
    public HttpStatus addExpansionValveSpecs(@Valid @RequestBody ExpansionValveSpecsDTO dto){
        componentAppService.addExpansionValveSpecs(dto);
        return HttpStatus.CREATED;
    }

    @GetMapping("/expansionValves")
    public ResponseEntity<List<ExpansionValve>> getAllExpansionValves() {
        return ResponseEntity.ok(componentAppService.getAllExpansionValves());
    }

    @PutMapping("/editExpansionValve/{id}")
    public HttpStatus editExpansionValve(@PathVariable Long id, @Valid @RequestBody ExpansionValveDTO dto){
        componentAppService.editExpansionValve(id, dto);
        return HttpStatus.OK;
    }

    @PutMapping("/editExpansionValveSpecs/{specId}")
    public HttpStatus editExpansionValveSpecs(@PathVariable Long specId, @Valid @RequestBody ExpansionValveSpecsDTO dto){
        componentAppService.editExpansionValveSpecs(specId, dto);
        return HttpStatus.OK;
    }

    @GetMapping("/allExpansionValveSpecs")
    public ResponseEntity<List<ExpansionValveSpecsResponseDTO>> getAllExpansionValveSpecs() {
        return ResponseEntity.ok(componentAppService.getAllExpansionValveSpecs());
    }

    // --- 4-WAY REVERSING VALVE ---
    @PostMapping("/addFourWayReversingValve")
    public HttpStatus addFourWayReversingValve(@Valid @RequestBody FourWayReversingValveDTO dto){
        componentAppService.addFourWayReversingValve(dto);
        return HttpStatus.CREATED;
    }

    @PostMapping("/addFourWayReversingValveSpecs")
    public HttpStatus addFourWayReversingValveSpecs(@Valid @RequestBody FourWayReversingValveSpecsDTO dto){
        componentAppService.addFourWayReversingValveSpecs(dto);
        return HttpStatus.CREATED;
    }

    @GetMapping("/fourWayReversingValves")
    public ResponseEntity<List<FourWayReversingValve>> getAllFourWayReversingValves() {
        return ResponseEntity.ok(componentAppService.getAllFourWayReversingValves());
    }

    @PutMapping("/editFourWayReversingValve/{id}")
    public HttpStatus editFourWayReversingValve(@PathVariable Long id, @Valid @RequestBody FourWayReversingValveDTO dto){
        componentAppService.editFourWayReversingValve(id, dto);
        return HttpStatus.OK;
    }

    @PutMapping("/editFourWayReversingValveSpecs/{specId}")
    public HttpStatus editFourWayReversingValveSpecs(@PathVariable Long specId, @Valid @RequestBody FourWayReversingValveSpecsDTO dto){
        componentAppService.editFourWayReversingValveSpecs(specId, dto);
        return HttpStatus.OK;
    }

    @GetMapping("/allFourWayReversingValveSpecs")
    public ResponseEntity<List<FourWayReversingValveSpecsResponseDTO>> getAllFourWayReversingValveSpecs() {
        return ResponseEntity.ok(componentAppService.getAllFourWayReversingValveSpecs());
    }

    // --- CHASSIS ---
    @PostMapping("/addChassis")
    public HttpStatus addChassis(@Valid @RequestBody ChassisDTO dto){
        componentAppService.addChassis(dto);
        return HttpStatus.CREATED;
    }

    @GetMapping("/chassis")
    public ResponseEntity<List<Chassis>> getAllChassis() {
        return ResponseEntity.ok(componentAppService.getAllChassis());
    }

    @PutMapping("/editChassis/{id}")
    public HttpStatus editChassis(@PathVariable Long id, @Valid @RequestBody ChassisDTO dto){
        componentAppService.editChassis(id, dto);
        return HttpStatus.OK;
    }

    // --- REFRIGERANT ---
    @PostMapping("/addRefrigerant")
    public HttpStatus addRefrigerant(@Valid @RequestBody RefrigerantDTO dto){
        componentAppService.addRefrigerant(dto);
        return HttpStatus.CREATED;
    }

    @GetMapping("/refrigerants")
    public ResponseEntity<List<Refrigerant>> getAllRefrigerants() {
        return ResponseEntity.ok(componentAppService.getAllRefrigerants());
    }

    @PutMapping("/editRefrigerant/{id}")
    public HttpStatus editRefrigerant(@PathVariable Long id, @Valid @RequestBody RefrigerantDTO dto){
        componentAppService.editRefrigerant(id, dto);
        return HttpStatus.OK;
    }

    // --- SOFT DELETE ---

    private String currentAdminEmail() {
        return (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @DeleteMapping("/compressor/{id}")
    public HttpStatus deleteCompressor(@PathVariable Long id){
        componentAppService.deleteCompressor(id, currentAdminEmail());
        return HttpStatus.OK;
    }

    @DeleteMapping("/evaporator/{id}")
    public HttpStatus deleteEvaporator(@PathVariable Long id){
        componentAppService.deleteEvaporator(id, currentAdminEmail());
        return HttpStatus.OK;
    }

    @DeleteMapping("/condenser/{id}")
    public HttpStatus deleteCondenser(@PathVariable Long id){
        componentAppService.deleteCondenser(id, currentAdminEmail());
        return HttpStatus.OK;
    }

    @DeleteMapping("/expansionValve/{id}")
    public HttpStatus deleteExpansionValve(@PathVariable Long id){
        componentAppService.deleteExpansionValve(id, currentAdminEmail());
        return HttpStatus.OK;
    }

    @DeleteMapping("/fourWayReversingValve/{id}")
    public HttpStatus deleteFourWayReversingValve(@PathVariable Long id){
        componentAppService.deleteFourWayReversingValve(id, currentAdminEmail());
        return HttpStatus.OK;
    }

    @DeleteMapping("/chassis/{id}")
    public HttpStatus deleteChassis(@PathVariable Long id){
        componentAppService.deleteChassis(id, currentAdminEmail());
        return HttpStatus.OK;
    }

    @DeleteMapping("/refrigerant/{id}")
    public HttpStatus deleteRefrigerant(@PathVariable Long id){
        componentAppService.deleteRefrigerant(id, currentAdminEmail());
        return HttpStatus.OK;
    }
}
