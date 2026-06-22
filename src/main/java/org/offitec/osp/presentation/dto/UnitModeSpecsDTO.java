package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

// Technical attributes specific to one operating mode (stored on that mode's TechSpecs).
@Getter
public class UnitModeSpecsDTO {

    public double capacity;
    public double maxCapacity;
    public double copErr;
    public double condenserRequiredDuty;
    public double quietCondenserRequiredDuty;

    @NotNull(message = "Compressor must be selected.")
    public Long compressorSpecsId;

    @NotNull(message = "Condenser must be selected.")
    public Long condenserSpecsId;

    @NotNull(message = "Evaporator must be selected.")
    public Long evaporatorSpecsId;

    @NotNull(message = "Expansion valve must be selected.")
    public Long expansionValveSpecsId;

    // Optional (heat pumps may use it; not required)
    public Long fourWayReversingValveSpecsId;
}
