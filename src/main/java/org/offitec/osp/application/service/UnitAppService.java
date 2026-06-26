package org.offitec.osp.application.service;

import org.offitec.osp.domain.entity.*;
import org.offitec.osp.domain.enums.AssetType;
import org.offitec.osp.domain.enums.Mod;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.enums.UnitTypeEnum;
import org.offitec.osp.domain.exception.*;
import org.offitec.osp.domain.service.UnitDomainService;
import org.offitec.osp.domain.service.AuditLogService;
import org.offitec.osp.domain.port.UserRepositoryPort;
import org.offitec.osp.infrastructure.config.EvictsUnitCaches;
import org.offitec.osp.infrastructure.repository.*;
import org.offitec.osp.infrastructure.storage.S3Service;
import org.offitec.osp.presentation.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UnitAppService {

    private final UnitDomainService unitDomainService;
    private final UnitJpaRepository unitJpaRepository;
    private final CompressorSpecsRepository compressorSpecsRepository;
    private final org.offitec.osp.infrastructure.repository.CompressorRatingRepository compressorRatingRepository;
    private final CondenserSpecsRepository condenserSpecsRepository;
    private final EvaporatorSpecsRepository evaporatorSpecsRepository;
    private final ExpansionValveSpecsRepository expansionValveSpecsRepository;
    private final FourWayReversingValveSpecsRepository fourWayReversingValveSpecsRepository;
    private final ChassisRepository chassisRepository;
    private final RefrigerantRepository refrigerantRepository;
    private final UnitAssetRepository unitAssetRepository;
    private final S3Service s3Service;
    private final AuditLogService auditLogService;
    private final UserRepositoryPort userRepositoryPort;

    public UnitAppService(UnitDomainService unitDomainService,
                          UnitJpaRepository unitJpaRepository,
                          CompressorSpecsRepository compressorSpecsRepository,
                          org.offitec.osp.infrastructure.repository.CompressorRatingRepository compressorRatingRepository,
                          CondenserSpecsRepository condenserSpecsRepository,
                          EvaporatorSpecsRepository evaporatorSpecsRepository,
                          ExpansionValveSpecsRepository expansionValveSpecsRepository,
                          FourWayReversingValveSpecsRepository fourWayReversingValveSpecsRepository,
                          ChassisRepository chassisRepository,
                          RefrigerantRepository refrigerantRepository,
                          UnitAssetRepository unitAssetRepository,
                          S3Service s3Service,
                          AuditLogService auditLogService,
                          UserRepositoryPort userRepositoryPort) {
        this.unitDomainService = unitDomainService;
        this.unitJpaRepository = unitJpaRepository;
        this.compressorSpecsRepository = compressorSpecsRepository;
        this.compressorRatingRepository = compressorRatingRepository;
        this.condenserSpecsRepository = condenserSpecsRepository;
        this.evaporatorSpecsRepository = evaporatorSpecsRepository;
        this.expansionValveSpecsRepository = expansionValveSpecsRepository;
        this.fourWayReversingValveSpecsRepository = fourWayReversingValveSpecsRepository;
        this.chassisRepository = chassisRepository;
        this.refrigerantRepository = refrigerantRepository;
        this.unitAssetRepository = unitAssetRepository;
        this.s3Service = s3Service;
        this.auditLogService = auditLogService;
        this.userRepositoryPort = userRepositoryPort;
    }

    // --- Create (chiller: shell + common + single cooling mode in one shot) ---

    @Transactional
    @EvictsUnitCaches
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
        List<MultipartFile> valid = files.stream().filter(this::notEmpty).toList();
        if (valid.isEmpty()) {
            return;
        }
        // Optimize + upload to S3 in parallel: image processing and the S3 round-trip
        // are the slow parts, so this cuts the wall time for multi-image uploads.
        Long unitId = unit.getId();
        List<String> urls = valid.parallelStream()
                .map(file -> uploadToS3(buildKey(unitId, file), file, type))
                .toList();
        // Persist on the calling thread (the JPA session is not thread-safe).
        for (String url : urls) {
            persistAsset(unit, type, url, false);
        }
    }

    private String uploadToS3(String key, MultipartFile file, AssetType type) {
        return switch (type) {
            case IMAGE -> s3Service.uploadImage(key, file);
            case DRAWING -> s3Service.uploadTechnicalImage(key, file);
            case ICON -> s3Service.uploadIcon(key, file);
            case DOCUMENT -> s3Service.uploadDocument(key, file);
        };
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
        return buildKey(unitId, file.getOriginalFilename());
    }

    private String buildKey(Long unitId, String filename) {
        String name = (filename == null || filename.isBlank()) ? "file" : filename.replaceAll("\\s+", "_");
        return unitId + "/" + UUID.randomUUID() + "-" + name;
    }

    // --- Direct client upload (presigned PUT) ---
    //
    // Two-step flow that lets the browser upload straight to R2:
    //   presignAssets  -> validate + hand out short-lived PUT URLs (key chosen here)
    //   confirmAssets  -> verify the uploaded objects and persist UnitAsset rows
    // The backend never sees the bytes, but still owns the object key and re-checks
    // type/size/key-prefix so a client can't register arbitrary or oversized objects.

    private static final long MAX_ASSET_BYTES = 25L * 1024 * 1024; // 25 MB per file

    private static final Set<String> RASTER_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ICON_TYPES = Set.of("image/png", "image/svg+xml", "image/x-icon", "image/vnd.microsoft.icon");
    private static final Set<String> DOCUMENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private Set<String> allowedTypes(AssetType type) {
        return switch (type) {
            case IMAGE, DRAWING -> RASTER_IMAGE_TYPES;
            case ICON -> ICON_TYPES;
            case DOCUMENT -> DOCUMENT_TYPES;
        };
    }

    // Storage prefix each asset type lives under (mirrors S3Service's PREFIX_* values).
    private String prefixFor(AssetType type) {
        return switch (type) {
            case IMAGE -> "unit-images";
            case DRAWING -> "technical-images";
            case ICON -> "icons";
            case DOCUMENT -> "documents";
        };
    }

    @Transactional(readOnly = true)
    public AssetPresignResponseDTO presignAssets(Long unitId, AssetPresignRequestDTO request) {
        // Confirms the unit exists (and that the caller, already an authenticated admin,
        // is targeting a real unit) before minting any upload URLs.
        unitJpaRepository.findById(unitId)
                .orElseThrow(() -> new UnitDoesntExistException("Unit doesn't exist."));

        if (request.getFiles() == null || request.getFiles().isEmpty()) {
            return new AssetPresignResponseDTO(List.of());
        }

        List<AssetPresignResponseDTO.Item> tickets = new ArrayList<>();
        for (AssetPresignRequestDTO.Item file : request.getFiles()) {
            validateUploadRequest(file);
            String contentType = file.getContentType().toLowerCase();
            String key = buildKey(unitId, file.getFilename());
            S3Service.PresignedUpload presigned = switch (file.getType()) {
                case IMAGE -> s3Service.presignImageUpload(key, contentType);
                case DRAWING -> s3Service.presignTechnicalImageUpload(key, contentType);
                case ICON -> s3Service.presignIconUpload(key, contentType);
                case DOCUMENT -> s3Service.presignDocumentUpload(key, contentType);
            };
            tickets.add(new AssetPresignResponseDTO.Item(file.getClientId(), presigned.uploadUrl(), presigned.key()));
        }
        return new AssetPresignResponseDTO(tickets);
    }

    private void validateUploadRequest(AssetPresignRequestDTO.Item file) {
        if (file.getType() == null) {
            throw new InvalidAssetException("Asset type is required.");
        }
        if (file.getSize() <= 0 || file.getSize() > MAX_ASSET_BYTES) {
            throw new InvalidAssetException("Each file must be between 1 byte and 25 MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes(file.getType()).contains(contentType.toLowerCase())) {
            throw new InvalidAssetException("Unsupported file type for " + file.getType() + ": " + contentType);
        }
    }

    @Transactional
    @EvictsUnitCaches
    public void confirmAssets(Long unitId, AssetConfirmRequestDTO request) {
        Unit unit = unitJpaRepository.findById(unitId)
                .orElseThrow(() -> new UnitDoesntExistException("Unit doesn't exist."));

        if (request.getFiles() == null || request.getFiles().isEmpty()) {
            return;
        }

        // If a new primary image is being registered, demote the current one first
        // (same behaviour as the old multipart uploadAssets path).
        boolean settingPrimary = request.getFiles().stream()
                .anyMatch(f -> f.isPrimary() && f.getType() == AssetType.IMAGE);
        if (settingPrimary) {
            unitAssetRepository.findByUnitId(unitId).stream()
                    .filter(a -> a.getAssetType() == AssetType.IMAGE && a.isPrimary())
                    .forEach(a -> { a.setPrimary(false); unitAssetRepository.save(a); });
        }

        for (AssetConfirmRequestDTO.Item file : request.getFiles()) {
            String key = validateConfirmKey(unitId, file);
            verifyUploadedObject(file.getType(), key);
            boolean primary = file.isPrimary() && file.getType() == AssetType.IMAGE;
            persistAsset(unit, file.getType(), s3Service.publicUrl(key), primary);
        }
    }

    // The client must hand back a key the backend itself issued: it has to sit under
    // this unit's prefix for the declared type. Rejects attempts to attach arbitrary
    // or cross-unit objects.
    private String validateConfirmKey(Long unitId, AssetConfirmRequestDTO.Item file) {
        if (file.getType() == null || file.getKey() == null) {
            throw new InvalidAssetException("Asset key and type are required.");
        }
        String expectedPrefix = prefixFor(file.getType()) + "/" + unitId + "/";
        if (!file.getKey().startsWith(expectedPrefix)) {
            throw new InvalidAssetException("Asset key is not valid for this unit.");
        }
        return file.getKey();
    }

    // Defense in depth: confirm the object actually exists in storage and that its
    // real (server-observed) size and content type are within the allowed bounds.
    // If not, the object is removed and the confirmation is rejected.
    private void verifyUploadedObject(AssetType type, String key) {
        com.amazonaws.services.s3.model.ObjectMetadata meta;
        try {
            meta = s3Service.statObject(key);
        } catch (Exception e) {
            throw new InvalidAssetException("Uploaded file was not found in storage: " + key);
        }
        boolean tooBig = meta.getContentLength() > MAX_ASSET_BYTES;
        String contentType = meta.getContentType();
        boolean badType = contentType == null || !allowedTypes(type).contains(contentType.toLowerCase());
        if (tooBig || badType) {
            try { s3Service.deleteByKey(key); } catch (Exception ignored) { /* best effort cleanup */ }
            throw new InvalidAssetException("Uploaded file failed validation (size or type).");
        }
    }

    // --- Read ---

    @Transactional(readOnly = true)
    public List<ChillerSummaryDTO> getAllChillers() {
        return unitJpaRepository.findByCategory(UnitCategory.CHILLER).stream()
                .filter(u -> !u.isDeleted())
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
        r.setEvapIn(cv.getEvapIn());
        r.setEvapOut(cv.getEvapOut());
        r.setCondIn(cv.getCondIn());
        r.setCondOut(cv.getCondOut());

        // common (unit-level)
        r.setCompressorQty(unit.getCompressorQty());
        r.setCondenserQty(unit.getCondenserQty());
        r.setExpansionValveQty(unit.getExpansionValveQty());
        r.setFanPI(unit.getFanPI());
        r.setWidth(unit.getWidth());
        r.setLength(unit.getLength());
        r.setHeight(unit.getHeight());
        r.setFanType(unit.getFanType());
        r.setNumberOfFans(unit.getNumberOfFans());
        r.setFanDiameter(unit.getFanDiameter());
        r.setAirflowRate(unit.getAirflowRate());
        r.setDischargeLineDiameter(unit.getDischargeLineDiameter());
        r.setLiquidLineDiameter(unit.getLiquidLineDiameter());
        r.setSuctionLineDiameter(unit.getSuctionLineDiameter());
        r.setGasTank(unit.getGasTank());
        r.setWaterInletConnection(unit.getWaterInletConnection());
        r.setWaterOutletConnection(unit.getWaterOutletConnection());
        r.setMinWaterInlet(unit.getMinWaterInlet());
        r.setMaxWaterInlet(unit.getMaxWaterInlet());
        r.setMinWaterOutlet(unit.getMinWaterOutlet());
        r.setMaxWaterOutlet(unit.getMaxWaterOutlet());
        r.setMinAmbient(unit.getMinAmbient());
        r.setMaxAmbient(unit.getMaxAmbient());

        // per-mode (tech specs)
        r.setCapacity(ts.getCapacity());
        r.setMaxCapacity(ts.getMaxCapacity() != null ? ts.getMaxCapacity() : 0.0);
        r.setCopErr(ts.getCopErr());
        r.setCondenserRequiredDuty(ts.getCondenserRequiredDuty());
        r.setQuietCondenserRequiredDuty(ts.getQuietCondenserRequiredDuty());
        r.setCompressorSpecsId(ts.getCompressorSpecs() != null ? ts.getCompressorSpecs().getId() : null);
        r.setCompressorRatingId(ts.getCompressorRating() != null ? ts.getCompressorRating().getId() : null);
        r.setCondenserSpecsId(ts.getCondenserSpecs() != null ? ts.getCondenserSpecs().getId() : null);
        r.setEvaporatorSpecsId(ts.getEvaporatorSpecs() != null ? ts.getEvaporatorSpecs().getId() : null);
        r.setExpansionValveSpecsId(ts.getExpansionValveSpecs() != null ? ts.getExpansionValveSpecs().getId() : null);
        r.setFourWayReversingValveSpecsId(ts.getFourWayReversingValveSpecs() != null ? ts.getFourWayReversingValveSpecs().getId() : null);
        r.setChassisId(unit.getChassis() != null ? unit.getChassis().getId() : null);

        List<UnitAssetDTO> assets = unitAssetRepository.findByUnitId(id).stream()
                .map(a -> new UnitAssetDTO(a.getId(), a.getUrl(), a.getAssetType().name(), a.isPrimary()))
                .collect(Collectors.toList());
        r.setAssets(assets);

        return r;
    }

    // --- Update (chiller) ---

    @Transactional
    @EvictsUnitCaches
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
    @EvictsUnitCaches
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
    @EvictsUnitCaches
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
    @EvictsUnitCaches
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
        techSpecs.setCompressorRating(unit.getCompressorRating()); // shared, chosen on the model form

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
                .filter(u -> !u.isDeleted())
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
        r.setCompressorRatingId(unit.getCompressorRating() != null ? unit.getCompressorRating().getId() : null);
        r.setCondenserQty(unit.getCondenserQty());
        r.setExpansionValveQty(unit.getExpansionValveQty());
        r.setFanPI(unit.getFanPI());
        r.setWidth(unit.getWidth());
        r.setLength(unit.getLength());
        r.setHeight(unit.getHeight());
        r.setFanType(unit.getFanType());
        r.setNumberOfFans(unit.getNumberOfFans());
        r.setFanDiameter(unit.getFanDiameter());
        r.setAirflowRate(unit.getAirflowRate());
        r.setDischargeLineDiameter(unit.getDischargeLineDiameter());
        r.setLiquidLineDiameter(unit.getLiquidLineDiameter());
        r.setSuctionLineDiameter(unit.getSuctionLineDiameter());
        r.setGasTank(unit.getGasTank());
        r.setWaterInletConnection(unit.getWaterInletConnection());
        r.setWaterOutletConnection(unit.getWaterOutletConnection());

        List<HeatPumpModeDTO> modes = new ArrayList<>();
        if (unit.getUnitDetails() != null) {
            for (UnitDetails d : unit.getUnitDetails()) {
                TechSpecs ts = d.getTechSpecs();
                DefaultCalculationValues cv = d.getDefCalcValues();

                HeatPumpModeDTO m = new HeatPumpModeDTO();
                m.setMod(d.getMod().name());
                m.setCapacity(ts.getCapacity());
                m.setMaxCapacity(ts.getMaxCapacity() != null ? ts.getMaxCapacity() : 0.0);
                m.setCopErr(ts.getCopErr());
                m.setCondenserRequiredDuty(ts.getCondenserRequiredDuty());
                m.setQuietCondenserRequiredDuty(ts.getQuietCondenserRequiredDuty());
                m.setCompressorSpecsId(ts.getCompressorSpecs() != null ? ts.getCompressorSpecs().getId() : null);
                m.setCompressorRatingId(ts.getCompressorRating() != null ? ts.getCompressorRating().getId() : null);
                m.setCondenserSpecsId(ts.getCondenserSpecs() != null ? ts.getCondenserSpecs().getId() : null);
                m.setEvaporatorSpecsId(ts.getEvaporatorSpecs() != null ? ts.getEvaporatorSpecs().getId() : null);
                m.setExpansionValveSpecsId(ts.getExpansionValveSpecs() != null ? ts.getExpansionValveSpecs().getId() : null);
                m.setFourWayReversingValveSpecsId(ts.getFourWayReversingValveSpecs() != null ? ts.getFourWayReversingValveSpecs().getId() : null);
                m.setAmbient(cv.getAmbient());
                m.setEvapIn(cv.getEvapIn());
                m.setEvapOut(cv.getEvapOut());
                m.setCondIn(cv.getCondIn());
                m.setCondOut(cv.getCondOut());
                modes.add(m);
            }
        }
        r.setChassisId(unit.getChassis() != null ? unit.getChassis().getId() : null);
        r.setModes(modes);

        List<UnitAssetDTO> assets = unitAssetRepository.findByUnitId(id).stream()
                .map(a -> new UnitAssetDTO(a.getId(), a.getUrl(), a.getAssetType().name(), a.isPrimary()))
                .collect(Collectors.toList());
        r.setAssets(assets);

        return r;
    }

    @Transactional
    @EvictsUnitCaches
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
    @EvictsUnitCaches
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
        details.getTechSpecs().setCompressorRating(unit.getCompressorRating()); // shared, from the model form
        applyCalcValues(details.getDefCalcValues(), dto.getUnitDefCalcValuesDTO());

        unitJpaRepository.save(unit);
    }

    // --- Shared assembly ---

    // Common attributes that are identical across all modes of a unit.
    void applyCommonSpecs(Unit unit, UnitTechSpecsDTO d) {
        unit.setCompressorQty(d.getCompressorQty());
        unit.setCondenserQty(d.getCondenserQty());
        unit.setExpansionValveQty(d.getExpansionValveQty());
        unit.setChassis(chassisRepository.findById(d.getChassisId())
                .orElseThrow(() -> new ChassisDoesntExistException("Selected chassis doesn't exist.")));
        unit.setFanPI(d.getFanPI());
        unit.setWidth(d.getWidth());
        unit.setLength(d.getLength());
        unit.setHeight(d.getHeight());
        unit.setFanType(d.getFanType());
        unit.setNumberOfFans(d.getNumberOfFans());
        unit.setFanDiameter(d.getFanDiameter());
        unit.setAirflowRate(d.getAirflowRate());
        unit.setDischargeLineDiameter(d.getDischargeLineDiameter());
        unit.setLiquidLineDiameter(d.getLiquidLineDiameter());
        unit.setSuctionLineDiameter(d.getSuctionLineDiameter());
        unit.setGasTank(d.getGasTank());
        unit.setWaterInletConnection(d.getWaterInletConnection());
        unit.setWaterOutletConnection(d.getWaterOutletConnection());
        unit.setMinWaterInlet(d.getMinWaterInlet());
        unit.setMaxWaterInlet(d.getMaxWaterInlet());
        unit.setMinWaterOutlet(d.getMinWaterOutlet());
        unit.setMaxWaterOutlet(d.getMaxWaterOutlet());
        unit.setMinAmbient(d.getMinAmbient());
        unit.setMaxAmbient(d.getMaxAmbient());
    }

    // A unit's compressor can be selected either as an imported Frascold rating (model + refrigerant,
    // the new AW flow) or as a legacy CompressorSpecs. At least one must be present.
    private void applyCompressorSelection(TechSpecs ts, Long compressorSpecsId, Long compressorRatingId) {
        ts.setCompressorRating(compressorRatingId == null ? null :
                compressorRatingRepository.findById(compressorRatingId)
                        .orElseThrow(() -> new CompressorSpecsDoesntExistException("Selected compressor model doesn't exist.")));
        ts.setCompressorSpecs(compressorSpecsId == null ? null :
                compressorSpecsRepository.findById(compressorSpecsId)
                        .orElseThrow(() -> new CompressorSpecsDoesntExistException("Selected compressor doesn't exist.")));
        if (ts.getCompressorRating() == null && ts.getCompressorSpecs() == null) {
            throw new CompressorSpecsDoesntExistException("A compressor must be selected.");
        }
    }

    // Per-mode attributes + the component spec points selected for that mode.
    void applyModeSpecs(TechSpecs ts, UnitTechSpecsDTO d) {
        ts.setCapacity(d.getCapacity());
        ts.setMaxCapacity(d.getMaxCapacity());
        ts.setCopErr(d.getCopErr());
        ts.setCondenserRequiredDuty(d.getCondenserRequiredDuty());
        ts.setQuietCondenserRequiredDuty(d.getQuietCondenserRequiredDuty());

        applyCompressorSelection(ts, d.getCompressorSpecsId(), d.getCompressorRatingId());
        ts.setCondenserSpecs(condenserSpecsRepository.findById(d.getCondenserSpecsId())
                .orElseThrow(() -> new CondenserSpecsDoesntExistException("Selected condenser doesn't exist.")));
        ts.setEvaporatorSpecs(evaporatorSpecsRepository.findById(d.getEvaporatorSpecsId())
                .orElseThrow(() -> new EvaporatorSpecsDoesntExistException("Selected evaporator doesn't exist.")));
        ts.setExpansionValveSpecs(expansionValveSpecsRepository.findById(d.getExpansionValveSpecsId())
                .orElseThrow(() -> new ExpansionValveSpecsDoesntExistException("Selected expansion valve doesn't exist.")));

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
        unit.setChassis(chassisRepository.findById(d.getChassisId())
                .orElseThrow(() -> new ChassisDoesntExistException("Selected chassis doesn't exist.")));
        unit.setFanPI(d.getFanPI());
        unit.setWidth(d.getWidth());
        unit.setLength(d.getLength());
        unit.setHeight(d.getHeight());
        unit.setFanType(d.getFanType());
        unit.setNumberOfFans(d.getNumberOfFans());
        unit.setFanDiameter(d.getFanDiameter());
        unit.setAirflowRate(d.getAirflowRate());
        unit.setDischargeLineDiameter(d.getDischargeLineDiameter());
        unit.setLiquidLineDiameter(d.getLiquidLineDiameter());
        unit.setSuctionLineDiameter(d.getSuctionLineDiameter());
        unit.setGasTank(d.getGasTank());
        unit.setWaterInletConnection(d.getWaterInletConnection());
        unit.setWaterOutletConnection(d.getWaterOutletConnection());
        unit.setMinWaterInlet(d.getMinWaterInlet());
        unit.setMaxWaterInlet(d.getMaxWaterInlet());
        unit.setMinWaterOutlet(d.getMinWaterOutlet());
        unit.setMaxWaterOutlet(d.getMaxWaterOutlet());
        unit.setMinAmbient(d.getMinAmbient());
        unit.setMaxAmbient(d.getMaxAmbient());

        // Unit-level compressor (shared by both heat-pump modes). Resolve and propagate to any modes
        // that already exist so editing the model updates them.
        CompressorRating rating = d.getCompressorRatingId() == null ? null
                : compressorRatingRepository.findById(d.getCompressorRatingId())
                        .orElseThrow(() -> new CompressorSpecsDoesntExistException("Selected compressor model doesn't exist."));
        unit.setCompressorRating(rating);
        if (unit.getUnitDetails() != null) {
            for (UnitDetails md : unit.getUnitDetails()) {
                if (md.getTechSpecs() != null) md.getTechSpecs().setCompressorRating(rating);
            }
        }
    }

    void applyModeSpecs(TechSpecs ts, UnitModeSpecsDTO d) {
        ts.setCapacity(d.getCapacity());
        ts.setMaxCapacity(d.getMaxCapacity());
        ts.setCopErr(d.getCopErr());
        ts.setCondenserRequiredDuty(d.getCondenserRequiredDuty());
        ts.setQuietCondenserRequiredDuty(d.getQuietCondenserRequiredDuty());

        // Heat-pump compressor is a unit-level selection (shared by both modes), applied from
        // unit.compressorRating by the caller — not per mode.
        ts.setCondenserSpecs(condenserSpecsRepository.findById(d.getCondenserSpecsId())
                .orElseThrow(() -> new CondenserSpecsDoesntExistException("Selected condenser doesn't exist.")));
        ts.setEvaporatorSpecs(evaporatorSpecsRepository.findById(d.getEvaporatorSpecsId())
                .orElseThrow(() -> new EvaporatorSpecsDoesntExistException("Selected evaporator doesn't exist.")));
        ts.setExpansionValveSpecs(expansionValveSpecsRepository.findById(d.getExpansionValveSpecsId())
                .orElseThrow(() -> new ExpansionValveSpecsDoesntExistException("Selected expansion valve doesn't exist.")));

        if (d.getFourWayReversingValveSpecsId() != null) {
            ts.setFourWayReversingValveSpecs(fourWayReversingValveSpecsRepository.findById(d.getFourWayReversingValveSpecsId())
                    .orElseThrow(() -> new FourWayReversingValveSpecsDoesntExistException("Selected 4-way reversing valve doesn't exist.")));
        } else {
            ts.setFourWayReversingValveSpecs(null);
        }
    }

    // --- Asset management ---

    @Transactional
    @EvictsUnitCaches
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
    @EvictsUnitCaches
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

    // Soft-delete a unit: kept in the DB (saved-units / projects still reference it),
    // hidden from all listings, and recorded in the audit log.
    @Transactional
    @EvictsUnitCaches
    public void deleteUnit(Long id, String adminEmail) {
        Unit unit = unitJpaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Unit not found: " + id));
        unit.setDeleted(true);
        unitJpaRepository.save(unit);
        Long adminId = adminEmail == null ? -1L
                : userRepositoryPort.findByEmail(adminEmail).map(User::getId).orElse(-1L);
        auditLogService.logAdminAction(adminId, "DELETE", "UNIT", id,
                "Deleted unit " + unit.getModel() + " (" + unit.getCategory().name() + ")");
    }

    void applyCalcValues(DefaultCalculationValues calcValues, UnitDefCalcValuesDTO calcDto) {
        calcValues.setAmbient(calcDto.getAmbient());
        calcValues.setEvapIn(calcDto.getEvapIn());
        calcValues.setEvapOut(calcDto.getEvapOut());
        calcValues.setCondIn(calcDto.getCondIn());
        calcValues.setCondOut(calcDto.getCondOut());
    }
}
