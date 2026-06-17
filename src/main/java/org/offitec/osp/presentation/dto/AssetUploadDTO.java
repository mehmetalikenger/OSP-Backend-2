package org.offitec.osp.presentation.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class AssetUploadDTO {
    private MultipartFile primaryImage;
    private List<MultipartFile> images;
    private List<MultipartFile> technicalImages;
    private List<MultipartFile> icons;
    private List<MultipartFile> documents;
}
