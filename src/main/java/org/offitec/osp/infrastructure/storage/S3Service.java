package org.offitec.osp.infrastructure.storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class S3Service {

    @Value("${spring.storage.s3.unit-images-bucket-name}")
    private String unitImagesBucket;

    @Value("${spring.storage.s3.unit-technicalImages-bucket-name}")
    private String unitTechnicalImagesBucket;

    @Value("${spring.storage.s3.unit-icons-bucket-name}")
    private String unitIconsBucket;

    @Value("${spring.storage.s3.unit-documents-bucket-name}")
    private String unitDocumentsBucket;

    @Value("${spring.storage.s3.user-pictures-bucket-name}")
    private String userPicturesBucket;

    private final AmazonS3 amazonS3;

    public S3Service(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    public String uploadImage(String key, MultipartFile file) {
        return upload(unitImagesBucket, key, file);
    }

    public String uploadTechnicalImage(String key, MultipartFile file) {
        return upload(unitTechnicalImagesBucket, key, file);
    }

    public String uploadIcon(String key, MultipartFile file) {
        return upload(unitIconsBucket, key, file);
    }

    public String uploadDocument(String key, MultipartFile file) {
        return upload(unitDocumentsBucket, key, file);
    }

    public String uploadUserPicture(String key, MultipartFile file) { return upload(userPicturesBucket, key, file); }
    public void deleteUserPicture(String url)    { delete(userPicturesBucket, url); }
    public String presignUserPicture(String url) { return presign(userPicturesBucket, url); }

    public void deleteImage(String url)          { delete(unitImagesBucket, url); }
    public void deleteTechnicalImage(String url) { delete(unitTechnicalImagesBucket, url); }
    public void deleteIcon(String url)           { delete(unitIconsBucket, url); }
    public void deleteDocument(String url)       { delete(unitDocumentsBucket, url); }

    public String presignImage(String url)          { return presign(unitImagesBucket, url); }
    public String presignTechnicalImage(String url) { return presign(unitTechnicalImagesBucket, url); }
    public String presignIcon(String url)           { return presign(unitIconsBucket, url); }
    public String presignDocument(String url)       { return presign(unitDocumentsBucket, url); }

    private String presign(String bucket, String url) {
        try {
            String key = new java.net.URI(url).getPath().substring(1);
            java.util.Date expiration = new java.util.Date(System.currentTimeMillis() + 3_600_000L); // 1 h
            return amazonS3.generatePresignedUrl(bucket, key, expiration).toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate pre-signed URL: " + url, e);
        }
    }

    private void delete(String bucket, String url) {
        try {
            String key = new java.net.URI(url).getPath().substring(1);
            amazonS3.deleteObject(bucket, key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete S3 object: " + url, e);
        }
    }

    // Streams the file straight to S3 (no temp file on disk) and returns the stored object URL.
    private String upload(String bucket, String key, MultipartFile file) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            amazonS3.putObject(new PutObjectRequest(bucket, key, file.getInputStream(), metadata));

            return amazonS3.getUrl(bucket, key).toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3: " + key, e);
        }
    }
}
