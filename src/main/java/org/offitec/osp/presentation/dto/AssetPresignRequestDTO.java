package org.offitec.osp.presentation.dto;

import lombok.Getter;
import lombok.Setter;
import org.offitec.osp.domain.enums.AssetType;

import java.util.List;

// Client's request for presigned upload URLs. One Item per file the browser wants
// to upload directly to R2. The backend validates each before issuing a URL.
@Getter
@Setter
public class AssetPresignRequestDTO {

    private List<Item> files;

    @Getter
    @Setter
    public static class Item {
        private String clientId;     // opaque id echoed back so the client can match URLs to files
        private String filename;
        private String contentType;
        private long size;
        private AssetType type;
    }
}
