package org.offitec.osp.application.service;

import org.offitec.osp.domain.entity.*;
import org.offitec.osp.domain.enums.CompressorKind;
import org.offitec.osp.domain.exception.*;
import org.offitec.osp.domain.service.ComponentDomainService;
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

    public ComponentAppService(ComponentDomainService componentDomainService, CompressorRepository compressorRepository, CompressorSpecsRepository compressorSpecsRepository, EvaporatorRepository evaporatorRepository, EvaporatorSpecsRepository evaporatorSpecsRepository, CondenserRepository condenserRepository, CondenserSpecsRepository condenserSpecsRepository, ExpansionValveRepository expansionValveRepository, ExpansionValveSpecsRepository expansionValveSpecsRepository, FourWayReversingValveRepository fourWayReversingValveRepository, FourWayReversingValveSpecsRepository fourWayReversingValveSpecsRepository, ChassisRepository chassisRepository, RefrigerantRepository refrigerantRepository) {
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
    }

    @Transactional
    public void addCompressor(CompressorDTO dto){

        componentDomainService.validateUniqueModel(dto.getModel());

        Compressor compressor = new Compressor();
        compressor.setType(CompressorKind.valueOf(dto.getType().toUpperCase()));
        compressor.setBrand(dto.getBrand());
        compressor.setModel(dto.getModel());

        compressorRepository.save(compressor);
    }

    public List<Compressor> getAllCompressors() {
        return compressorRepository.findAll();
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
        specs.setCapacity(dto.getCapacity());
        specs.setPowerInput(dto.getPowerInput());
        specs.setQC1(dto.getQC1()); specs.setQC2(dto.getQC2()); specs.setQC3(dto.getQC3());
        specs.setQC4(dto.getQC4()); specs.setQC5(dto.getQC5()); specs.setQC6(dto.getQC6());
        specs.setQC7(dto.getQC7()); specs.setQC8(dto.getQC8()); specs.setQC9(dto.getQC9());
        specs.setQC10(dto.getQC10());
        specs.setPC1(dto.getPC1()); specs.setPC2(dto.getPC2()); specs.setPC3(dto.getPC3());
        specs.setPC4(dto.getPC4()); specs.setPC5(dto.getPC5()); specs.setPC6(dto.getPC6());
        specs.setPC7(dto.getPC7()); specs.setPC8(dto.getPC8()); specs.setPC9(dto.getPC9());
        specs.setPC10(dto.getPC10());

        compressorSpecsRepository.save(specs);
    }

    @Transactional
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

        compressorRepository.save(compressor);
    }

    @Transactional
    public void editCompressorSpecs(Long specId, CompressorSpecsDTO dto) {

        Optional<CompressorSpecs> dbSpecs = compressorSpecsRepository.findById(specId);

        if(dbSpecs.isEmpty()){
            throw new CompressorSpecsDoesntExistException("Compressor Specs doesn't exist.");
        }

        CompressorSpecs specs = dbSpecs.get();
        specs.setCapacity(dto.getCapacity());
        specs.setPowerInput(dto.getPowerInput());
        specs.setQC1(dto.getQC1()); specs.setQC2(dto.getQC2()); specs.setQC3(dto.getQC3());
        specs.setQC4(dto.getQC4()); specs.setQC5(dto.getQC5()); specs.setQC6(dto.getQC6());
        specs.setQC7(dto.getQC7()); specs.setQC8(dto.getQC8()); specs.setQC9(dto.getQC9());
        specs.setQC10(dto.getQC10());
        specs.setPC1(dto.getPC1()); specs.setPC2(dto.getPC2()); specs.setPC3(dto.getPC3());
        specs.setPC4(dto.getPC4()); specs.setPC5(dto.getPC5()); specs.setPC6(dto.getPC6());
        specs.setPC7(dto.getPC7()); specs.setPC8(dto.getPC8()); specs.setPC9(dto.getPC9());
        specs.setPC10(dto.getPC10());

        compressorSpecsRepository.save(specs);
    }

    public List<CompressorSpecsResponseDTO> getAllCompressorSpecs() {
        return compressorSpecsRepository.findAll().stream().map(specs -> {
            CompressorSpecsResponseDTO dto = new CompressorSpecsResponseDTO();
            dto.setId(specs.getId());
            dto.setCapacity(specs.getCapacity());
            dto.setPowerInput(specs.getPowerInput());
            if (specs.getCompressor() != null) {
                dto.setBrand(specs.getCompressor().getBrand());
                dto.setModel(specs.getCompressor().getModel());
                dto.setType(specs.getCompressor().getType().name());
            }
            dto.setQC1(specs.getQC1()); dto.setQC2(specs.getQC2()); dto.setQC3(specs.getQC3());
            dto.setQC4(specs.getQC4()); dto.setQC5(specs.getQC5()); dto.setQC6(specs.getQC6());
            dto.setQC7(specs.getQC7()); dto.setQC8(specs.getQC8()); dto.setQC9(specs.getQC9());
            dto.setQC10(specs.getQC10());
            dto.setPC1(specs.getPC1()); dto.setPC2(specs.getPC2()); dto.setPC3(specs.getPC3());
            dto.setPC4(specs.getPC4()); dto.setPC5(specs.getPC5()); dto.setPC6(specs.getPC6());
            dto.setPC7(specs.getPC7()); dto.setPC8(specs.getPC8()); dto.setPC9(specs.getPC9());
            dto.setPC10(specs.getPC10());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void addEvaporator(EvaporatorDTO dto) {

        componentDomainService.validateUniqueModelForEvaporator(dto.getModel());

        Evaporator evaporator = new Evaporator();
        evaporator.setBrand(dto.getBrand());
        evaporator.setModel(dto.getModel());

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
        return evaporatorRepository.findAll();
    }

    @Transactional
    public void editEvaporator(Long id, EvaporatorDTO dto) {

        componentDomainService.validateUniqueModelForEditEvaporator(dto.getModel(), id);

        Optional<Evaporator> dbEvaporator = evaporatorRepository.findById(id);

        if(dbEvaporator.isEmpty()){
            throw new EvaporatorDoesntExistException("Evaporator doesn't exist.");
        }

        Evaporator evaporator = dbEvaporator.get();
        evaporator.setBrand(dto.getBrand());
        evaporator.setModel(dto.getModel());

        evaporatorRepository.save(evaporator);
    }

    @Transactional
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
        return evaporatorSpecsRepository.findAll().stream().map(specs -> {
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
        return condenserRepository.findAll();
    }

    @Transactional
    public void editCondenser(Long id, CondenserDTO dto) {
        componentDomainService.validateUniqueModelForEditCondenser(dto.getModel(), id);
        Optional<Condenser> dbCondenser = condenserRepository.findById(id);
        if(dbCondenser.isEmpty()){
            throw new CondenserDoesntExistException("Condenser doesn't exist.");
        }
        Condenser condenser = dbCondenser.get();
        condenser.setBrand(dto.getBrand());
        condenser.setModel(dto.getModel());
        condenserRepository.save(condenser);
    }

    @Transactional
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
        return condenserSpecsRepository.findAll().stream().map(specs -> {
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
        return expansionValveRepository.findAll();
    }

    @Transactional
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
        return expansionValveSpecsRepository.findAll().stream().map(specs -> {
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
        return fourWayReversingValveRepository.findAll();
    }

    @Transactional
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
        return fourWayReversingValveSpecsRepository.findAll().stream().map(specs -> {
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
        return chassisRepository.findAll();
    }

    @Transactional
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
        return refrigerantRepository.findAll();
    }

    @Transactional
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
}
