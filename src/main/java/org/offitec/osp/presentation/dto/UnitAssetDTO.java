package org.offitec.osp.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UnitAssetDTO {
    private Long id;
    private String url;
    private String assetType;
    private boolean isPrimary;
}
