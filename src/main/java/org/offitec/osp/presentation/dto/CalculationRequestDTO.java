package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalculationRequestDTO {

    @NotNull(message = "Unit ID can't be null.")
    private Long unitId;

    @NotNull(message = "Mod can't be null.")
    private String mod;

    private double ambient;
    private double evapIn;
    private double evapOut;
    private double condIn;
    private double condOut;

    // --- Faithful-engine inputs (used when the unit has a CompressorRating). All optional with
    // sensible defaults so existing callers that don't send them keep working. ---

    // Compressor frequency in Hz: 50 or 60 for fixed speed, or an inverter frequency. Default 50.
    private Double frequencyHz;

    // Liquid subcooling below the condenser bubble point (K). Default 0.
    private Double subcooling;

    // Suction superheat above the evaporator dew point (K). Default 10. Ignored when
    // suctionGasTemp is provided.
    private Double superheat;

    // Absolute suction-gas temperature (°C). When set, overrides superheat.
    private Double suctionGasTemp;

    // Compressor speed (rpm). Used by the Copeland trivariate polynomial path; ignored by Frascold.
    // Defaults to the rating's max speed (or 3000) when not supplied.
    private Double rpm;

    // Optional glycol mixture correction. type e.g. "Ethylene Glycol" / "Propylene Glycol",
    // percentage 5..50. When set, capacity/power are scaled by the correction factors.
    private String glycolType;
    private Integer glycolPercentage;
}
