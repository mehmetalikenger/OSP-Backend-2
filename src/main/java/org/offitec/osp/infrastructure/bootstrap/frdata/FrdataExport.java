package org.offitec.osp.infrastructure.bootstrap.frdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Jackson DTOs mirroring the structure produced by tools/extract_frdata.ps1 (frdata-export.json).
 * Only the fields the importer consumes are declared; everything else is ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FrdataExport(
        int compressorCount,
        int ratingCount,
        List<Comp> compressors
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Comp(
            Integer key,                 // FSS3 tblSRClist primary key (unique per variant)
            String name,
            Integer mddKey,
            String modelDesc,
            String type,                 // "SK" or "TK"
            Boolean isSubcritical,
            Double tDisMax,
            String defaultRefrigerant,
            Double displacement,
            Integer pistonCount,
            Double nominalHp,
            Double oilA, Double oilB, Double oilC,
            List<Rating> ratings
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Rating(
            String refrigerant,
            String refrigerantLib,
            boolean useMassCap,
            boolean eco,
            double[] capCoeffs,
            double[] powerCoeffs,
            double[] massCoeffs,
            RefCond referenceCondition,
            CapCtrl capControl,
            Envelope envelope
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RefCond(double ohRef, double taspRef, double scRef, double tliqRef) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CapCtrl(
            Double minFrequency, Double maxFrequency,
            Double minSpeed, Double maxSpeed,
            CoefSet frequencyCoef
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CoefSet(double[] cap, double[] pow, double[] curr) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Envelope(Integer key, Integer ecoKey, List<Pt> points) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pt(double x, double y) {}
}
