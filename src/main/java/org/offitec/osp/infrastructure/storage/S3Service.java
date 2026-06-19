package org.offitec.osp.infrastructure.storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class S3Service {

    private static final String PREFIX_IMAGES    = "unit-images";
    private static final String PREFIX_TECHNICAL = "technical-images";
    private static final String PREFIX_ICONS     = "icons";
    private static final String PREFIX_DOCUMENTS = "documents";
    private static final String PREFIX_USERS     = "user-pictures";

    @Value("${spring.storage.r2.bucket-name}")
    private String bucket;

    @Value("${spring.storage.r2.public-base-url}")
    private String publicBaseUrl;

    private final AmazonS3 storage;

    public S3Service(AmazonS3 storage) {
        this.storage = storage;
    }

    public String uploadImage(String key, MultipartFile file) {
        return uploadOptimized(PREFIX_IMAGES + "/" + key, file);
    }

    public String uploadTechnicalImage(String key, MultipartFile file) {
        return uploadOptimized(PREFIX_TECHNICAL + "/" + key, file);
    }

    public String uploadIcon(String key, MultipartFile file) {
        return uploadOptimized(PREFIX_ICONS + "/" + key, file);
    }

    public String uploadDocument(String key, MultipartFile file) {
        return upload(PREFIX_DOCUMENTS + "/" + key, file);
    }

    public String uploadUserPicture(String key, MultipartFile file) {
        return upload(PREFIX_USERS + "/" + key, file);
    }

    public void deleteImage(String url)          { delete(url); }
    public void deleteTechnicalImage(String url) { delete(url); }
    public void deleteIcon(String url)           { delete(url); }
    public void deleteDocument(String url)       { delete(url); }
    public void deleteUserPicture(String url)    { delete(url); }

    public String presignImage(String url)          { return presign(url); }
    public String presignTechnicalImage(String url) { return presign(url); }
    public String presignIcon(String url)           { return presign(url); }
    public String presignDocument(String url)       { return presign(url); }
    public String presignUserPicture(String url)    { return presign(url); }

    private String presign(String url) {
        try {
            String key = new java.net.URI(url).getPath().substring(1);
            java.util.Date expiration = new java.util.Date(System.currentTimeMillis() + 3_600_000L);
            return storage.generatePresignedUrl(bucket, key, expiration).toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate pre-signed URL: " + url, e);
        }
    }

    private void delete(String url) {
        try {
            String key = new java.net.URI(url).getPath().substring(1);
            storage.deleteObject(bucket, key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete object: " + url, e);
        }
    }

    private String upload(String key, MultipartFile file) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            metadata.setCacheControl("public, max-age=31536000");

            storage.putObject(new PutObjectRequest(bucket, key, file.getInputStream(), metadata));
            return publicBaseUrl + "/" + key;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file: " + key, e);
        }
    }

    // ---- Image optimization (resize + recompress before storing) ----

    private static final int MAX_DIMENSION = 1920;
    private static final double JPEG_QUALITY = 0.82;

    // Optimizes raster images (JPEG/PNG) before upload; everything else (SVG, GIF,
    // unreadable files) and any failure falls back to the untouched original.
    private String uploadOptimized(String key, MultipartFile file) {
        byte[] optimized;
        String contentType;
        try {
            optimized = optimizeImage(file);
            contentType = file.getContentType();
        } catch (Exception e) {
            optimized = null;
            contentType = null;
        }

        if (optimized == null) {
            return upload(key, file);
        }

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(optimized.length);
        metadata.setCacheControl("public, max-age=31536000");
        storage.putObject(new PutObjectRequest(bucket, key, new ByteArrayInputStream(optimized), metadata));
        return publicBaseUrl + "/" + key;
    }

    private static final long OPTIMIZE_THRESHOLD_BYTES = 300 * 1024; // skip optimization under 300 KB

    // Returns optimized bytes, or null when the file should be stored as-is
    // (non-raster type, undecodable, already small, or when optimizing wouldn't save space).
    private byte[] optimizeImage(MultipartFile file) throws IOException {
        if (file.getSize() < OPTIMIZE_THRESHOLD_BYTES) return null;

        String contentType = file.getContentType();
        String format;
        if ("image/jpeg".equalsIgnoreCase(contentType)) {
            format = "jpg";
        } else if ("image/png".equalsIgnoreCase(contentType)) {
            format = "png";
        } else {
            return null;
        }

        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.Builder<BufferedImage> builder = Thumbnails.of(image);
        if (image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION) {
            builder.size(MAX_DIMENSION, MAX_DIMENSION);
        } else {
            builder.scale(1.0);
        }
        builder.outputFormat(format);
        if ("jpg".equals(format)) {
            builder.outputQuality(JPEG_QUALITY);
        }
        builder.toOutputStream(out);

        byte[] bytes = out.toByteArray();
        return bytes.length < file.getSize() ? bytes : null;
    }
}
