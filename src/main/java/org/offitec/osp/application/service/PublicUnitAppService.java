package org.offitec.osp.application.service;

import org.offitec.osp.domain.entity.*;
import org.offitec.osp.domain.enums.AssetType;
import org.offitec.osp.domain.enums.Mod;
import org.offitec.osp.domain.enums.UnitCategory;
import org.offitec.osp.domain.enums.UnitTypeEnum;
import org.offitec.osp.domain.exception.UnitDoesntExistException;
import org.offitec.osp.infrastructure.repository.SavedUnitRepository;
import org.offitec.osp.infrastructure.repository.UnitJpaRepository;
import org.offitec.osp.infrastructure.repository.UserRepository;
import org.offitec.osp.infrastructure.storage.S3Service;
import org.offitec.osp.presentation.dto.CalcAssetDTO;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PublicUnitAppService {

    private final UnitJpaRepository unitJpaRepository;
    private final SavedUnitRepository savedUnitRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    public PublicUnitAppService(UnitJpaRepository unitJpaRepository,
                                SavedUnitRepository savedUnitRepository,
                                UserRepository userRepository,
                                S3Service s3Service) {
        this.unitJpaRepository = unitJpaRepository;
        this.savedUnitRepository = savedUnitRepository;
        this.userRepository = userRepository;
        this.s3Service = s3Service;
    }

    // --- Listing ---

    @Transactional(readOnly = true)
    public List<UnitCardDTO> getUnitsByType(UnitCategory category, UnitTypeEnum unitType) {
        Long userId = currentUserId();
        return unitJpaRepository.findByCategoryAndUnitType(category, unitType).stream()
                .map(u -> toCard(u, userId))
                .collect(Collectors.toList());
    }

    // --- Detail ---

    @Transactional(readOnly = true)
    public UnitDetailPublicDTO getUnitDetail(Long id) {
        Unit unit = unitJpaRepository.findById(id)
                .orElseThrow(() -> new UnitDoesntExistException("Unit not found."));
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
        return savedUnitRepository.findByUserId(userId).stream()
                .map(SavedUnit::getUnit)
                .filter(u -> (category == null || u.getCategory() == category)
                        && (unitType == null || u.getUnitType() == unitType))
                .map(u -> toCard(u, userId))
                .collect(Collectors.toList());
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
                    case IMAGE -> images.add(new CalcAssetDTO(s3Service.presignImage(a.getUrl()), null));
                    case DRAWING -> drawings.add(new CalcAssetDTO(s3Service.presignTechnicalImage(a.getUrl()), null));
                    case DOCUMENT -> {
                        String presigned = s3Service.presignDocument(a.getUrl());
                        documents.add(new CalcAssetDTO(presigned, extractFileName(a.getUrl())));
                    }
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
                        dcv.getAmbient(), dcv.getCondensation(), dcv.getEvaporation(),
                        dcv.getSubcooling(), dcv.getSuperheat(),
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

    private UnitCardDTO toCard(Unit unit, Long userId) {
        String primaryImageUrl = null;
        List<UnitAsset> assets = unit.getAssets();
        if (assets != null) {
            for (UnitAsset a : assets) {
                if (a.getAssetType() == AssetType.IMAGE && a.isPrimary()) {
                    primaryImageUrl = s3Service.presignImage(a.getUrl());
                    break;
                }
            }
        }

        String capacityRange = buildCapacityRange(unit);
        String refrigerant = unit.getRefrigerant() != null ? unit.getRefrigerant().getCode() : null;
        boolean saved = userId != null && savedUnitRepository.existsByUserIdAndUnitId(userId, unit.getId());

        return new UnitCardDTO(
                unit.getId(),
                unit.getName(),
                unit.getModel(),
                primaryImageUrl,
                capacityRange,
                refrigerant,
                unit.getUnitType().name(),
                unit.getCategory().name(),
                saved
        );
    }

    private UnitDetailPublicDTO toDetail(Unit unit, Long userId) {
        String primaryImageUrl = null;
        List<String> iconUrls = new ArrayList<>();

        List<UnitAsset> assets = unit.getAssets();
        if (assets != null) {
            for (UnitAsset a : assets) {
                if (a.getAssetType() == AssetType.IMAGE && a.isPrimary()) {
                    primaryImageUrl = s3Service.presignImage(a.getUrl());
                } else if (a.getAssetType() == AssetType.ICON) {
                    iconUrls.add(s3Service.presignIcon(a.getUrl()));
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

    private String buildCapacityRange(Unit unit) {
        if (unit.getUnitDetails() == null || unit.getUnitDetails().isEmpty()) return null;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (UnitDetails d : unit.getUnitDetails()) {
            if (d.getTechSpecs() != null) {
                double cap = d.getTechSpecs().getCapacity();
                if (cap < min) min = cap;
                if (cap > max) max = cap;
            }
        }
        if (min == Double.MAX_VALUE) return null;
        if (min == max) return String.format("%.1f kW", min);
        return String.format("%.1f to %.1f kW", min, max);
    }

    private List<TechSpecItemDTO> buildSpecs(Unit unit) {
        List<TechSpecItemDTO> specs = new ArrayList<>();

        addSpec(specs, "Capacity", buildCapacityRange(unit));

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
        if (unit.getFanDiameter() > 0) addSpec(specs, "Fan Diameter", unit.getFanDiameter() + " mm");
        if (unit.getAirflowRate() > 0) addSpec(specs, "Airflow Rate", unit.getAirflowRate() + " m³/h");
        if (unit.getFanPI() > 0) addSpec(specs, "Fan Power Input", unit.getFanPI() + " kW");
        if (unit.getWidth() > 0 && unit.getHeight() > 0 && unit.getLength() > 0) {
            addSpec(specs, "Dimensions (W×H×L)", String.format("%.0f × %.0f × %.0f mm", unit.getWidth(), unit.getHeight(), unit.getLength()));
        }
        if (unit.getGasTank() > 0) addSpec(specs, "Gas Tank", unit.getGasTank() + " L");
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
