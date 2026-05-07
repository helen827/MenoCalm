package com.livemore.api.service;

import com.livemore.api.config.AppProperties;
import com.livemore.api.domain.CommunityMediaDocument;
import com.livemore.api.web.dto.MediaPresignRequest;
import com.livemore.api.web.dto.MediaPresignResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CommunityMediaService {

    private static final DateTimeFormatter YEAR_FMT = DateTimeFormatter.ofPattern("yyyy").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM").withZone(ZoneOffset.UTC);

    private final CommunityMediaStore mediaStore;
    private final S3Presigner s3Presigner;
    private final AppProperties appProperties;
    private final CommunityMediaPublicUrlBuilder publicUrlBuilder;

    public CommunityMediaService(
            CommunityMediaStore mediaStore,
            S3Presigner s3Presigner,
            AppProperties appProperties,
            CommunityMediaPublicUrlBuilder publicUrlBuilder
    ) {
        this.mediaStore = mediaStore;
        this.s3Presigner = s3Presigner;
        this.appProperties = appProperties;
        this.publicUrlBuilder = publicUrlBuilder;
    }

    public MediaPresignResponse createPresign(String userId, MediaPresignRequest request) {
        AppProperties.Storage storage = appProperties.getStorage();
        ensureStorageConfigured(storage);
        String visibility = normalizeVisibility(request.getVisibility());

        Instant now = Instant.now();
        String objectKey = buildObjectKey(userId, request.getContentType(), request.getFileName(), now);
        String mediaId = UUID.randomUUID().toString();
        CommunityMediaDocument doc = new CommunityMediaDocument();
        doc.setId(mediaId);
        doc.setOwnerUserId(userId);
        doc.setBucket(storage.getS3Bucket());
        doc.setObjectKey(objectKey);
        doc.setMimeType(request.getContentType());
        doc.setSizeBytes(request.getSizeBytes());
        doc.setSha256(request.getSha256());
        doc.setVisibility(visibility);
        doc.setStatus("PENDING_UPLOAD");
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);
        mediaStore.upsertMedia(doc);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(storage.getS3Bucket())
                .key(objectKey)
                .contentType(request.getContentType())
                .build();
        Duration ttl = Duration.ofSeconds(storage.getPresignTtlSeconds());
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(
                PutObjectPresignRequest.builder().putObjectRequest(putObjectRequest).signatureDuration(ttl).build()
        );

        MediaPresignResponse response = new MediaPresignResponse();
        response.setMediaId(mediaId);
        response.setBucket(storage.getS3Bucket());
        response.setObjectKey(objectKey);
        response.setUploadMethod("PUT");
        response.setUploadUrl(presigned.url().toString());
        response.setExpiresAtEpochSeconds(now.plusSeconds(storage.getPresignTtlSeconds()).getEpochSecond());
        response.setPublicUrl(publicUrlBuilder.buildPublicUrl(visibility, "PENDING_UPLOAD", objectKey));
        response.setRequiredHeaders(Map.of("Content-Type", request.getContentType()));
        return response;
    }

    public void markUploadComplete(String userId, String mediaId) {
        if (mediaId == null || mediaId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "media_id_required");
        }
        int updated = mediaStore.markMediaReadyIfPending(mediaId.trim(), userId);
        if (updated < 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "media_not_found_or_not_pending");
        }
    }

    private void ensureStorageConfigured(AppProperties.Storage storage) {
        if (isBlank(storage.getS3Bucket())
                || isBlank(storage.getS3AccessKey())
                || isBlank(storage.getS3SecretKey())
                || isBlank(storage.getS3Region())
                || isPlaceholder(storage.getS3Bucket())
                || isPlaceholder(storage.getS3AccessKey())
                || isPlaceholder(storage.getS3SecretKey())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "storage_not_configured");
        }
    }

    private String normalizeVisibility(String visibility) {
        String normalized = visibility == null ? "public" : visibility.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("public") && !normalized.equals("private")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_visibility");
        }
        return normalized;
    }

    private String buildObjectKey(String userId, String contentType, String fileName, Instant now) {
        String folder = contentType != null && contentType.startsWith("video/") ? "videos"
                : (contentType != null && contentType.startsWith("image/") ? "images" : "files");
        String extension = "";
        if (fileName != null) {
            int idx = fileName.lastIndexOf('.');
            if (idx > -1 && idx < fileName.length() - 1) {
                String ext = fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
                if (ext.matches("[a-z0-9]{1,10}")) {
                    extension = "." + ext;
                }
            }
        }
        return "community/" + folder + "/" + YEAR_FMT.format(now) + "/" + MONTH_FMT.format(now)
                + "/" + userId + "/" + UUID.randomUUID() + extension;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isPlaceholder(String value) {
        if (value == null) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("replace-with-real-") || normalized.startsWith("replace-");
    }
}
