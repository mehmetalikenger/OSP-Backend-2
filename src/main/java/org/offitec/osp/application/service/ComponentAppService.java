package org.offitec.osp.application.service;

import org.offitec.osp.domain.entity.*;
import org.offitec.osp.domain.enums.CompressorKind;
import org.offitec.osp.domain.enums.CondenserType;
import org.offitec.osp.domain.enums.EvaporatorType;
import org.offitec.osp.domain.enums.Mod;
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
    private final CompressorRatingRepository compressorRatingRepository;
    private final CompressorModeCapacityRepository compressorModeCapacityRepository;
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

    public ComponentAppService(ComponentDomainService componentDomainService, CompressorRepository compressorRepository, CompressorRatingRepository compressorRatingRepository, CompressorModeCapacityRepository compressorModeCapacityRepository, EvaporatorRepository evaporatorRepository, EvaporatorSpecsRepository evaporatorSpecsRepository, CondenserRepository condenserRepository, CondenserSpecsRepository condenserSpecsRepository, ExpansionValveRepository expansionValveRepository, ExpansionValveSpecsRepository expansionValveSpecsRepository, FourWayReversingValveRepository fourWayReversingValveRepository, FourWayReversingValveSpecsRepository fourWayReversingValveSpecsRepository, ChassisRepository chassisRepository, RefrigerantRepository refrigerantRepository, AuditLogService auditLogService, UserRepositoryPort userRepositoryPort) {
        this.componentDomainService = componentDomainService;
        this.compressorRepository = compressorRepository;
        this.compressorRatingRepository = compressorRatingRepository;
        this.compressorModeCapacityRepository = compressorModeCapacityRepository;
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

        // Identity only (brand, type, model, moc, lra). Refrigerant + coefficients live on
        // CompressorRating now. The add flow is Copeland-driven; brand is taken as provided (lenient).
        Compressor compressor = new Compressor();
        compressor.setType(CompressorKind.valueOf(dto.getType().toUpperCase()));
        compressor.setBrand(dto.getBrand());
        compressor.setModel(dto.getModel());
        compressor.setMoc(dto.getMoc());
        compressor.setLra(dto.getLra());

        compressorRepository.save(compressor);
    }

    public List<Compressor> getAllCompressors() {
        return compressorRepository.findAll().stream().filter(x -> !x.isDeleted()).collect(Collectors.toList());
    }

    @Transactional
    @EvictsUnitCaches
    public void editCompressor(Long id, CompressorDTO dto) {

        Compressor compressor = compressorRepository.findById(id)
                .orElseThrow(() -> new CompressorDoesntExistException("Compressor doesn't exist."));

        // Brand-based permissions: Frascold compressors are import-managed, so only their electrical
        // ratings (moc/lra) are editable. Copeland (admin-created) compressors are fully editable.
        if (isCopeland(compressor.getBrand())) {
            componentDomainService.validateUniqueModelForEdit(dto.getModel(), id);
            compressor.setType(CompressorKind.valueOf(dto.getType().toUpperCase()));
            compressor.setBrand(dto.getBrand());
            compressor.setModel(dto.getModel());
        }
        compressor.setMoc(dto.getMoc());
        compressor.setLra(dto.getLra());

        compressorRepository.save(compressor);
    }

    // --- COMPRESSOR RATING (compressor + refrigerant coefficient set) ---

    @Transactional
    @EvictsUnitCaches
    public void addCompressorRating(CompressorRatingDTO dto) {
        Compressor compressor = compressorRepository.findById(dto.getCompressorId())
                .orElseThrow(() -> new CompressorDoesntExistException("Compressor doesn't exist."));
        Refrigerant refrigerant = refrigerantRepository.findById(dto.getRefrigerantId())
                .orElseThrow(() -> new RefrigerantDoesntExistException("Selected refrigerant doesn't exist."));

        CompressorRating rating = new CompressorRating();
        rating.setCompressor(compressor);
        rating.setRefrigerant(refrigerant);
        applyRatingFields(rating, dto);
        compressorRatingRepository.save(rating);
    }

    // Edit a rating's coefficients/reference/speed. Copeland-only: Frascold coefficient sets are
    // owned by the importer and rejected here.
    @Transactional
    @EvictsUnitCaches
    public void editCompressorRating(Long ratingId, CompressorRatingDTO dto) {
        CompressorRating rating = compressorRatingRepository.findById(ratingId)
                .orElseThrow(() -> new CompressorDoesntExistException("Compressor rating doesn't exist."));
        if (rating.getCompressor() == null || !isCopeland(rating.getCompressor().getBrand())) {
            throw new IllegalArgumentException("Only Copeland ratings can be edited.");
        }
        if (dto.getRefrigerantId() != null) {
            rating.setRefrigerant(refrigerantRepository.findById(dto.getRefrigerantId())
                    .orElseThrow(() -> new RefrigerantDoesntExistException("Selected refrigerant doesn't exist.")));
        }
        applyRatingFields(rating, dto);
        compressorRatingRepository.save(rating);
    }

    // Shared field copy for add/edit rating: coefficients, reference condition (defaults ohRef=10,
    // scRef=0), inverter range.
    private void applyRatingFields(CompressorRating r, CompressorRatingDTO dto) {
        r.setCapCoeffs(dto.getCapCoeffs());
        r.setPowerCoeffs(dto.getPowerCoeffs());
        r.setMassCoeffs(dto.getMassCoeffs());
        r.setOhRef(dto.getOhRef() != null ? dto.getOhRef() : 10.0);
        r.setScRef(dto.getScRef() != null ? dto.getScRef() : 0.0);
        r.setMinFrequency(dto.getMinFrequency());
        r.setMaxFrequency(dto.getMaxFrequency());
        r.setMinSpeed(dto.getMinSpeed());
        r.setMaxSpeed(dto.getMaxSpeed());
    }

    // Upsert a rating's per-mode nominal capacity, keyed by (rating, mod). Usable by both brands —
    // this is how Frascold ratings (which import with no mode-capacities) get their duties filled.
    @Transactional
    @EvictsUnitCaches
    public void addCompressorModeCapacity(CompressorModeCapacityDTO dto) {
        CompressorRating rating = compressorRatingRepository.findById(dto.getCompressorRatingId())
                .orElseThrow(() -> new CompressorDoesntExistException("Compressor rating doesn't exist."));
        Mod mod = Mod.valueOf(dto.getMod().trim().toUpperCase());

        CompressorModeCapacity mc = rating.getModeCapacities().stream()
                .filter(c -> c.getMod() == mod).findFirst().orElse(null);
        if (mc == null) {
            mc = new CompressorModeCapacity();
            mc.setMod(mod);
            rating.getModeCapacities().add(mc);
        }
        mc.setCapacity(dto.getCapacity());
        mc.setPowerInput(dto.getPowerInput());
        mc.setMaxCapacity(dto.getMaxCapacity());
        compressorRatingRepository.save(rating); // cascade persists the child with the FK set
    }

    @Transactional(readOnly = true)
    public List<CompressorRatingResponseDTO> getAllCompressorRatings() {
        return compressorRatingRepository.findAll().stream()
                .filter(r -> r.getCompressor() != null && !r.getCompressor().isDeleted())
                .map(this::toRatingResponse)
                .collect(Collectors.toList());
    }

    private CompressorRatingResponseDTO toRatingResponse(CompressorRating r) {
        CompressorRatingResponseDTO dto = new CompressorRatingResponseDTO();
        dto.setId(r.getId());
        Compressor c = r.getCompressor();
        if (c != null) {
            dto.setCompressorId(c.getId());
            dto.setBrand(c.getBrand());
            dto.setType(c.getType() != null ? c.getType().name() : null);
            dto.setModel(c.getModel());
        }
        if (r.getRefrigerant() != null) {
            dto.setRefrigerantId(r.getRefrigerant().getId());
            dto.setRefrigerantCode(r.getRefrigerant().getCode());
        }
        dto.setCapCoeffs(r.getCapCoeffs());
        dto.setPowerCoeffs(r.getPowerCoeffs());
        dto.setMassCoeffs(r.getMassCoeffs());
        dto.setOhRef(r.getOhRef());
        dto.setScRef(r.getScRef());
        dto.setMinFrequency(r.getMinFrequency());
        dto.setMaxFrequency(r.getMaxFrequency());
        dto.setMinSpeed(r.getMinSpeed());
        dto.setMaxSpeed(r.getMaxSpeed());
        List<CompressorModeCapacityDTO> caps = r.getModeCapacities().stream().map(mc -> {
            CompressorModeCapacityDTO m = new CompressorModeCapacityDTO();
            m.setCompressorRatingId(r.getId());
            m.setMod(mc.getMod().name());
            m.setCapacity(mc.getCapacity());
            m.setPowerInput(mc.getPowerInput());
            m.setMaxCapacity(mc.getMaxCapacity());
            return m;
        }).collect(Collectors.toList());
        dto.setModeCapacities(caps);
        return dto;
    }

    // The admin brand option is spelled "Copelant" (a typo for Copeland); accept both.
    private static boolean isCopeland(String brand) {
        return brand != null && brand.trim().toLowerCase().startsWith("copel");
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
