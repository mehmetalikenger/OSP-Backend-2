package org.offitec.osp.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// One ticket per requested file: where to PUT the bytes and the storage key to send
// back at confirm time.
@Getter
@Setter
public class AssetPresignResponseDTO {

    private List<Item> files;

    public AssetPresignResponseDTO(List<Item> files) {
        this.files = files;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String clientId;
        private String uploadUrl;
        private String key;
    }
}
