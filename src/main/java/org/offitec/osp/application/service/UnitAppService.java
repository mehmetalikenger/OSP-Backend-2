package org.offitec.osp.application.service;

import org.offitec.osp.domain.entity.*;
import org.offitec.osp.domain.enums.AssetType;
import org.offitec.osp.domain.enums.Mod;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.enums.UnitTypeEnum;
import org.offitec.osp.domain.exception.*;
import org.offitec.osp.domain.service.UnitDomainService;
import org.offitec.osp.infrastructure.repository.*;
import org.offitec.osp.infrastructure.storage.S3Service;
import org.offitec.osp.presentation.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UnitAppService {

    private final UnitDomainService unitDomainService;
    private final UnitJpaRepository unitJpaRepository;
    private final CompressorSpecsRepository compressorSpecsRepository;
    private final CondenserSpecsRepository condenserSpecsRepository;
    private final EvaporatorSpecsRepository evaporatorSpecsRepository;
    private final ExpansionValveSpecsRepository expansionValveSpecsRepository;
    private final FourWayReversingValveSpecsRepository fourWayReversingValveSpecsRepository;
    private final ChassisRepository chassisRepository;
    private final RefrigerantRepository refrigerantRepository;
    private final UnitAssetRepository unitAssetRepository;
    private final S3Service s3Service;

    public UnitAppService(UnitDomainService unitDomainService,
                          UnitJpaRepository unitJpaRepository,
                          CompressorSpecsRepository compressorSpecsRepository,
                          CondenserSpecsRepository condenserSpecsRepository,
                          EvaporatorSpecsRepository evaporatorSpecsRepository,
                          ExpansionValveSpecsRepository expansionValveSpecsRepository,
                          FourWayReversingValveSpecsRepository fourWayReversingValveSpecsRepository,
                          ChassisRepository chassisRepository,
                          RefrigerantRepository refrigerantRepository,
                          UnitAssetRepository unitAssetRepository,
                          S3Service s3Service) {
        this.unitDomainService = unitDomainService;
        this.unitJpaRepository = unitJpaRepository;
        this.compressorSpecsRepository = compressorSpecsRepository;
        this.condenserSpecsRepository = condenserSpecsRepository;
        this.evaporatorSpecsRepository = evaporatorSpecsRepository;
        this.expansionValveSpecsRepository = expansionValveSpecsRepository;
        this.fourWayReversingValveSpecsRepository = fourWayReversingValveSpecsRepository;
        this.chassisRepository = chassisRepository;
        this.refrigerantRepository = refrigerantRepository;
        this.unitAssetRepository = unitAssetRepository;
        this.s3Service = s3Service;
    }

    // --- Create (chiller: shell + common + single cooling mode in one shot) ---

    @Transactional
    public Long addChiller(ChillerWrapperDTO dto) {

        ChillerDTO chillerDto = dto.getChillerDto();
        UnitTechSpecsDTO techDto = dto.getUnitTechSpecsDTO();

        unitDomainService.validateUniqueModel(chillerDto.getModel());

        UnitTypeEnum unitType = UnitTypeEnum.valueOf(chillerDto.getType().trim().toUpperCase());
        Mod mod = Mod.valueOf(chillerDto.getMod().trim().toUpperCase());

        Unit unit = new Unit();
        unit.setModel(chillerDto.getModel());
        unit.setName(chillerDto.getName());
        unit.setDescription(chillerDto.getDescription());
        unit.setCategory(UnitCategory.CHILLER);
        unit.setUnitType(unitType);
        applyCommonSpecs(unit, techDto);

        TechSpecs techSpecs = new TechSpecs();
        applyModeSpecs(techSpecs, techDto);

        DefaultCalculationValues calcValues = new DefaultCalculationValues();
        applyCalcValues(calcValues, dto.getUnitDefCalcValuesDTO());

        UnitDetails unitDetails = new UnitDetails();
        unitDetails.setMod(mod);
        unitDetails.setTechSpecs(techSpecs);
        unitDetails.setDefCalcValues(calcValues);
        unitDetails.setUnit(unit);

        unit.setUnitDetails(List.of(unitDetails));

        unitJpaRepository.save(unit);

        return unit.getId();
    }

    // Uploads each file to its S3 bucket and records a UnitAsset row with the returned URL.
    private void saveAssets(ChillerDTO dto, Unit unit) {

        if (notEmpty(dto.getPrimaryImage())) {
            String url = s3Service.uploadImage(buildKey(unit.getId(), dto.getPrimaryImage()), dto.getPrimaryImage());
            persistAsset(unit, AssetType.IMAGE, url, true);
        }

        uploadAll(dto.getImages(), unit, AssetType.IMAGE);
        uploadAll(dto.getTechnicalImages(), unit, AssetType.DRAWING);
        uploadAll(dto.getIcons(), unit, AssetType.ICON);
        uploadAll(dto.getDocuments(), unit, AssetType.DOCUMENT);
    }

    private void uploadAll(List<MultipartFile> files, Unit unit, AssetType type) {
        if (files == null) {
            return;
        }
        for (MultipartFile file : files) {
            if (!notEmpty(file)) {
                continue;
            }
            String key = buildKey(unit.getId(), file);
            String url = switch (type) {
                case IMAGE -> s3Service.uploadImage(key, file);
                case DRAWING -> s3Service.uploadTechnicalImage(key, file);
                case ICON -> s3Service.uploadIcon(key, file);
                case DOCUMENT -> s3Service.uploadDocument(key, file);
            };
            persistAsset(unit, type, url, false);
        }
    }

    private void persistAsset(Unit unit, AssetType type, String url, boolean isPrimary) {
        UnitAsset asset = new UnitAsset();
        asset.setUnit(unit);
        asset.setAssetType(type);
        asset.setUrl(url);
        asset.setPrimary(isPrimary);
        unitAssetRepository.save(asset);
    }

    private boolean notEmpty(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private String buildKey(Long unitId, MultipartFile file) {
        String name = file.getOriginalFilename();
        name = (name == null || name.isBlank()) ? "file" : name.replaceAll("\\s+", "_");
        return unitId + "/" + UUID.randomUUID() + "-" + name;
    }

    // --- Read ---

    @Transactional(readOnly = true)
    public List<ChillerSummaryDTO> getAllChillers() {
        return unitJpaRepository.findByCategory(UnitCategory.CHILLER).stream()
                .map(u -> new ChillerSummaryDTO(u.getId(), u.getModel(), u.getUnitType().name()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChillerResponseDTO getChiller(Long id) {

        Unit unit = unitJpaRepository.findById(id)
                .orElseThrow(() -> new UnitDoesntExistException("Chiller doesn't exist."));

        UnitDetails details = unit.getUnitDetails().stream().findFirst()
                .orElseThrow(() -> new UnitDoesntExistException("Chiller details not found."));

        TechSpecs ts = details.getTechSpecs();
        DefaultCalculationValues cv = details.getDefCalcValues();

        ChillerResponseDTO r = new ChillerResponseDTO();
        r.setId(unit.getId());
        r.setModel(unit.getModel());
        r.setName(unit.getName());
        r.setDescription(unit.getDescription());
        r.setType(unit.getUnitType().name());
        r.setMod(details.getMod().name());

        // calc values
        r.setAmbient(cv.getAmbient());
        r.setCondensation(cv.getCondensation());
        r.setEvaporation(cv.getEvaporation());
        r.setSubcooling(cv.getSubcooling());
        r.setSuperheat(cv.getSuperheat());
        r.setEvapIn(cv.getEvapIn());
        r.setEvapOut(cv.getEvapOut());
        r.setCondIn(cv.getCondIn());
        r.setCondOut(cv.getCondOut());

        // common (unit-level)
        r.setCompressorQty(unit.getCompressorQty());
        r.setCondenserQty(unit.getCondenserQty());
        r.setExpansionValveQty(unit.getExpansionValveQty());
        r.setRefrigerantId(unit.getRefrigerant() != null ? unit.getRefrigerant().getId() : null);
        r.setFanPI(unit.getFanPI());
        r.setWidth(unit.getWidth());
        r.setLength(unit.getLength());
        r.setHeight(unit.getHeight());
        r.setNumberOfFans(unit.getNumberOfFans());
        r.setFanDiameter(unit.getFanDiameter());
        r.setAirflowRate(unit.getAirflowRate());
        r.setDischargeLineDiameter(unit.getDischargeLineDiameter());
        r.setLiquidLineDiameter(unit.getLiquidLineDiameter());
        r.setSuctionLineDiameter(unit.getSuctionLineDiameter());
        r.setGasTank(unit.getGasTank());

        // per-mode (tech specs)
        r.setCapacity(ts.getCapacity());
        r.setCopErr(ts.getCopErr());
        r.setCondenserRequiredDuty(ts.getCondenserRequiredDuty());
        r.setQuietCondenserRequiredDuty(ts.getQuietCondenserRequiredDuty());
        r.setCompressorSpecsId(ts.getCompressorSpecs() != null ? ts.getCompressorSpecs().getId() : null);
        r.setCondenserSpecsId(ts.getCondenserSpecs() != null ? ts.getCondenserSpecs().getId() : null);
        r.setEvaporatorSpecsId(ts.getEvaporatorSpecs() != null ? ts.getEvaporatorSpecs().getId() : null);
        r.setExpansionValveSpecsId(ts.getExpansionValveSpecs() != null ? ts.getExpansionValveSpecs().getId() : null);
        r.setFourWayReversingValveSpecsId(ts.getFourWayReversingValveSpecs() != null ? ts.getFourWayReversingValveSpecs().getId() : null);
        r.setChassisId(ts.getChassis() != null ? ts.getChassis().getId() : null);

        List<UnitAssetDTO> assets = unitAssetRepository.findByUnitId(id).stream()
                .map(a -> {
                    String signedUrl = switch (a.getAssetType()) {
                        case IMAGE    -> s3Service.presignImage(a.getUrl());
                        case DRAWING  -> s3Service.presignTechnicalImage(a.getUrl());
                        case ICON     -> s3Service.presignIcon(a.getUrl());
                        case DOCUMENT -> s3Service.presignDocument(a.getUrl());
                    };
                    return new UnitAssetDTO(a.getId(), signedUrl, a.getAssetType().name(), a.isPrimary());
                })
                .collect(Collectors.toList());
        r.setAssets(assets);

        return r;
    }

    // --- Update (chiller) ---

    @Transactional
    public void editUnit(Long id, ChillerWrapperDTO dto) {

        Unit unit = unitJpaRepository.findById(id)
                .orElseThrow(() -> new UnitDoesntExistException("Chiller doesn't exist."));

        ChillerDTO chillerDto = dto.getChillerDto();
        UnitTechSpecsDTO techDto = dto.getUnitTechSpecsDTO();

        unitDomainService.validateUniqueModelForEdit(chillerDto.getModel(), id);

        UnitTypeEnum unitType = UnitTypeEnum.valueOf(chillerDto.getType().trim().toUpperCase());
        Mod mod = Mod.valueOf(chillerDto.getMod().trim().toUpperCase());

        unit.setModel(chillerDto.getModel());
        unit.setName(chillerDto.getName());
        unit.setDescription(chillerDto.getDescription());
        unit.setUnitType(unitType);
        applyCommonSpecs(unit, techDto);

        UnitDetails details = unit.getUnitDetails().stream().findFirst()
                .orElseThrow(() -> new UnitDoesntExistException("Chiller details not found."));
        details.setMod(mod);

        applyModeSpecs(details.getTechSpecs(), techDto);
        applyCalcValues(details.getDefCalcValues(), dto.getUnitDefCalcValuesDTO());

        unitJpaRepository.save(unit);
    }

    // --- Heat pump: shell (model + common tech), created without modes ---

    @Transactional
    public Long addHeatPump(HeatPumpModelWrapperDTO dto) {

        HeatPumpDTO hp = dto.getHeatPumpDto();

        unitDomainService.validateUniqueModel(hp.getModel());

        UnitTypeEnum unitType = UnitTypeEnum.valueOf(hp.getType().trim().toUpperCase());

        Unit unit = new Unit();
        unit.setModel(hp.getModel());
        unit.setName(hp.getName());
        unit.setDescription(hp.getDescription());
        unit.setCategory(UnitCategory.HEAT_PUMP);
        unit.setUnitType(unitType);
        applyCommonSpecs(unit, dto.getCommonSpecsDto());

        unitJpaRepository.save(unit);
        return unit.getId();
    }

    @Transactional
    public void uploadAssets(Long unitId, AssetUploadDTO dto) {

        Unit unit = unitJpaRepository.findById(unitId)
                .orElseThrow(() -> new UnitDoesntExistException("Unit doesn't exist."));

        if (notEmpty(dto.getPrimaryImage())) {
            // Clear any existing primary image before setting the new one
            unitAssetRepository.findByUnitId(unitId).stream()
                    .filter(a -> a.getAssetType() == AssetType.IMAGE && a.isPrimary())
                    .forEach(a -> { a.setPrimary(false); unitAssetRepository.save(a); });
            String url = s3Service.uploadImage(buildKey(unitId, dto.getPrimaryImage()), dto.getPrimaryImage());
            persistAsset(unit, AssetType.IMAGE, url, true);
        }

        uploadAll(dto.getImages(), unit, AssetType.IMAGE);
        uploadAll(dto.getTechnicalImages(), unit, AssetType.DRAWING);
        uploadAll(dto.getIcons(), unit, AssetType.ICON);
        uploadAll(dto.getDocuments(), unit, AssetType.DOCUMENT);
    }

    // --- Heat pump: attach one mode's details to an existing heat pump ---

    @Transactional
    public void addHeatPumpDetails(HeatPumpDetailsWrapperDTO dto) {

        Unit unit = unitJpaRepository.findById(dto.getHeatPumpId())
                .orElseThrow(() -> new UnitDoesntExistException("Heat pump doesn't exist."));

        if (unit.getCategory() != UnitCategory.HEAT_PUMP) {
            throw new UnitDoesntExistException("Selected unit is not a heat pump.");
        }

        Mod mod = Mod.valueOf(dto.getMod().trim().toUpperCase());

        boolean modeExists = unit.getUnitDetails() != null && unit.getUnitDetails().stream()
                .anyMatch(d -> d.getMod() == mod);
        if (modeExists) {
            throw new ModelAlreadyExistsException("This mode already exists for the heat pump.");
        }

        TechSpecs techSpecs = new TechSpecs();
        applyModeSpecs(techSpecs, dto.getModeSpecsDto());

        DefaultCalculationValues calcValues = new DefaultCalculationValues();
        applyCalcValues(calcValues, dto.getUnitDefCalcValuesDTO());

        UnitDetails details = new UnitDetails();
        details.setMod(mod);
        details.setTechSpecs(techSpecs);
        details.setDefCalcValues(calcValues);
        details.setUnit(unit);

        if (unit.getUnitDetails() == null) {
            unit.setUnitDetails(new ArrayList<>());
        }
        unit.getUnitDetails().add(details);

        unitJpaRepository.save(unit);
    }

    @Transactional(readOnly = true)
    public List<HeatPumpSummaryDTO> getAllHeatPumps() {
        return unitJpaRepository.findByCategory(UnitCategory.HEAT_PUMP).stream()
                .map(u -> new HeatPumpSummaryDTO(
                        u.getId(),
                        u.getModel(),
                        u.getUnitType().name(),
                        u.getUnitDetails() == null ? List.of()
                                : u.getUnitDetails().stream().map(d -> d.getMod().name()).collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HeatPumpResponseDTO getHeatPump(Long id) {

        Unit unit = unitJpaRepository.findById(id)
                .orElseThrow(() -> new UnitDoesntExistException("Heat pump doesn't exist."));

        if (unit.getCategory() != UnitCategory.HEAT_PUMP) {
            throw new UnitDoesntExistException("Selected unit is not a heat pump.");
        }

        HeatPumpResponseDTO r = new HeatPumpResponseDTO();
        r.setId(unit.getId());
        r.setModel(unit.getModel());
        r.setName(unit.getName());
        r.setDescription(unit.getDescription());
        r.setType(unit.getUnitType().name());

        r.setCompressorQty(unit.getCompressorQty());
        r.setCondenserQty(unit.getCondenserQty());
        r.setExpansionValveQty(unit.getExpansionValveQty());
        r.setRefrigerantId(unit.getRefrigerant() != null ? unit.getRefrigerant().getId() : null);
        r.setFanPI(unit.getFanPI());
        r.setWidth(unit.getWidth());
        r.setLength(unit.getLength());
        r.setHeight(unit.getHeight());
        r.setNumberOfFans(unit.getNumberOfFans());
        r.setFanDiameter(unit.getFanDiameter());
        r.setAirflowRate(unit.getAirflowRate());
        r.setDischargeLineDiameter(unit.getDischargeLineDiameter());
        r.setLiquidLineDiameter(unit.getLiquidLineDiameter());
        r.setSuctionLineDiameter(unit.getSuctionLineDiameter());
        r.setGasTank(unit.getGasTank());

        List<HeatPumpModeDTO> modes = new ArrayList<>();
        if (unit.getUnitDetails() != null) {
            for (UnitDetails d : unit.getUnitDetails()) {
                TechSpecs ts = d.getTechSpecs();
                DefaultCalculationValues cv = d.getDefCalcValues();

                HeatPumpModeDTO m = new HeatPumpModeDTO();
                m.setMod(d.getMod().name());
                m.setCapacity(ts.getCapacity());
                m.setCopErr(ts.getCopErr());
                m.setCondenserRequiredDuty(ts.getCondenserRequiredDuty());
                m.setQuietCondenserRequiredDuty(ts.getQuietCondenserRequiredDuty());
                m.setCompressorSpecsId(ts.getCompressorSpecs() != null ? ts.getCompressorSpecs().getId() : null);
                m.setCondenserSpecsId(ts.getCondenserSpecs() != null ? ts.getCondenserSpecs().getId() : null);
                m.setEvaporatorSpecsId(ts.getEvaporatorSpecs() != null ? ts.getEvaporatorSpecs().getId() : null);
                m.setExpansionValveSpecsId(ts.getExpansionValveSpecs() != null ? ts.getExpansionValveSpecs().getId() : null);
                m.setFourWayReversingValveSpecsId(ts.getFourWayReversingValveSpecs() != null ? ts.getFourWayReversingValveSpecs().getId() : null);
                m.setChassisId(ts.getChassis() != null ? ts.getChassis().getId() : null);
                m.setAmbient(cv.getAmbient());
                m.setCondensation(cv.getCondensation());
                m.setEvaporation(cv.getEvaporation());
                m.setSubcooling(cv.getSubcooling());
                m.setSuperheat(cv.getSuperheat());
                m.setEvapIn(cv.getEvapIn());
                m.setEvapOut(cv.getEvapOut());
                m.setCondIn(cv.getCondIn());
                m.setCondOut(cv.getCondOut());
                modes.add(m);
            }
        }
        r.setModes(modes);

        List<UnitAssetDTO> assets = unitAssetRepository.findByUnitId(id).stream()
                .map(a -> {
                    String signedUrl = switch (a.getAssetType()) {
                        case IMAGE    -> s3Service.presignImage(a.getUrl());
                        case DRAWING  -> s3Service.presignTechnicalImage(a.getUrl());
                        case ICON     -> s3Service.presignIcon(a.getUrl());
                        case DOCUMENT -> s3Service.presignDocument(a.getUrl());
                    };
                    return new UnitAssetDTO(a.getId(), signedUrl, a.getAssetType().name(), a.isPrimary());
                })
                .collect(Collectors.toList());
        r.setAssets(assets);

        return r;
    }

    @Transactional
    public void editHeatPump(Long id, HeatPumpModelWrapperDTO dto) {

        Unit unit = unitJpaRepository.findById(id)
                .orElseThrow(() -> new UnitDoesntExistException("Heat pump doesn't exist."));

        if (unit.getCategory() != UnitCategory.HEAT_PUMP) {
            throw new UnitDoesntExistException("Selected unit is not a heat pump.");
        }

        HeatPumpDTO hp = dto.getHeatPumpDto();
        unitDomainService.validateUniqueModelForEdit(hp.getModel(), id);

        unit.setModel(hp.getModel());
        unit.setName(hp.getName());
        unit.setDescription(hp.getDescription());
        unit.setUnitType(UnitTypeEnum.valueOf(hp.getType().trim().toUpperCase()));
        applyCommonSpecs(unit, dto.getCommonSpecsDto());

        unitJpaRepository.save(unit);
    }

    @Transactional
    public void editHeatPumpDetails(HeatPumpDetailsWrapperDTO dto) {

        Unit unit = unitJpaRepository.findById(dto.getHeatPumpId())
                .orElseThrow(() -> new UnitDoesntExistException("Heat pump doesn't exist."));

        Mod mod = Mod.valueOf(dto.getMod().trim().toUpperCase());

        UnitDetails details = unit.getUnitDetails() == null ? null
                : unit.getUnitDetails().stream().filter(d -> d.getMod() == mod).findFirst().orElse(null);
        if (details == null) {
            throw new UnitDoesntExistException("This mode doesn't exist for the heat pump.");
        }

        applyModeSpecs(details.getTechSpecs(), dto.getModeSpecsDto());
        applyCalcValues(details.getDefCalcValues(), dto.getUnitDefCalcValuesDTO());

        unitJpaRepository.save(unit);
    }

    // --- Shared assembly ---

    // Common attributes that are identical across all modes of a unit.
    void applyCommonSpecs(Unit unit, UnitTechSpecsDTO d) {
        unit.setCompressorQty(d.getCompressorQty());
        unit.setCondenserQty(d.getCondenserQty());
        unit.setExpansionValveQty(d.getExpansionValveQty());
        unit.setRefrigerant(refrigerantRepository.findById(d.getRefrigerantId())
                .orElseThrow(() -> new RefrigerantDoesntExistException("Selected refrigerant doesn't exist.")));
        unit.setFanPI(d.getFanPI());
        unit.setWidth(d.getWidth());
        unit.setLength(d.getLength());
        unit.setHeight(d.getHeight());
        unit.setNumberOfFans(d.getNumberOfFans());
        unit.setFanDiameter(d.getFanDiameter());
        unit.setAirflowRate(d.getAirflowRate());
        unit.setDischargeLineDiameter(d.getDischargeLineDiameter());
        unit.setLiquidLineDiameter(d.getLiquidLineDiameter());
        unit.setSuctionLineDiameter(d.getSuctionLineDiameter());
        unit.setGasTank(d.getGasTank());
    }

    // Per-mode attributes + the component spec points selected for that mode.
    void applyModeSpecs(TechSpecs ts, UnitTechSpecsDTO d) {
        ts.setCapacity(d.getCapacity());
        ts.setCopErr(d.getCopErr());
        ts.setCondenserRequiredDuty(d.getCondenserRequiredDuty());
        ts.setQuietCondenserRequiredDuty(d.getQuietCondenserRequiredDuty());

        ts.setCompressorSpecs(compressorSpecsRepository.findById(d.getCompressorSpecsId())
                .orElseThrow(() -> new CompressorSpecsDoesntExistException("Selected compressor doesn't exist.")));
        ts.setCondenserSpecs(condenserSpecsRepository.findById(d.getCondenserSpecsId())
                .orElseThrow(() -> new CondenserSpecsDoesntExistException("Selected condenser doesn't exist.")));
        ts.setEvaporatorSpecs(evaporatorSpecsRepository.findById(d.getEvaporatorSpecsId())
                .orElseThrow(() -> new EvaporatorSpecsDoesntExistException("Selected evaporator doesn't exist.")));
        ts.setExpansionValveSpecs(expansionValveSpecsRepository.findById(d.getExpansionValveSpecsId())
                .orElseThrow(() -> new ExpansionValveSpecsDoesntExistException("Selected expansion valve doesn't exist.")));
        ts.setChassis(chassisRepository.findById(d.getChassisId())
                .orElseThrow(() -> new ChassisDoesntExistException("Selected chassis doesn't exist.")));

        if (d.getFourWayReversingValveSpecsId() != null) {
            ts.setFourWayReversingValveSpecs(fourWayReversingValveSpecsRepository.findById(d.getFourWayReversingValveSpecsId())
                    .orElseThrow(() -> new FourWayReversingValveSpecsDoesntExistException("Selected 4-way reversing valve doesn't exist.")));
        } else {
            ts.setFourWayReversingValveSpecs(null);
        }
    }

    // Overloads for the split heat-pump DTOs (common on the model page, mode on the mod page).

    void applyCommonSpecs(Unit unit, UnitCommonSpecsDTO d) {
        unit.setCompressorQty(d.getCompressorQty());
        unit.setCondenserQty(d.getCondenserQty());
        unit.setExpansionValveQty(d.getExpansionValveQty());
        unit.setRefrigerant(refrigerantRepository.findById(d.getRefrigerantId())
                .orElseThrow(() -> new RefrigerantDoesntExistException("Selected refrigerant doesn't exist.")));
        unit.setFanPI(d.getFanPI());
        unit.setWidth(d.getWidth());
        unit.setLength(d.getLength());
        unit.setHeight(d.getHeight());
        unit.setNumberOfFans(d.getNumberOfFans());
        unit.setFanDiameter(d.getFanDiameter());
        unit.setAirflowRate(d.getAirflowRate());
        unit.setDischargeLineDiameter(d.getDischargeLineDiameter());
        unit.setLiquidLineDiameter(d.getLiquidLineDiameter());
        unit.setSuctionLineDiameter(d.getSuctionLineDiameter());
        unit.setGasTank(d.getGasTank());
    }

    void applyModeSpecs(TechSpecs ts, UnitModeSpecsDTO d) {
        ts.setCapacity(d.getCapacity());
        ts.setCopErr(d.getCopErr());
        ts.setCondenserRequiredDuty(d.getCondenserRequiredDuty());
        ts.setQuietCondenserRequiredDuty(d.getQuietCondenserRequiredDuty());

        ts.setCompressorSpecs(compressorSpecsRepository.findById(d.getCompressorSpecsId())
                .orElseThrow(() -> new CompressorSpecsDoesntExistException("Selected compressor doesn't exist.")));
        ts.setCondenserSpecs(condenserSpecsRepository.findById(d.getCondenserSpecsId())
                .orElseThrow(() -> new CondenserSpecsDoesntExistException("Selected condenser doesn't exist.")));
        ts.setEvaporatorSpecs(evaporatorSpecsRepository.findById(d.getEvaporatorSpecsId())
                .orElseThrow(() -> new EvaporatorSpecsDoesntExistException("Selected evaporator doesn't exist.")));
        ts.setExpansionValveSpecs(expansionValveSpecsRepository.findById(d.getExpansionValveSpecsId())
                .orElseThrow(() -> new ExpansionValveSpecsDoesntExistException("Selected expansion valve doesn't exist.")));
        ts.setChassis(chassisRepository.findById(d.getChassisId())
                .orElseThrow(() -> new ChassisDoesntExistException("Selected chassis doesn't exist.")));

        if (d.getFourWayReversingValveSpecsId() != null) {
            ts.setFourWayReversingValveSpecs(fourWayReversingValveSpecsRepository.findById(d.getFourWayReversingValveSpecsId())
                    .orElseThrow(() -> new FourWayReversingValveSpecsDoesntExistException("Selected 4-way reversing valve doesn't exist.")));
        } else {
            ts.setFourWayReversingValveSpecs(null);
        }
    }

    // --- Asset management ---

    @Transactional
    public void deleteAsset(Long assetId) {
        UnitAsset asset = unitAssetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found: " + assetId));

        switch (asset.getAssetType()) {
            case IMAGE    -> s3Service.deleteImage(asset.getUrl());
            case DRAWING  -> s3Service.deleteTechnicalImage(asset.getUrl());
            case ICON     -> s3Service.deleteIcon(asset.getUrl());
            case DOCUMENT -> s3Service.deleteDocument(asset.getUrl());
        }

        unitAssetRepository.delete(asset);
    }

    @Transactional
    public void setPrimaryAsset(Long assetId) {
        UnitAsset target = unitAssetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found: " + assetId));

        if (target.getAssetType() != AssetType.IMAGE) {
            throw new RuntimeException("Only IMAGE assets can be set as primary.");
        }

        Long unitId = target.getUnit().getId();
        unitAssetRepository.findByUnitId(unitId).stream()
                .filter(a -> a.getAssetType() == AssetType.IMAGE && a.isPrimary())
                .forEach(a -> { a.setPrimary(false); unitAssetRepository.save(a); });

        target.setPrimary(true);
        unitAssetRepository.save(target);
    }

    void applyCalcValues(DefaultCalculationValues calcValues, UnitDefCalcValuesDTO calcDto) {
        calcValues.setAmbient(calcDto.getAmbient());
        calcValues.setCondensation(calcDto.getCondensation());
        calcValues.setEvaporation(calcDto.getEvaporation());
        calcValues.setSubcooling(calcDto.getSubcooling());
        calcValues.setSuperheat(calcDto.getSuperheat());
        calcValues.setEvapIn(calcDto.getEvapIn());
        calcValues.setEvapOut(calcDto.getEvapOut());
        calcValues.setCondIn(calcDto.getCondIn());
        calcValues.setCondOut(calcDto.getCondOut());
    }
}
