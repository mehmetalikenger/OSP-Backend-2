package org.offitec.osp.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class ChillerDTO {

    @NotBlank(message = "Unit model can't be blank.")
    public String model;

    public String name;
    public String description;

    @NotBlank(message = "Unit type can't be blank.")
    public String type;

    @NotBlank(message = "Mod can't be blank.")
    public String mod;

    public MultipartFile primaryImage;
    public List<MultipartFile> images;
    public List<MultipartFile> technicalImages;
    public List<MultipartFile> icons;
    public List<MultipartFile> documents;
}
