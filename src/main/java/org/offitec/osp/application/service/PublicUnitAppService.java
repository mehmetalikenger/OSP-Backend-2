package org.offitec.osp.application.service;

import org.offitec.osp.domain.entity.*;
import org.offitec.osp.domain.enums.AssetType;
import org.offitec.osp.domain.enums.CondenserType;
import org.offitec.osp.domain.enums.EvaporatorType;
import org.offitec.osp.domain.enums.Mod;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.enums.UnitTypeEnum;
import org.offitec.osp.domain.exception.UnitDoesntExistException;
import org.offitec.osp.infrastructure.config.CacheConfig;
import org.offitec.osp.infrastructure.repository.CalculationOutputValuesRepository;
import org.offitec.osp.infrastructure.repository.CustomCalculationValuesRepository;
import org.offitec.osp.infrastructure.repository.SavedUnitRepository;
import org.offitec.osp.infrastructure.repository.UnitDetailsRepository;
import org.offitec.osp.infrastructure.repository.UnitJpaRepository;
import org.offitec.osp.infrastructure.repository.UserRepository;
import org.offitec.osp.presentation.dto.CalcAssetDTO;
import org.offitec.osp.presentation.dto.CalculationRequestDTO;
import org.offitec.osp.presentation.dto.CalculationResultDTO;
import org.offitec.osp.presentation.dto.DefCalcValuesPublicDTO;
import org.offitec.osp.presentation.dto.TechSpecItemDTO;
import org.offitec.osp.presentation.dto.UnitCalcDataDTO;
import org.offitec.osp.presentation.dto.PageResponse;
import org.offitec.osp.presentation.dto.UnitCardDTO;
import org.offitec.osp.presentation.dto.UnitDetailPublicDTO;
import org.offitec.osp.presentation.dto.UnitMatchRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PublicUnitAppService {

    private final UnitJpaRepository unitJpaRepository;
    private final UnitDetailsRepository unitDetailsRepository;
    private final CustomCalculationValuesRepository customCalcValsRepository;
    private final CalculationOutputValuesRepository calcOutputValsRepository;
    private final SavedUnitRepository savedUnitRepository;
    private final UserRepository userRepository;
    private final UnitCalculationEngine calculationEngine;
    private final CompressorPerformanceEngine performanceEngine;

    public PublicUnitAppService(UnitJpaRepository unitJpaRepository,
                                UnitDetailsRepository unitDetailsRepository,
                                CustomCalculationValuesRepository customCalcValsRepository,
                                CalculationOutputValuesRepository calcOutputValsRepository,
                                SavedUnitRepository savedUnitRepository,
                                UserRepository userRepository,
                                UnitCalculationEngine calculationEngine,
                                CompressorPerformanceEngine performanceEngine) {
        this.unitJpaRepository = unitJpaRepository;
        this.unitDetailsRepository = unitDetailsRepository;
        this.customCalcValsRepository = customCalcValsRepository;
        this.calcOutputValsRepository = calcOutputValsRepository;
        this.savedUnitRepository = savedUnitRepository;
        this.userRepository = userRepository;
        this.calculationEngine = calculationEngine;
        this.performanceEngine = performanceEngine;
    }

    // Self-reference so the cached, user-independent loader is invoked through the Spring
    // proxy (a plain in-class call would bypass the @Cacheable advice). @Lazy breaks the
    // self-referential construction cycle.
    @Autowired
    @Lazy
    private PublicUnitAppService self;

    // --- Listing ---

    @Transactional(readOnly = true)
    public PageResponse<UnitCardDTO> getUnitsByType(UnitCategory category, UnitTypeEnum unitType, Pageable pageable) {
        Long userId = currentUserId();
        // Query 1: one page of cards (scalar fields + primary image + saved flag).
        Page<UnitCardDTO> page = unitJpaRepository.findCards(category, unitType, userId, pageable);
        // Query 2..4: per-mode capacities, icon URLs and refrigerant codes for this page's units.
        applyCapacities(page.getContent());
        applyIcons(page.getContent());
        applyRefrigerants(page.getContent());
        return toPageResponse(page);
    }

    // --- Capacity match (products page) ---

    /**
     * Computes each candidate unit's refrigerating capacity from the user's operating
     * conditions (the unit's compressor polynomial, scaled by compressor count) and returns
     * the cards for units whose capacity lands within {@code targetCapacity ± diffPercent}.
     * Candidates are the units of the given category/type, optionally narrowed by refrigerant.
     */
    @Transactional(readOnly = true)
    public List<UnitCardDTO> matchUnits(UnitMatchRequestDTO dto) {
        UnitCategory category = UnitCategory.valueOf(dto.getCategory().trim().toUpperCase());
        UnitTypeEnum type = UnitTypeEnum.valueOf(dto.getType().trim().toUpperCase());
        String refCode = (dto.getRefrigerant() == null || dto.getRefrigerant().isBlank())
                ? null : dto.getRefrigerant().trim();

        double target = dto.getTargetCapacity() != null ? dto.getTargetCapacity() : 0.0;
        double pct = Math.max(dto.getDiffPercent(), 0.0);
        double lo = target * (1.0 - pct / 100.0);
        double hi = target * (1.0 + pct / 100.0);

        List<Long> matchedIds = new ArrayList<>();
        for (Unit unit : unitJpaRepository.findUnitsForMatching(category, type, refCode)) {
            if (unit.getUnitDetails() == null) continue;
            // Capacity is evaluated on the cooling mode (the rating point for chillers).
            UnitDetails cooling = unit.getUnitDetails().stream()
                    .filter(d -> d.getMod() == Mod.COOLING
                            && d.getTechSpecs() != null
                            && d.getTechSpecs().getCompressorSpecs() != null)
                    .findFirst()
                    .orElse(null);
            if (cooling == null) continue;

            double capacityKw = calculationEngine.compute(
                    cooling.getTechSpecs().getCompressorSpecs(),
                    unit.getCompressorQty(),
                    dto.getAmbient(),
                    dto.getEvapOut()).capacityKw();

            if (capacityKw >= lo && capacityKw <= hi) matchedIds.add(unit.getId());
        }

        if (matchedIds.isEmpty()) return List.of();

        List<UnitCardDTO> cards = unitJpaRepository.findCardsByIds(matchedIds, currentUserId());
        applyCapacities(cards);
        applyIcons(cards);
        applyRefrigerants(cards);
        return cards;
    }

    // --- Detail ---

    @Transactional(readOnly = true)
    public UnitDetailPublicDTO getUnitDetail(Long id) {
        // The heavy, user-independent part (specs/images/description) is cached by id;
        // only the per-user `saved` flag is computed per request and overlaid here.
        UnitDetailPublicDTO base = self.loadStaticDetail(id);
        Long userId = currentUserId();
        boolean saved = userId != null && savedUnitRepository.existsByUserIdAndUnitId(userId, id);
        return new UnitDetailPublicDTO(
                base.getId(), base.getName(), base.getModel(), base.getDescription(),
                base.getPrimaryImageUrl(), base.getIconUrls(), base.getSpecs(),
                base.getUnitType(), base.getCategory(), saved
        );
    }

    // Cached, user-independent unit detail (saved = false). Loaded via a single
    // fetch-join query so it doesn't walk the lazy component graph. Evicted on admin
    // unit/asset writes (see UnitAppService @CacheEvict).
    @Cacheable(value = CacheConfig.UNIT_DETAIL, key = "#id")
    @Transactional(readOnly = true)
    public UnitDetailPublicDTO loadStaticDetail(Long id) {
        Unit unit = unitJpaRepository.findDetailGraphById(id)
                .orElseThrow(() -> new UnitDoesntExistException("Unit not found."));
        if (unit.isDeleted()) throw new UnitDoesntExistException("Unit not found.");
        return toDetail(unit);
    }

    // --- Save / Unsave ---

    @Transactional
    public boolean toggleSave(Long unitId) {
        Long userId = currentUserId();
        Optional<SavedUnit> existing = savedUnitRepository.findByUserIdAndUnitId(userId, unitId);
        if (existing.isPresent()) {
            savedUnitRepository.delete(existing.get());
            return false;
        }
        Unit unit = unitJpaRepository.findById(unitId)
                .orElseThrow(() -> new UnitDoesntExistException("Unit not found."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found."));
        SavedUnit saved = new SavedUnit();
        saved.setUser(user);
        saved.setUnit(unit);
        savedUnitRepository.save(saved);
        return true;
    }

    // --- Saved units listing ---

    @Transactional(readOnly = true)
    public PageResponse<UnitCardDTO> getSavedUnits(UnitCategory category, UnitTypeEnum unitType, Pageable pageable) {
        Long userId = currentUserId();
        Page<UnitCardDTO> page = unitJpaRepository.findSavedCards(userId, category, unitType, pageable);
        applyCapacities(page.getContent());
        applyIcons(page.getContent());
        applyRefrigerants(page.getContent());
        return toPageResponse(page);
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.hasNext());
    }

    // --- Calculation page data ---

    // Fully user-independent, so the whole payload is cached by id (evicted on admin
    // unit/asset writes). Loaded via the single fetch-join query.
    @Cacheable(value = CacheConfig.UNIT_CALC_DATA, key = "#id")
    @Transactional(readOnly = true)
    public UnitCalcDataDTO getUnitCalcData(Long id) {
        Unit unit = unitJpaRepository.findDetailGraphById(id)
                .orElseThrow(() -> new UnitDoesntExistException("Unit not found."));

        List<CalcAssetDTO> images = new ArrayList<>();
        List<CalcAssetDTO> drawings = new ArrayList<>();
        List<CalcAssetDTO> documents = new ArrayList<>();

        if (unit.getAssets() != null) {
            for (UnitAsset a : unit.getAssets()) {
                switch (a.getAssetType()) {
                    case IMAGE   -> images.add(new CalcAssetDTO(a.getUrl(), null));
                    case DRAWING -> drawings.add(new CalcAssetDTO(a.getUrl(), null));
                    case DOCUMENT -> documents.add(new CalcAssetDTO(a.getUrl(), extractFileName(a.getUrl())));
                    default -> {}
                }
            }
        }

        DefCalcValuesPublicDTO coolingDefaults = null;
        DefCalcValuesPublicDTO heatingDefaults = null;

        if (unit.getUnitDetails() != null) {
            for (UnitDetails d : unit.getUnitDetails()) {
                DefaultCalculationValues dcv = d.getDefCalcValues();
                if (dcv == null) continue;
                DefCalcValuesPublicDTO dto = new DefCalcValuesPublicDTO(
                        dcv.getAmbient(),
                        dcv.getEvapIn(), dcv.getEvapOut(), dcv.getCondIn(), dcv.getCondOut()
                );
                if (d.getMod() == Mod.COOLING) coolingDefaults = dto;
                else if (d.getMod() == Mod.HEATING) heatingDefaults = dto;
            }
        }

        return new UnitCalcDataDTO(
                unit.getName(),
                unit.getModel(),
                unit.getCategory().name(),
                images,
                drawings,
                documents,
                buildSpecs(unit),
                coolingDefaults,
                heatingDefaults
        );
    }

    // --- Calculation ---

    @Transactional
    public CalculationResultDTO calculate(CalculationRequestDTO dto) {
        Unit unit = unitJpaRepository.findById(dto.getUnitId())
                .orElseThrow(() -> new UnitDoesntExistException("Unit not found."));

        Mod mod = Mod.valueOf(dto.getMod().trim().toUpperCase());

        UnitDetails details = unitDetailsRepository.findByUnitIdAndMod(dto.getUnitId(), mod)
                .orElseThrow(() -> new RuntimeException("Unit details not found for mod: " + mod));

        TechSpecs techSpecs = details.getTechSpecs();
        if (techSpecs == null) throw new RuntimeException("Tech specs not configured for this unit.");

        GlycolCorrection.Factors gf = mod == Mod.HEATING
                ? GlycolCorrection.Factors.NONE
                : GlycolCorrection.lookup(dto.getGlycolType(), dto.getGlycolPercentage());

        // Prefer the faithful FSS3 engine when the unit is linked to an imported, calculable rating.
        CompressorRating rating = techSpecs.getCompressorRating();
        if (canUseFaithfulEngine(rating)) {
            CalculationResultDTO faithful = calculateFaithful(unit, rating, mod, dto, gf);
            if (faithful != null) return faithful;
            // fall through to the legacy engine if the operating point produced no valid cycle
        }

        CompressorSpecs specs = techSpecs.getCompressorSpecs();
        if (specs == null) throw new RuntimeException("Compressor specs not configured for this unit.");

        // Heating is rated on the hot-water (condenser) outlet; cooling on the chilled-water
        // (evaporator) outlet. The engine derives the polynomial variables from the mode and
        // picks the ISCR/Copeland trivariate formula when applicable.
        double leavingWaterTemp = mod == Mod.HEATING ? dto.getCondOut() : dto.getEvapOut();
        UnitCalculationEngine.Result design = calculationEngine.compute(
                specs, unit.getCompressorQty(), mod, dto.getAmbient(), leavingWaterTemp);

        // A selected glycol mixture scales capacity, power and pressure drop by its factors.
        // Heating heats the water (no glycol needed), so the correction is skipped for heating.
        double refrigeratingKw = design.capacityKw() * gf.capacity();   // evaporator/cooling capacity
        double totalP = design.powerKw() * gf.power();
        double condenserDuty = refrigeratingKw + totalP;                // heat rejected at the condenser

        // In heating the useful output is the heat delivered at the condenser (= evaporator capacity
        // + compressor power); in cooling it's the evaporator/refrigerating capacity. COP/EER and the
        // water flow rate follow whichever is the headline figure.
        double headlineCapacity = mod == Mod.HEATING ? condenserDuty : refrigeratingKw;
        double copEer = totalP > 0 ? headlineCapacity / totalP : 0;

        double pressureDrop = 50.0 * gf.pressureDrop();

        // Flow rate derived from the headline capacity, matching the report assembler so the
        // calculation page and the PDF show the same value: capacity(kW) * 860 / 5000 (m³/h).
        double flowRate = headlineCapacity * 860.0 / 5000.0;

        CustomCalculationValues customVals = customCalcValsRepository.save(new CustomCalculationValues(
                null, mod,
                dto.getAmbient(), dto.getEvapIn(), dto.getEvapOut(), dto.getCondIn(), dto.getCondOut(),
                dto.getGlycolType(), dto.getGlycolPercentage()));

        CalculationOutputValues outputVals = calcOutputValsRepository.save(new CalculationOutputValues(
                null, mod, refrigeratingKw, refrigeratingKw, totalP, condenserDuty, 0, copEer, 0, 0, pressureDrop));

        return new CalculationResultDTO(headlineCapacity, totalP, copEer, flowRate, pressureDrop,
                customVals.getId(), outputVals.getId(),
                0, mod == Mod.HEATING ? condenserDuty : 0, 0, false, false);
    }

    private boolean canUseFaithfulEngine(CompressorRating rating) {
        return rating != null
                && rating.isCalculable()
                && rating.getRefrigerant() != null
                && rating.getRefrigerant().getCoolpropName() != null
                && performanceEngine.isAvailable();
    }

    // Runs the ported FSS3 cycle. Te/Tc come from the unit's approach bridge:
    //   AW (air-source condenser):
    //     Cooling: Te = chilled-water outlet − 5, Tc = ambient + 15
    //     Heating: Te = ambient − 5,             Tc = hot-water outlet + 5
    //   WW (water-source condenser):
    //     Cooling & Heating: Te = evaporator-water outlet − 5, Tc = condenser-water outlet + 5
    // Returns null if the cycle is not valid (caller falls back to the legacy engine).
    private CalculationResultDTO calculateFaithful(Unit unit, CompressorRating rating, Mod mod,
                                                   CalculationRequestDTO dto, GlycolCorrection.Factors gf) {
        double te, tc;
        if (unit.getUnitType() == org.offitec.osp.domain.enums.UnitTypeEnum.WW) {
            te = dto.getEvapOut() - 5.0;
            tc = dto.getCondOut() + 5.0;
        } else { // AW
            te = mod == Mod.HEATING ? dto.getAmbient() - 5.0 : dto.getEvapOut() - 5.0;
            tc = mod == Mod.HEATING ? dto.getCondOut() + 5.0 : dto.getAmbient() + 15.0;
        }

        double frequency = dto.getFrequencyHz() != null ? dto.getFrequencyHz() : 50.0;
        double subcooling = dto.getSubcooling() != null ? dto.getSubcooling() : 0.0;
        CompressorPerformanceEngine.Suction suction = dto.getSuctionGasTemp() != null
                ? new CompressorPerformanceEngine.SuctionGasTemp(dto.getSuctionGasTemp())
                : new CompressorPerformanceEngine.Superheat(dto.getSuperheat() != null ? dto.getSuperheat() : 10.0);

        boolean transcritical = "TK".equalsIgnoreCase(rating.getCompressor().getFrascoldType());

        CompressorPerformanceEngine.Result res = performanceEngine.compute(new CompressorPerformanceEngine.Input(
                rating, rating.getRefrigerant().getCoolpropName(), transcritical,
                te, tc, suction, subcooling, frequency, null));
        if (!res.valid()) return null;

        int qty = Math.max(unit.getCompressorQty(), 1);
        double refrigeratingKw = res.coolingCapacityW() * qty / 1000.0 * gf.capacity();   // kW, evaporator side
        double totalP = res.powerInputW() * qty / 1000.0 * gf.power();                    // kW
        double condenserDuty = res.condenserDutyW() * qty / 1000.0;                       // kW, heat rejected at condenser
        double evaporatorDuty = res.evaporatorDutyW() * qty / 1000.0 * gf.capacity();
        double massFlow = res.massFlowKgH() * qty;

        // In heating the useful output is the heat delivered at the condenser; in cooling it's the
        // evaporator/refrigerating capacity. COP/EER and the water flow rate follow the headline figure.
        double headlineCapacity = mod == Mod.HEATING ? condenserDuty : refrigeratingKw;
        double copEer = totalP > 0 ? headlineCapacity / totalP : 0;
        double pressureDrop = 50.0 * gf.pressureDrop();
        double flowRate = headlineCapacity * 860.0 / 5000.0;

        CustomCalculationValues customVals = customCalcValsRepository.save(new CustomCalculationValues(
                null, mod,
                dto.getAmbient(), dto.getEvapIn(), dto.getEvapOut(), dto.getCondIn(), dto.getCondOut(),
                dto.getGlycolType(), dto.getGlycolPercentage()));

        CalculationOutputValues outputVals = calcOutputValsRepository.save(new CalculationOutputValues(
                null, mod, refrigeratingKw, evaporatorDuty, totalP, condenserDuty, 0, copEer, massFlow, frequency, pressureDrop));

        return new CalculationResultDTO(headlineCapacity, totalP, copEer, flowRate, pressureDrop,
                customVals.getId(), outputVals.getId(),
                massFlow, condenserDuty, res.dischargeTempC(), res.withinEnvelope(), true);
    }

    private String extractFileName(String url) {
        try {
            String path = new java.net.URI(url).getPath();
            int lastSlash = path.lastIndexOf('/');
            String name = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
            // Strip the UUID prefix added by buildKey: "{uuid}-originalName"
            name = name.replaceFirst("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}-", "");
            return name.isBlank() ? "Document" : name;
        } catch (Exception e) {
            return "Document";
        }
    }

    // --- Mapping helpers ---

    // A unit's capacity for one mode. Used to format the capacity label the same way
    // for both the catalog cards and the detail/spec views.
    private record ModeCapacity(Mod mod, double capacity, double maxCapacity) {}

    // Fills each card's capacity from a SINGLE batched query over all the units'
    // modes, instead of touching the database once per unit (which was the N+1).
    private void applyCapacities(List<UnitCardDTO> cards) {
        if (cards.isEmpty()) return;

        List<Long> unitIds = cards.stream().map(UnitCardDTO::getId).collect(Collectors.toList());

        // Group the flat (unitId, mod, capacity, maxCapacity) rows by unit id.
        Map<Long, List<ModeCapacity>> byUnit = unitDetailsRepository.findModeCapacitiesByUnitIds(unitIds)
                .stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(
                                row -> new ModeCapacity((Mod) row[1],
                                        row[2] != null ? ((Number) row[2]).doubleValue() : 0.0,
                                        row[3] != null ? ((Number) row[3]).doubleValue() : 0.0),
                                Collectors.toList())));

        for (UnitCardDTO card : cards) {
            card.setCapacityRange(formatCapacity(byUnit.get(card.getId())));
        }
    }

    // Fills each card's icon URLs from a SINGLE batched query over all the units' icon
    // assets (same N+1-avoidance as applyCapacities).
    private void applyIcons(List<UnitCardDTO> cards) {
        if (cards.isEmpty()) return;
        List<Long> unitIds = cards.stream().map(UnitCardDTO::getId).collect(Collectors.toList());

        Map<Long, List<String>> byUnit = unitJpaRepository.findIconUrls(unitIds).stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> (String) row[1], Collectors.toList())));

        for (UnitCardDTO card : cards) {
            card.setIconUrls(byUnit.getOrDefault(card.getId(), List.of()));
        }
    }

    // Fills each card's refrigerant code from a SINGLE batched query that derives the code
    // from the unit's compressor (refrigerant moved to the Compressor). A unit may yield
    // several rows (one per mode); the first is kept as the representative.
    private void applyRefrigerants(List<UnitCardDTO> cards) {
        if (cards.isEmpty()) return;
        List<Long> unitIds = cards.stream().map(UnitCardDTO::getId).collect(Collectors.toList());

        Map<Long, String> byUnit = unitJpaRepository.findRefrigerantCodesByUnitIds(unitIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (String) row[1],
                        (first, dup) -> first));

        for (UnitCardDTO card : cards) {
            card.setRefrigerant(byUnit.get(card.getId()));
        }
    }

    // Per-mode capacity from a loaded unit (used by the detail/spec views, which
    // already have the UnitDetails graph in hand).
    private String capacityFromUnit(Unit unit) {
        if (unit.getUnitDetails() == null) return null;
        List<ModeCapacity> modes = new ArrayList<>();
        for (UnitDetails d : unit.getUnitDetails()) {
            if (d.getTechSpecs() != null) {
                Double max = d.getTechSpecs().getMaxCapacity();
                modes.add(new ModeCapacity(d.getMod(), d.getTechSpecs().getCapacity(),
                        max != null ? max : 0.0));
            }
        }
        return formatCapacity(modes);
    }

    // Single mode -> "38.0 - 45.0 kW". Multiple modes -> "45.0 - 52.0 kW (Heating), ..."
    // with Heating listed first. When a mode has no max capacity, just the single value.
    private String formatCapacity(List<ModeCapacity> modes) {
        if (modes == null || modes.isEmpty()) return null;

        if (modes.size() == 1) {
            return capRange(modes.get(0));
        }

        return modes.stream()
                .sorted(Comparator.comparingInt(m -> modeOrder(m.mod())))
                .map(m -> capRange(m) + " (" + modeLabel(m.mod()) + ")")
                .collect(Collectors.joining(", "));
    }

    // Just the single capacity value ("38.0 kW"); the max capacity is intentionally not shown.
    private String capRange(ModeCapacity m) {
        return String.format("%.1f kW", m.capacity());
    }

    private int modeOrder(Mod mod) {
        return mod == Mod.HEATING ? 0 : 1; // Heating before Cooling
    }

    private String modeLabel(Mod mod) {
        String lower = mod.name().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1); // Heating / Cooling
    }

    // Builds the user-independent detail (saved = false); the caller overlays the
    // per-user saved flag. Kept free of request state so the result is cacheable.
    private UnitDetailPublicDTO toDetail(Unit unit) {
        String primaryImageUrl = null;
        List<String> iconUrls = new ArrayList<>();

        List<UnitAsset> assets = unit.getAssets();
        if (assets != null) {
            for (UnitAsset a : assets) {
                if (a.getAssetType() == AssetType.IMAGE && a.isPrimary()) {
                    primaryImageUrl = a.getUrl();
                } else if (a.getAssetType() == AssetType.ICON) {
                    iconUrls.add(a.getUrl());
                }
            }
        }

        List<TechSpecItemDTO> specs = buildSpecs(unit);

        return new UnitDetailPublicDTO(
                unit.getId(),
                unit.getName(),
                unit.getModel(),
                unit.getDescription(),
                primaryImageUrl,
                iconUrls,
                specs,
                unit.getUnitType().name(),
                unit.getCategory().name(),
                false
        );
    }

    private List<TechSpecItemDTO> buildSpecs(Unit unit) {
        List<TechSpecItemDTO> specs = new ArrayList<>();

        addSpec(specs, "Capacity", capacityFromUnit(unit));

        if (unit.getUnitDetails() != null) {
            for (UnitDetails d : unit.getUnitDetails()) {
                TechSpecs ts = d.getTechSpecs();
                if (ts != null && ts.getCopErr() > 0) {
                    String label = d.getMod() == Mod.COOLING ? "EER" : "COP";
                    addSpec(specs, label, String.format("%.2f", ts.getCopErr()));
                }
            }
        }

        Refrigerant refrigerant = unitRefrigerant(unit);
        if (refrigerant != null) addSpec(specs, "Refrigerant", refrigerant.getCode());

        // Chassis is a unit-level selection.
        if (unit.getChassis() != null) addSpec(specs, "Chassis Model", unit.getChassis().getModel());

        // Component brand/model/type/capacity, taken from the first configured mode (shared across modes).
        CompressorSpecs compressorSpecs = firstCompressorSpecs(unit);
        if (compressorSpecs != null && compressorSpecs.getCompressor() != null) {
            Compressor compressor = compressorSpecs.getCompressor();
            addSpec(specs, "Compressor Brand", compressor.getBrand());
            addSpec(specs, "Compressor Model", compressor.getModel());
            if (compressor.getType() != null) addSpec(specs, "Compressor Type", compressor.getType().name());
            addSpec(specs, "Compressor Capacity", capacityKw(compressorSpecs.getCapacity()));
            // MOC = compressor qty * per-compressor MOC; LRA shown as-is.
            if (compressor.getMoc() != null && compressor.getMoc() > 0) {
                double totalMoc = compressor.getMoc() * Math.max(unit.getCompressorQty(), 1);
                addSpec(specs, "MOC", fmtNum(totalMoc) + " A");
            }
            if (compressor.getLra() != null && compressor.getLra() > 0) {
                addSpec(specs, "LRA", fmtNum(compressor.getLra()) + " A");
            }
        }

        CondenserSpecs condenserSpecs = firstCondenserSpecs(unit);
        if (condenserSpecs != null && condenserSpecs.getCondenser() != null) {
            Condenser condenser = condenserSpecs.getCondenser();
            addSpec(specs, "Condenser Brand", condenser.getBrand());
            addSpec(specs, "Condenser Model", condenser.getModel());
            addSpec(specs, "Condenser Type", condenserTypeLabel(condenser.getType()));
            addSpec(specs, "Condenser Capacity", capacityKw(condenserSpecs.getCapacity()));
        }

        EvaporatorSpecs evaporatorSpecs = firstEvaporatorSpecs(unit);
        if (evaporatorSpecs != null && evaporatorSpecs.getEvaporator() != null) {
            Evaporator evaporator = evaporatorSpecs.getEvaporator();
            addSpec(specs, "Evaporator Brand", evaporator.getBrand());
            addSpec(specs, "Evaporator Model", evaporator.getModel());
            addSpec(specs, "Evaporator Type", evaporatorTypeLabel(evaporator.getType()));
            addSpec(specs, "Evaporator Capacity", capacityKw(evaporatorSpecs.getCapacity()));
        }

        ExpansionValveSpecs expansionValveSpecs = firstExpansionValveSpecs(unit);
        if (expansionValveSpecs != null && expansionValveSpecs.getExpansionValve() != null) {
            ExpansionValve expansionValve = expansionValveSpecs.getExpansionValve();
            addSpec(specs, "Expansion Valve Brand", expansionValve.getBrand());
            addSpec(specs, "Expansion Valve Model", expansionValve.getModel());
            addSpec(specs, "Expansion Valve Capacity", capacityKw(expansionValveSpecs.getCapacity()));
        }

        FourWayReversingValveSpecs fourWayValveSpecs = firstFourWayReversingValveSpecs(unit);
        if (fourWayValveSpecs != null && fourWayValveSpecs.getFourWayReversingValve() != null) {
            FourWayReversingValve fourWayValve = fourWayValveSpecs.getFourWayReversingValve();
            addSpec(specs, "4-Way Reversing Valve Brand", fourWayValve.getBrand());
            addSpec(specs, "4-Way Reversing Valve Model", fourWayValve.getModel());
            addSpec(specs, "4-Way Reversing Valve Capacity", capacityKw(fourWayValveSpecs.getCapacity()));
        }

        if (unit.getCompressorQty() > 0) addSpec(specs, "Compressors", String.valueOf(unit.getCompressorQty()));
        if (unit.getCondenserQty() > 0) addSpec(specs, "Condensers", String.valueOf(unit.getCondenserQty()));
        if (unit.getExpansionValveQty() > 0) addSpec(specs, "Expansion Valves", String.valueOf(unit.getExpansionValveQty()));
        if (unit.getNumberOfFans() > 0) addSpec(specs, "Number of Fans", String.valueOf(unit.getNumberOfFans()));
        if (unit.getFanDiameter() > 0) addSpec(specs, "Fan Diameter", fmtNum(unit.getFanDiameter()) + " mm");
        if (unit.getAirflowRate() > 0) {
            // Total air flow = fan quantity * per-fan air flow rate.
            double totalAirflow = Math.max(unit.getNumberOfFans(), 1) * unit.getAirflowRate();
            addSpec(specs, "Airflow Rate", fmtNum(totalAirflow) + " m³/h");
        }
        if (unit.getFanPI() > 0) addSpec(specs, "Fan Power Input", fmtNum(unit.getFanPI()) + " kW");
        if (unit.getWidth() > 0 && unit.getHeight() > 0 && unit.getLength() > 0) {
            addSpec(specs, "Dimensions (W×H×L)", String.format("%.0f × %.0f × %.0f mm", unit.getWidth(), unit.getHeight(), unit.getLength()));
        }
        if (unit.getGasTank() > 0) addSpec(specs, "Gas Tank", fmtNum(unit.getGasTank()) + " L");
        if (unit.getDischargeLineDiameter() != null && !unit.getDischargeLineDiameter().isBlank()) {
            addSpec(specs, "Discharge Line Diameter", unit.getDischargeLineDiameter());
        }
        if (unit.getLiquidLineDiameter() != null && !unit.getLiquidLineDiameter().isBlank()) {
            addSpec(specs, "Liquid Line Diameter", unit.getLiquidLineDiameter());
        }
        if (unit.getSuctionLineDiameter() != null && !unit.getSuctionLineDiameter().isBlank()) {
            addSpec(specs, "Suction Line Diameter", unit.getSuctionLineDiameter());
        }
        addSpec(specs, "Water Inlet Connection", unit.getWaterInletConnection());
        addSpec(specs, "Water Outlet Connection", unit.getWaterOutletConnection());

        return specs;
    }

    private String fmtNum(double v) {
        if (v == Math.rint(v) && !Double.isInfinite(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }

    private void addSpec(List<TechSpecItemDTO> specs, String label, String value) {
        if (value != null && !value.isBlank()) {
            specs.add(new TechSpecItemDTO(label, value));
        }
    }

    // --- Component lookups (return the first occurrence across the unit's modes) ---

    private CompressorSpecs firstCompressorSpecs(Unit unit) {
        if (unit.getUnitDetails() == null) return null;
        for (UnitDetails d : unit.getUnitDetails()) {
            TechSpecs ts = d.getTechSpecs();
            if (ts != null && ts.getCompressorSpecs() != null) return ts.getCompressorSpecs();
        }
        return null;
    }

    // The unit's refrigerant is now derived from its compressor (the first configured mode's,
    // shared across modes). Null when no compressor/refrigerant is set.
    private Refrigerant unitRefrigerant(Unit unit) {
        CompressorSpecs cs = firstCompressorSpecs(unit);
        if (cs == null || cs.getCompressor() == null) return null;
        return cs.getCompressor().getRefrigerant();
    }

    private CondenserSpecs firstCondenserSpecs(Unit unit) {
        if (unit.getUnitDetails() == null) return null;
        for (UnitDetails d : unit.getUnitDetails()) {
            TechSpecs ts = d.getTechSpecs();
            if (ts != null && ts.getCondenserSpecs() != null) return ts.getCondenserSpecs();
        }
        return null;
    }

    private EvaporatorSpecs firstEvaporatorSpecs(Unit unit) {
        if (unit.getUnitDetails() == null) return null;
        for (UnitDetails d : unit.getUnitDetails()) {
            TechSpecs ts = d.getTechSpecs();
            if (ts != null && ts.getEvaporatorSpecs() != null) return ts.getEvaporatorSpecs();
        }
        return null;
    }

    private ExpansionValveSpecs firstExpansionValveSpecs(Unit unit) {
        if (unit.getUnitDetails() == null) return null;
        for (UnitDetails d : unit.getUnitDetails()) {
            TechSpecs ts = d.getTechSpecs();
            if (ts != null && ts.getExpansionValveSpecs() != null) return ts.getExpansionValveSpecs();
        }
        return null;
    }

    private FourWayReversingValveSpecs firstFourWayReversingValveSpecs(Unit unit) {
        if (unit.getUnitDetails() == null) return null;
        for (UnitDetails d : unit.getUnitDetails()) {
            TechSpecs ts = d.getTechSpecs();
            if (ts != null && ts.getFourWayReversingValveSpecs() != null) return ts.getFourWayReversingValveSpecs();
        }
        return null;
    }

    private String capacityKw(double capacity) {
        if (capacity <= 0) return null;
        return fmtNum(capacity) + " kW";
    }

    // Type labels mirror the options shown in the admin component forms.

    private String condenserTypeLabel(CondenserType type) {
        if (type == null) return null;
        return switch (type) {
            case MICROCHANNEL -> "Microchannel";
        };
    }

    private String evaporatorTypeLabel(EvaporatorType type) {
        if (type == null) return null;
        return switch (type) {
            case PLATE -> "Plate";
            case COIL -> "Coil";
            case SHELL_AND_TUBE -> "Shell & Tube";
        };
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        // JwtFilter stashes the user id in the authentication details, so we avoid a
        // per-request DB lookup. Fall back to email if it isn't present for some reason.
        if (auth.getDetails() instanceof Long id) return id;
        String email = auth.getName();
        if (email == null) return null;
        return userRepository.findByEmail(email).map(User::getId).orElse(null);
    }
}
