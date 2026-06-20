package org.offitec.osp.presentation.dto;

import lombok.Getter;
import lombok.Setter;
import org.offitec.osp.domain.enums.AssetType;

import java.util.List;

// Sent after the browser has uploaded the files to R2. The backend verifies each
// object and records a UnitAsset row. `key` must be one the backend itself issued
// at presign time (it is re-validated against the unit's prefix).
@Getter
@Setter
public class AssetConfirmRequestDTO {

    private List<Item> files;

    @Getter
    @Setter
    public static class Item {
        private String key;
        private AssetType type;
        private boolean primary;
    }
}
