package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

// Technical attributes specific to one operating mode (stored on that mode's TechSpecs).
@Getter
public class UnitModeSpecsDTO {

    public double copErr;
    public double condenserRequiredDuty;
    public double quietCondenserRequiredDuty;

    // Compressor rating id (compressor + refrigerant coefficient set). For heat pumps the rating is
    // a unit-level selection (shared by both modes), so this is typically unused here.
    public Long compressorRatingId;

    @NotNull(message = "Condenser must be selected.")
    public Long condenserSpecsId;

    @NotNull(message = "Evaporator must be selected.")
    public Long evaporatorSpecsId;

    @NotNull(message = "Expansion valve must be selected.")
    public Long expansionValveSpecsId;

    // Optional (heat pumps may use it; not required)
    public Long fourWayReversingValveSpecsId;
}
