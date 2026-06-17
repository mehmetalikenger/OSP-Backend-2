package org.offitec.osp.presentation.dto;

import java.util.List;

// id/model/type for the selector, plus which modes already exist on the heat pump.
public record HeatPumpSummaryDTO(Long id, String model, String type, List<String> mods) {
}
