package com.vcsm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "voice.model.s3.bucket")
public class VoiceModelStorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String keyPrefix;

    public VoiceModelStorageService(
            S3Client s3Client,
            @Value("${voice.model.s3.bucket:}") String bucket,
            @Value("${voice.model.s3.prefix:voice-models}") String keyPrefix
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.keyPrefix = keyPrefix;
    }

    public StoredVoiceModel store(MultipartFile file) throws IOException {
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("voice.model.s3.bucket must be configured before uploading voice models.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null
                ? "voice-model.zip"
                : file.getOriginalFilename());
        String modelKey = buildModelKey(originalFilename);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(modelKey)
                .contentType("application/zip")
                .metadata(java.util.Map.of(
                        "original-filename", originalFilename,
                        "uploaded-at", Instant.now().toString()
                ))
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return new StoredVoiceModel(bucket, modelKey);
    }

    private String buildModelKey(String originalFilename) {
        String normalizedPrefix = keyPrefix.replaceAll("^/+", "").replaceAll("/+$", "");
        String uniqueName = UUID.randomUUID() + "-" + originalFilename;
        return normalizedPrefix.isBlank() ? uniqueName : normalizedPrefix + "/" + uniqueName;
    }

    public record StoredVoiceModel(String bucket, String modelKey) {
    }
}
