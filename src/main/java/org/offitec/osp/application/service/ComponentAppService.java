package org.offitec.osp.application.service;

import org.offitec.osp.domain.entity.*;
import org.offitec.osp.domain.enums.CompressorKind;
import org.offitec.osp.domain.enums.CondenserType;
import org.offitec.osp.domain.enums.EvaporatorType;
import org.offitec.osp.domain.exception.*;
import org.offitec.osp.domain.service.ComponentDomainService;
import org.offitec.osp.domain.service.AuditLogService;
import org.offitec.osp.domain.port.UserRepositoryPort;
import org.offitec.osp.infrastructure.config.EvictsUnitCaches;
import org.offitec.osp.infrastructure.repository.*;
import org.offitec.osp.presentation.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ComponentAppService {

    private final ComponentDomainService componentDomainService;
    private final CompressorRepository compressorRepository;
    private final CompressorSpecsRepository compressorSpecsRepository;
    private final EvaporatorRepository evaporatorRepository;
    private final EvaporatorSpecsRepository evaporatorSpecsRepository;
    private final CondenserRepository condenserRepository;
    private final CondenserSpecsRepository condenserSpecsRepository;
    private final ExpansionValveRepository expansionValveRepository;
    private final ExpansionValveSpecsRepository expansionValveSpecsRepository;
    private final FourWayReversingValveRepository fourWayReversingValveRepository;
    private final FourWayReversingValveSpecsRepository fourWayReversingValveSpecsRepository;
    private final ChassisRepository chassisRepository;
    private final RefrigerantRepository refrigerantRepository;
    private final AuditLogService auditLogService;
    private final UserRepositoryPort userRepositoryPort;

    public ComponentAppService(ComponentDomainService componentDomainService, CompressorRepository compressorRepository, CompressorSpecsRepository compressorSpecsRepository, EvaporatorRepository evaporatorRepository, EvaporatorSpecsRepository evaporatorSpecsRepository, CondenserRepository condenserRepository, CondenserSpecsRepository condenserSpecsRepository, ExpansionValveRepository expansionValveRepository, ExpansionValveSpecsRepository expansionValveSpecsRepository, FourWayReversingValveRepository fourWayReversingValveRepository, FourWayReversingValveSpecsRepository fourWayReversingValveSpecsRepository, ChassisRepository chassisRepository, RefrigerantRepository refrigerantRepository, AuditLogService auditLogService, UserRepositoryPort userRepositoryPort) {
        this.componentDomainService = componentDomainService;
        this.compressorRepository = compressorRepository;
        this.compressorSpecsRepository = compressorSpecsRepository;
        this.evaporatorRepository = evaporatorRepository;
        this.evaporatorSpecsRepository = evaporatorSpecsRepository;
        this.condenserRepository = condenserRepository;
        this.condenserSpecsRepository = condenserSpecsRepository;
        this.expansionValveRepository = expansionValveRepository;
        this.expansionValveSpecsRepository = expansionValveSpecsRepository;
        this.fourWayReversingValveRepository = fourWayReversingValveRepository;
        this.fourWayReversingValveSpecsRepository = fourWayReversingValveSpecsRepository;
        this.chassisRepository = chassisRepository;
        this.refrigerantRepository = refrigerantRepository;
        this.auditLogService = auditLogService;
        this.userRepositoryPort = userRepositoryPort;
    }

    @Transactional
    public void addCompressor(CompressorDTO dto){

        componentDomainService.validateUniqueModel(dto.getModel());

        Compressor compressor = new Compressor();
        compressor.setType(CompressorKind.valueOf(dto.getType().toUpperCase()));
        compressor.setBrand(dto.getBrand());
        compressor.setModel(dto.getModel());
        compressor.setMoc(dto.getMoc());
        compressor.setLra(dto.getLra());
        compressor.setRefrigerant(resolveRefrigerant(dto.getRefrigerantId()));

        compressorRepository.save(compressor);
    }

    // Resolves an optional refrigerant id to its entity (null id -> null, unknown id -> error).
    private Refrigerant resolveRefrigerant(Long refrigerantId) {
        if (refrigerantId == null) return null;
        return refrigerantRepository.findById(refrigerantId)
                .orElseThrow(() -> new RefrigerantDoesntExistException("Selected refrigerant doesn't exist."));
    }

    public List<Compressor> getAllCompressors() {
        return compressorRepository.findAll().stream().filter(x -> !x.isDeleted()).collect(Collectors.toList());
    }

    @Transactional
    public void addCompressorSpecs(CompressorSpecsDTO dto){

        Optional<Compressor> dbCompressor = compressorRepository.findById(dto.getCompressorId());

        if(dbCompressor.isEmpty()){
            throw new CompressorDoesntExistException("Compressor doesn't exist.");
        }

        Compressor compressor = dbCompressor.get();

        CompressorSpecs specs = new CompressorSpecs();
        specs.setCompressor(compressor);
        applyCompressorSpecsFields(specs, dto);

        compressorSpecsRepository.save(specs);
    }

    // Shared field copy for add/edit compressor specs (capacity/power, RPMs and all
    // capacity/power coefficients). RPMs and qC11..qC20 are ISCR-only (null otherwise).
    private void applyCompressorSpecsFields(CompressorSpecs specs, CompressorSpecsDTO dto) {
        specs.setCapacity(dto.getCapacity());
        specs.setPowerInput(dto.getPowerInput());
        specs.setRpmBase(dto.getRpmBase());
        specs.setRpmMin(dto.getRpmMin());
        specs.setRpmMax(dto.getRpmMax());
        specs.setQC1(dto.getQC1()); specs.setQC2(dto.getQC2()); specs.setQC3(dto.getQC3());
        specs.setQC4(dto.getQC4()); specs.setQC5(dto.getQC5()); specs.setQC6(dto.getQC6());
        specs.setQC7(dto.getQC7()); specs.setQC8(dto.getQC8()); specs.setQC9(dto.getQC9());
        specs.setQC10(dto.getQC10());
        specs.setQC11(dto.getQC11()); specs.setQC12(dto.getQC12()); specs.setQC13(dto.getQC13());
        specs.setQC14(dto.getQC14()); specs.setQC15(dto.getQC15()); specs.setQC16(dto.getQC16());
        specs.setQC17(dto.getQC17()); specs.setQC18(dto.getQC18()); specs.setQC19(dto.getQC19());
        specs.setQC20(dto.getQC20());
        specs.setPC1(dto.getPC1()); specs.setPC2(dto.getPC2()); specs.setPC3(dto.getPC3());
        specs.setPC4(dto.getPC4()); specs.setPC5(dto.getPC5()); specs.setPC6(dto.getPC6());
        specs.setPC7(dto.getPC7()); specs.setPC8(dto.getPC8()); specs.setPC9(dto.getPC9());
        specs.setPC10(dto.getPC10());
        specs.setPC11(dto.getPC11()); specs.setPC12(dto.getPC12()); specs.setPC13(dto.getPC13());
        specs.setPC14(dto.getPC14()); specs.setPC15(dto.getPC15()); specs.setPC16(dto.getPC16());
        specs.setPC17(dto.getPC17()); specs.setPC18(dto.getPC18()); specs.setPC19(dto.getPC19());
        specs.setPC20(dto.getPC20());
    }

    @Transactional
    @EvictsUnitCaches
    public void editCompressor(Long id, CompressorDTO dto) {

        componentDomainService.validateUniqueModelForEdit(dto.getModel(), id);

        Optional<Compressor> dbCompressor = compressorRepository.findById(id);

        if(dbCompressor.isEmpty()){
            throw new CompressorDoesntExistException("Compressor doesn't exist.");
        }

        Compressor compressor = dbCompressor.get();
        compressor.setType(CompressorKind.valueOf(dto.getType().toUpperCase()));
        compressor.setBrand(dto.getBrand());
        compressor.setModel(dto.getModel());
        compressor.setMoc(dto.getMoc());
        compressor.setLra(dto.getLra());
        compressor.setRefrigerant(resolveRefrigerant(dto.getRefrigerantId()));

        compressorRepository.save(compressor);
    }

    @Transactional
    @EvictsUnitCaches
    public void editCompressorSpecs(Long specId, CompressorSpecsDTO dto) {

        Optional<CompressorSpecs> dbSpecs = compressorSpecsRepository.findById(specId);

        if(dbSpecs.isEmpty()){
            throw new CompressorSpecsDoesntExistException("Compressor Specs doesn't exist.");
        }

        CompressorSpecs specs = dbSpecs.get();
        applyCompressorSpecsFields(specs, dto);

        compressorSpecsRepository.save(specs);
    }

    public List<CompressorSpecsResponseDTO> getAllCompressorSpecs() {
        return compressorSpecsRepository.findAll().stream().filter(specs -> specs.getCompressor() != null && !specs.getCompressor().isDeleted()).map(specs -> {
            CompressorSpecsResponseDTO dto = new CompressorSpecsResponseDTO();
            dto.setId(specs.getId());
            dto.setCapacity(specs.getCapacity());
            dto.setPowerInput(specs.getPowerInput());
            if (specs.getCompressor() != null) {
                dto.setBrand(specs.getCompressor().getBrand());
                dto.setModel(specs.getCompressor().getModel());
                dto.setType(specs.getCompressor().getType().name());
                dto.setRefrigerantId(specs.getCompressor().getRefrigerant() != null
                        ? specs.getCompressor().getRefrigerant().getId() : null);
            }
            dto.setRpmBase(specs.getRpmBase());
            dto.setRpmMin(specs.getRpmMin());
            dto.setRpmMax(specs.getRpmMax());
            dto.setQC1(specs.getQC1()); dto.setQC2(specs.getQC2()); dto.setQC3(specs.getQC3());
            dto.setQC4(specs.getQC4()); dto.setQC5(specs.getQC5()); dto.setQC6(specs.getQC6());
            dto.setQC7(specs.getQC7()); dto.setQC8(specs.getQC8()); dto.setQC9(specs.getQC9());
            dto.setQC10(specs.getQC10());
            dto.setQC11(specs.getQC11()); dto.setQC12(specs.getQC12()); dto.setQC13(specs.getQC13());
            dto.setQC14(specs.getQC14()); dto.setQC15(specs.getQC15()); dto.setQC16(specs.getQC16());
            dto.setQC17(specs.getQC17()); dto.setQC18(specs.getQC18()); dto.setQC19(specs.getQC19());
            dto.setQC20(specs.getQC20());
            dto.setPC1(specs.getPC1()); dto.setPC2(specs.getPC2()); dto.setPC3(specs.getPC3());
            dto.setPC4(specs.getPC4()); dto.setPC5(specs.getPC5()); dto.setPC6(specs.getPC6());
            dto.setPC7(specs.getPC7()); dto.setPC8(specs.getPC8()); dto.setPC9(specs.getPC9());
            dto.setPC10(specs.getPC10());
            dto.setPC11(specs.getPC11()); dto.setPC12(specs.getPC12()); dto.setPC13(specs.getPC13());
            dto.setPC14(specs.getPC14()); dto.setPC15(specs.getPC15()); dto.setPC16(specs.getPC16());
            dto.setPC17(specs.getPC17()); dto.setPC18(specs.getPC18()); dto.setPC19(specs.getPC19());
            dto.setPC20(specs.getPC20());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void addEvaporator(EvaporatorDTO dto) {

        componentDomainService.validateUniqueModelForEvaporator(dto.getModel());

        Evaporator evaporator = new Evaporator();
        evaporator.setBrand(dto.getBrand());
        evaporator.setModel(dto.getModel());
        evaporator.setType(parseEvaporatorType(dto.getType()));

        evaporatorRepository.save(evaporator);
    }

    @Transactional
    public void addEvaporatorSpecs(EvaporatorSpecsDTO dto) {

        Optional<Evaporator> dbEvaporator = evaporatorRepository.findById(dto.getEvaporatorId());

        if(dbEvaporator.isEmpty()){
            throw new EvaporatorDoesntExistException("Evaporator doesn't exist.");
        }

        EvaporatorSpecs specs = new EvaporatorSpecs();
        specs.setEvaporator(dbEvaporator.get());
        specs.setCapacity(dto.getCapacity());

        evaporatorSpecsRepository.save(specs);
    }

    public List<Evaporator> getAllEvaporators() {
        return evaporatorRepository.findAll().stream().filter(x -> !x.isDeleted()).collect(Collectors.toList());
    }

    @Transactional
    @EvictsUnitCaches
    public void editEvaporator(Long id, EvaporatorDTO dto) {

        componentDomainService.validateUniqueModelForEditEvaporator(dto.getModel(), id);

        Optional<Evaporator> dbEvaporator = evaporatorRepository.findById(id);

        if(dbEvaporator.isEmpty()){
            throw new EvaporatorDoesntExistException("Evaporator doesn't exist.");
        }

        Evaporator evaporator = dbEvaporator.get();
        evaporator.setBrand(dto.getBrand());
        evaporator.setModel(dto.getModel());
        evaporator.setType(parseEvaporatorType(dto.getType()));

        evaporatorRepository.save(evaporator);
    }

    @Transactional
    @EvictsUnitCaches
    public void editEvaporatorSpecs(Long specId, EvaporatorSpecsDTO dto) {

        Optional<EvaporatorSpecs> dbSpecs = evaporatorSpecsRepository.findById(specId);

        if(dbSpecs.isEmpty()){
            throw new EvaporatorSpecsDoesntExistException("Evaporator Specs doesn't exist.");
        }

        EvaporatorSpecs specs = dbSpecs.get();
        specs.setCapacity(dto.getCapacity());

        evaporatorSpecsRepository.save(specs);
    }

    public List<EvaporatorSpecsResponseDTO> getAllEvaporatorSpecs() {
        return evaporatorSpecsRepository.findAll().stream().filter(specs -> specs.getEvaporator() != null && !specs.getEvaporator().isDeleted()).map(specs -> {
            EvaporatorSpecsResponseDTO dto = new EvaporatorSpecsResponseDTO();
            dto.setId(specs.getId());
            dto.setCapacity(specs.getCapacity());
            if (specs.getEvaporator() != null) {
                dto.setBrand(specs.getEvaporator().getBrand());
                dto.setModel(specs.getEvaporator().getModel());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    // --- CONDENSER ---
    @Transactional
    public void addCondenser(CondenserDTO dto) {
        componentDomainService.validateUniqueModelForCondenser(dto.getModel());
        Condenser condenser = new Condenser();
        condenser.setBrand(dto.getBrand());
        condenser.setModel(dto.getModel());
        condenser.setType(parseCondenserType(dto.getType()));
        condenserRepository.save(condenser);
    }

    @Transactional
    public void addCondenserSpecs(CondenserSpecsDTO dto) {
        Optional<Condenser> dbCondenser = condenserRepository.findById(dto.getCondenserId());
        if(dbCondenser.isEmpty()){
            throw new CondenserDoesntExistException("Condenser doesn't exist.");
        }
        CondenserSpecs specs = new CondenserSpecs();
        specs.setCondenser(dbCondenser.get());
        specs.setCapacity(dto.getCapacity());
        condenserSpecsRepository.save(specs);
    }

    public List<Condenser> getAllCondensers() {
        return condenserRepository.findAll().stream().filter(x -> !x.isDeleted()).collect(Collectors.toList());
    }

    @Transactional
    @EvictsUnitCaches
    public void editCondenser(Long id, CondenserDTO dto) {
        componentDomainService.validateUniqueModelForEditCondenser(dto.getModel(), id);
        Optional<Condenser> dbCondenser = condenserRepository.findById(id);
        if(dbCondenser.isEmpty()){
            throw new CondenserDoesntExistException("Condenser doesn't exist.");
        }
        Condenser condenser = dbCondenser.get();
        condenser.setBrand(dto.getBrand());
        condenser.setModel(dto.getModel());
        condenser.setType(parseCondenserType(dto.getType()));
        condenserRepository.save(condenser);
    }

    @Transactional
    @EvictsUnitCaches
    public void editCondenserSpecs(Long specId, CondenserSpecsDTO dto) {
        Optional<CondenserSpecs> dbSpecs = condenserSpecsRepository.findById(specId);
        if(dbSpecs.isEmpty()){
            throw new CondenserSpecsDoesntExistException("Condenser Specs doesn't exist.");
        }
        CondenserSpecs specs = dbSpecs.get();
        specs.setCapacity(dto.getCapacity());
        condenserSpecsRepository.save(specs);
    }

    public List<CondenserSpecsResponseDTO> getAllCondenserSpecs() {
        return condenserSpecsRepository.findAll().stream().filter(specs -> specs.getCondenser() != null && !specs.getCondenser().isDeleted()).map(specs -> {
            CondenserSpecsResponseDTO dto = new CondenserSpecsResponseDTO();
            dto.setId(specs.getId());
            dto.setCapacity(specs.getCapacity());
            if (specs.getCondenser() != null) {
                dto.setBrand(specs.getCondenser().getBrand());
                dto.setModel(specs.getCondenser().getModel());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    // --- EXPANSION VALVE ---
    @Transactional
    public void addExpansionValve(ExpansionValveDTO dto) {
        componentDomainService.validateUniqueModelForExpansionValve(dto.getModel());
        ExpansionValve expansionValve = new ExpansionValve();
        expansionValve.setBrand(dto.getBrand());
        expansionValve.setModel(dto.getModel());
        expansionValveRepository.save(expansionValve);
    }

    @Transactional
    public void addExpansionValveSpecs(ExpansionValveSpecsDTO dto) {
        Optional<ExpansionValve> dbExpansionValve = expansionValveRepository.findById(dto.getExpansionValveId());
        if(dbExpansionValve.isEmpty()){
            throw new ExpansionValveDoesntExistException("Expansion Valve doesn't exist.");
        }
        ExpansionValveSpecs specs = new ExpansionValveSpecs();
        specs.setExpansionValve(dbExpansionValve.get());
        specs.setCapacity(dto.getCapacity());
        expansionValveSpecsRepository.save(specs);
    }

    public List<ExpansionValve> getAllExpansionValves() {
        return expansionValveRepository.findAll().stream().filter(x -> !x.isDeleted()).collect(Collectors.toList());
    }

    @Transactional
    @EvictsUnitCaches
    public void editExpansionValve(Long id, ExpansionValveDTO dto) {
        componentDomainService.validateUniqueModelForEditExpansionValve(dto.getModel(), id);
        Optional<ExpansionValve> dbExpansionValve = expansionValveRepository.findById(id);
        if(dbExpansionValve.isEmpty()){
            throw new ExpansionValveDoesntExistException("Expansion Valve doesn't exist.");
        }
        ExpansionValve expansionValve = dbExpansionValve.get();
        expansionValve.setBrand(dto.getBrand());
        expansionValve.setModel(dto.getModel());
        expansionValveRepository.save(expansionValve);
    }

    @Transactional
    @EvictsUnitCaches
    public void editExpansionValveSpecs(Long specId, ExpansionValveSpecsDTO dto) {
        Optional<ExpansionValveSpecs> dbSpecs = expansionValveSpecsRepository.findById(specId);
        if(dbSpecs.isEmpty()){
            throw new ExpansionValveSpecsDoesntExistException("Expansion Valve Specs doesn't exist.");
        }
        ExpansionValveSpecs specs = dbSpecs.get();
        specs.setCapacity(dto.getCapacity());
        expansionValveSpecsRepository.save(specs);
    }

    public List<ExpansionValveSpecsResponseDTO> getAllExpansionValveSpecs() {
        return expansionValveSpecsRepository.findAll().stream().filter(specs -> specs.getExpansionValve() != null && !specs.getExpansionValve().isDeleted()).map(specs -> {
            ExpansionValveSpecsResponseDTO dto = new ExpansionValveSpecsResponseDTO();
            dto.setId(specs.getId());
            dto.setCapacity(specs.getCapacity());
            if (specs.getExpansionValve() != null) {
                dto.setBrand(specs.getExpansionValve().getBrand());
                dto.setModel(specs.getExpansionValve().getModel());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    // --- 4-WAY REVERSING VALVE ---
    @Transactional
    public void addFourWayReversingValve(FourWayReversingValveDTO dto) {
        componentDomainService.validateUniqueModelForFourWayReversingValve(dto.getModel());
        FourWayReversingValve valve = new FourWayReversingValve();
        valve.setBrand(dto.getBrand());
        valve.setModel(dto.getModel());
        fourWayReversingValveRepository.save(valve);
    }

    @Transactional
    public void addFourWayReversingValveSpecs(FourWayReversingValveSpecsDTO dto) {
        Optional<FourWayReversingValve> dbValve = fourWayReversingValveRepository.findById(dto.getFourWayReversingValveId());
        if(dbValve.isEmpty()){
            throw new FourWayReversingValveDoesntExistException("4-Way Reversing Valve doesn't exist.");
        }
        FourWayReversingValveSpecs specs = new FourWayReversingValveSpecs();
        specs.setFourWayReversingValve(dbValve.get());
        specs.setCapacity(dto.getCapacity());
        fourWayReversingValveSpecsRepository.save(specs);
    }

    public List<FourWayReversingValve> getAllFourWayReversingValves() {
        return fourWayReversingValveRepository.findAll().stream().filter(x -> !x.isDeleted()).collect(Collectors.toList());
    }

    @Transactional
    @EvictsUnitCaches
    public void editFourWayReversingValve(Long id, FourWayReversingValveDTO dto) {
        componentDomainService.validateUniqueModelForEditFourWayReversingValve(dto.getModel(), id);
        Optional<FourWayReversingValve> dbValve = fourWayReversingValveRepository.findById(id);
        if(dbValve.isEmpty()){
            throw new FourWayReversingValveDoesntExistException("4-Way Reversing Valve doesn't exist.");
        }
        FourWayReversingValve valve = dbValve.get();
        valve.setBrand(dto.getBrand());
        valve.setModel(dto.getModel());
        fourWayReversingValveRepository.save(valve);
    }

    @Transactional
    @EvictsUnitCaches
    public void editFourWayReversingValveSpecs(Long specId, FourWayReversingValveSpecsDTO dto) {
        Optional<FourWayReversingValveSpecs> dbSpecs = fourWayReversingValveSpecsRepository.findById(specId);
        if(dbSpecs.isEmpty()){
            throw new FourWayReversingValveSpecsDoesntExistException("4-Way Reversing Valve Specs doesn't exist.");
        }
        FourWayReversingValveSpecs specs = dbSpecs.get();
        specs.setCapacity(dto.getCapacity());
        fourWayReversingValveSpecsRepository.save(specs);
    }

    public List<FourWayReversingValveSpecsResponseDTO> getAllFourWayReversingValveSpecs() {
        return fourWayReversingValveSpecsRepository.findAll().stream().filter(specs -> specs.getFourWayReversingValve() != null && !specs.getFourWayReversingValve().isDeleted()).map(specs -> {
            FourWayReversingValveSpecsResponseDTO dto = new FourWayReversingValveSpecsResponseDTO();
            dto.setId(specs.getId());
            dto.setCapacity(specs.getCapacity());
            if (specs.getFourWayReversingValve() != null) {
                dto.setBrand(specs.getFourWayReversingValve().getBrand());
                dto.setModel(specs.getFourWayReversingValve().getModel());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    // --- CHASSIS ---
    @Transactional
    public void addChassis(ChassisDTO dto) {
        componentDomainService.validateUniqueModelForChassis(dto.getModel());
        Chassis chassis = new Chassis();
        chassis.setBrand(dto.getBrand());
        chassis.setModel(dto.getModel());
        chassisRepository.save(chassis);
    }

    public List<Chassis> getAllChassis() {
        return chassisRepository.findAll().stream().filter(x -> !x.isDeleted()).collect(Collectors.toList());
    }

    @Transactional
    @EvictsUnitCaches
    public void editChassis(Long id, ChassisDTO dto) {
        componentDomainService.validateUniqueModelForEditChassis(dto.getModel(), id);
        Optional<Chassis> dbChassis = chassisRepository.findById(id);
        if(dbChassis.isEmpty()){
            throw new ChassisDoesntExistException("Chassis doesn't exist.");
        }
        Chassis chassis = dbChassis.get();
        chassis.setBrand(dto.getBrand());
        chassis.setModel(dto.getModel());
        chassisRepository.save(chassis);
    }

    // --- REFRIGERANT ---
    @Transactional
    public void addRefrigerant(RefrigerantDTO dto) {
        componentDomainService.validateUniqueCodeForRefrigerant(dto.getCode());
        Refrigerant refrigerant = new Refrigerant();
        refrigerant.setName(dto.getName());
        refrigerant.setCode(dto.getCode());
        refrigerantRepository.save(refrigerant);
    }

    public List<Refrigerant> getAllRefrigerants() {
        return refrigerantRepository.findAll().stream().filter(x -> !x.isDeleted()).collect(Collectors.toList());
    }

    @Transactional
    @EvictsUnitCaches
    public void editRefrigerant(Long id, RefrigerantDTO dto) {
        componentDomainService.validateUniqueCodeForEditRefrigerant(dto.getCode(), id);
        Optional<Refrigerant> dbRefrigerant = refrigerantRepository.findById(id);
        if(dbRefrigerant.isEmpty()){
            throw new RefrigerantDoesntExistException("Refrigerant doesn't exist.");
        }
        Refrigerant refrigerant = dbRefrigerant.get();
        refrigerant.setName(dto.getName());
        refrigerant.setCode(dto.getCode());
        refrigerantRepository.save(refrigerant);
    }

    // --- SOFT DELETE ---
    // Components are never hard-deleted: existing units reference their specs, so we
    // flag them deleted (hidden from all listings) and record the action in the audit log.

    private Long resolveAdminId(String adminEmail) {
        if (adminEmail == null) return -1L;
        return userRepositoryPort.findByEmail(adminEmail).map(User::getId).orElse(-1L);
    }

    // Null/blank-safe enum parsing for the optional component "type" fields.
    private static CondenserType parseCondenserType(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return CondenserType.valueOf(raw.trim().toUpperCase());
    }

    private static EvaporatorType parseEvaporatorType(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return EvaporatorType.valueOf(raw.trim().toUpperCase());
    }

    @Transactional
    @EvictsUnitCaches
    public void deleteCompressor(Long id, String adminEmail) {
        Compressor c = compressorRepository.findById(id)
                .orElseThrow(() -> new CompressorDoesntExistException("Compressor doesn't exist."));
        c.setDeleted(true);
        compressorRepository.save(c);
        auditLogService.logAdminAction(resolveAdminId(adminEmail), "DELETE", "COMPRESSOR", id, "Deleted compressor " + c.getModel());
    }

    @Transactional
    @EvictsUnitCaches
    public void deleteEvaporator(Long id, String adminEmail) {
        Evaporator e = evaporatorRepository.findById(id)
                .orElseThrow(() -> new EvaporatorDoesntExistException("Evaporator doesn't exist."));
        e.setDeleted(true);
        evaporatorRepository.save(e);
        auditLogService.logAdminAction(resolveAdminId(adminEmail), "DELETE", "EVAPORATOR", id, "Deleted evaporator " + e.getModel());
    }

    @Transactional
    @EvictsUnitCaches
    public void deleteCondenser(Long id, String adminEmail) {
        Condenser c = condenserRepository.findById(id)
                .orElseThrow(() -> new CondenserDoesntExistException("Condenser doesn't exist."));
        c.setDeleted(true);
        condenserRepository.save(c);
        auditLogService.logAdminAction(resolveAdminId(adminEmail), "DELETE", "CONDENSER", id, "Deleted condenser " + c.getModel());
    }

    @Transactional
    @EvictsUnitCaches
    public void deleteExpansionValve(Long id, String adminEmail) {
        ExpansionValve ev = expansionValveRepository.findById(id)
                .orElseThrow(() -> new ExpansionValveDoesntExistException("Expansion Valve doesn't exist."));
        ev.setDeleted(true);
        expansionValveRepository.save(ev);
        auditLogService.logAdminAction(resolveAdminId(adminEmail), "DELETE", "EXPANSION_VALVE", id, "Deleted expansion valve " + ev.getModel());
    }

    @Transactional
    @EvictsUnitCaches
    public void deleteFourWayReversingValve(Long id, String adminEmail) {
        FourWayReversingValve v = fourWayReversingValveRepository.findById(id)
                .orElseThrow(() -> new FourWayReversingValveDoesntExistException("4-Way Reversing Valve doesn't exist."));
        v.setDeleted(true);
        fourWayReversingValveRepository.save(v);
        auditLogService.logAdminAction(resolveAdminId(adminEmail), "DELETE", "FOUR_WAY_REVERSING_VALVE", id, "Deleted 4-way reversing valve " + v.getModel());
    }

    @Transactional
    @EvictsUnitCaches
    public void deleteChassis(Long id, String adminEmail) {
        Chassis ch = chassisRepository.findById(id)
                .orElseThrow(() -> new ChassisDoesntExistException("Chassis doesn't exist."));
        ch.setDeleted(true);
        chassisRepository.save(ch);
        auditLogService.logAdminAction(resolveAdminId(adminEmail), "DELETE", "CHASSIS", id, "Deleted chassis " + ch.getModel());
    }

    @Transactional
    @EvictsUnitCaches
    public void deleteRefrigerant(Long id, String adminEmail) {
        Refrigerant r = refrigerantRepository.findById(id)
                .orElseThrow(() -> new RefrigerantDoesntExistException("Refrigerant doesn't exist."));
        r.setDeleted(true);
        refrigerantRepository.save(r);
        auditLogService.logAdminAction(resolveAdminId(adminEmail), "DELETE", "REFRIGERANT", id, "Deleted refrigerant " + r.getCode());
    }
}
