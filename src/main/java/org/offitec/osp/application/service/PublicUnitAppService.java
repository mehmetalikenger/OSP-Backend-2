package org.offitec.osp.application.service;

import org.offitec.osp.domain.entity.*;
import org.offitec.osp.domain.enums.AssetType;
import org.offitec.osp.domain.enums.Mod;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.enums.UnitTypeEnum;
import org.offitec.osp.domain.exception.UnitDoesntExistException;
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
import org.offitec.osp.presentation.dto.UnitCardDTO;
import org.offitec.osp.presentation.dto.UnitDetailPublicDTO;
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

    public PublicUnitAppService(UnitJpaRepository unitJpaRepository,
                                UnitDetailsRepository unitDetailsRepository,
                                CustomCalculationValuesRepository customCalcValsRepository,
                                CalculationOutputValuesRepository calcOutputValsRepository,
                                SavedUnitRepository savedUnitRepository,
                                UserRepository userRepository) {
        this.unitJpaRepository = unitJpaRepository;
        this.unitDetailsRepository = unitDetailsRepository;
        this.customCalcValsRepository = customCalcValsRepository;
        this.calcOutputValsRepository = calcOutputValsRepository;
        this.savedUnitRepository = savedUnitRepository;
        this.userRepository = userRepository;
    }

    // --- Listing ---

    @Transactional(readOnly = true)
    public List<UnitCardDTO> getUnitsByType(UnitCategory category, UnitTypeEnum unitType) {
        Long userId = currentUserId();
        // Query 1: the cards (scalar fields + primary image + saved flag), one statement.
        List<UnitCardDTO> cards = unitJpaRepository.findCards(category, unitType, userId);
        // Query 2: per-mode capacities for all those units at once, stitched in below.
        applyCapacities(cards);
        return cards;
    }

    // --- Detail ---

    @Transactional(readOnly = true)
    public UnitDetailPublicDTO getUnitDetail(Long id) {
        Unit unit = unitJpaRepository.findById(id)
                .orElseThrow(() -> new UnitDoesntExistException("Unit not found."));
        if (unit.isDeleted()) throw new UnitDoesntExistException("Unit not found.");
        Long userId = currentUserId();
        return toDetail(unit, userId);
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
    public List<UnitCardDTO> getSavedUnits(UnitCategory category, UnitTypeEnum unitType) {
        Long userId = currentUserId();
        List<UnitCardDTO> cards = unitJpaRepository.findSavedCards(userId, category, unitType);
        applyCapacities(cards);
        return cards;
    }

    // --- Calculation page data ---

    @Transactional(readOnly = true)
    public UnitCalcDataDTO getUnitCalcData(Long id) {
        Unit unit = unitJpaRepository.findById(id)
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

        CompressorSpecs specs = techSpecs.getCompressorSpecs();
        if (specs == null) throw new RuntimeException("Compressor specs not configured for this unit.");

        double S = dto.getEvapOut() - 5;
        double D = dto.getAmbient() + 15;

        double q = evalPolynomial(S, D,
                specs.getQC1(), specs.getQC2(), specs.getQC3(), specs.getQC4(), specs.getQC5(),
                specs.getQC6(), specs.getQC7(), specs.getQC8(), specs.getQC9(), specs.getQC10());

        double p = evalPolynomial(S, D,
                specs.getPC1(), specs.getPC2(), specs.getPC3(), specs.getPC4(), specs.getPC5(),
                specs.getPC6(), specs.getPC7(), specs.getPC8(), specs.getPC9(), specs.getPC10());

        double totalQ = q * unit.getCompressorQty();
        double totalP = p * unit.getCompressorQty();
        double cop = totalP > 0 ? totalQ / totalP : 0;
        double copEer = cop;

        CustomCalculationValues customVals = new CustomCalculationValues(
                null,
                dto.getAmbient(), dto.getEvapIn(), dto.getEvapOut(), dto.getCondIn(), dto.getCondOut()
        );
        customVals = customCalcValsRepository.save(customVals);

        CalculationOutputValues outputVals = new CalculationOutputValues(
                null,
                totalQ,
                totalQ,
                totalP,
                totalQ + totalP,
                0,
                copEer,
                0,
                0
        );
        outputVals = calcOutputValsRepository.save(outputVals);

        return new CalculationResultDTO(totalQ, totalP, copEer, customVals.getId(), outputVals.getId());
    }

    private double evalPolynomial(double S, double D,
            double c1, double c2, double c3, double c4, double c5,
            double c6, double c7, double c8, double c9, double c10) {
        return c1
                + c2 * S
                + c3 * D
                + c4 * S * S
                + c5 * S * D
                + c6 * D * D
                + c7 * S * S * S
                + c8 * D * S * S
                + c9 * S * D * D
                + c10 * D * D * D;
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
    private record ModeCapacity(Mod mod, double capacity) {}

    // Fills each card's capacity from a SINGLE batched query over all the units'
    // modes, instead of touching the database once per unit (which was the N+1).
    private void applyCapacities(List<UnitCardDTO> cards) {
        if (cards.isEmpty()) return;

        List<Long> unitIds = cards.stream().map(UnitCardDTO::getId).collect(Collectors.toList());

        // Group the flat (unitId, mod, capacity) rows by unit id.
        Map<Long, List<ModeCapacity>> byUnit = unitDetailsRepository.findModeCapacitiesByUnitIds(unitIds)
                .stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(
                                row -> new ModeCapacity((Mod) row[1], ((Number) row[2]).doubleValue()),
                                Collectors.toList())));

        for (UnitCardDTO card : cards) {
            card.setCapacityRange(formatCapacity(byUnit.get(card.getId())));
        }
    }

    // Per-mode capacity from a loaded unit (used by the detail/spec views, which
    // already have the UnitDetails graph in hand).
    private String capacityFromUnit(Unit unit) {
        if (unit.getUnitDetails() == null) return null;
        List<ModeCapacity> modes = new ArrayList<>();
        for (UnitDetails d : unit.getUnitDetails()) {
            if (d.getTechSpecs() != null) {
                modes.add(new ModeCapacity(d.getMod(), d.getTechSpecs().getCapacity()));
            }
        }
        return formatCapacity(modes);
    }

    // Single mode -> "38.0 kW". Multiple modes -> "45.0 kW (Heating), 38.0 kW (Cooling)"
    // with Heating listed first.
    private String formatCapacity(List<ModeCapacity> modes) {
        if (modes == null || modes.isEmpty()) return null;

        if (modes.size() == 1) {
            return String.format("%.1f kW", modes.get(0).capacity());
        }

        return modes.stream()
                .sorted(Comparator.comparingInt(m -> modeOrder(m.mod())))
                .map(m -> String.format("%.1f kW (%s)", m.capacity(), modeLabel(m.mod())))
                .collect(Collectors.joining(", "));
    }

    private int modeOrder(Mod mod) {
        return mod == Mod.HEATING ? 0 : 1; // Heating before Cooling
    }

    private String modeLabel(Mod mod) {
        String lower = mod.name().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1); // Heating / Cooling
    }

    private UnitDetailPublicDTO toDetail(Unit unit, Long userId) {
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
        boolean saved = userId != null && savedUnitRepository.existsByUserIdAndUnitId(userId, unit.getId());

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
                saved
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

        if (unit.getRefrigerant() != null) addSpec(specs, "Refrigerant", unit.getRefrigerant().getCode());
        if (unit.getCompressorQty() > 0) addSpec(specs, "Compressors", String.valueOf(unit.getCompressorQty()));
        if (unit.getCondenserQty() > 0) addSpec(specs, "Condensers", String.valueOf(unit.getCondenserQty()));
        if (unit.getExpansionValveQty() > 0) addSpec(specs, "Expansion Valves", String.valueOf(unit.getExpansionValveQty()));
        if (unit.getNumberOfFans() > 0) addSpec(specs, "Number of Fans", String.valueOf(unit.getNumberOfFans()));
        if (unit.getFanDiameter() > 0) addSpec(specs, "Fan Diameter", fmtNum(unit.getFanDiameter()) + " mm");
        if (unit.getAirflowRate() > 0) addSpec(specs, "Airflow Rate", fmtNum(unit.getAirflowRate()) + " m³/h");
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

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String email = auth.getName();
        return userRepository.findByEmail(email).map(User::getId).orElse(null);
    }
}
