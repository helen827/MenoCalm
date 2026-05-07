package com.livemore.api.service;

import com.livemore.api.config.AppProperties;
import com.livemore.api.domain.CommunityMediaDocument;
import com.livemore.api.web.dto.MediaPresignRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityMediaServiceTest {

    @Mock
    private CommunityMediaStore mediaStore;
    @Mock
    private S3Presigner s3Presigner;
    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;

    private AppProperties appProperties;
    private CommunityMediaService service;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getStorage().setS3Bucket("live-more-assets");
        appProperties.getStorage().setS3AccessKey("ak");
        appProperties.getStorage().setS3SecretKey("sk");
        appProperties.getStorage().setS3Region("us-east-1");
        appProperties.getStorage().setPresignTtlSeconds(600);
        appProperties.getStorage().setPublicBaseUrl("https://cdn.example.com");
        service = new CommunityMediaService(
                mediaStore,
                s3Presigner,
                appProperties,
                new CommunityMediaPublicUrlBuilder(appProperties)
        );
    }

    @Test
    void createPresign_persistsMetadataAndReturnsUrl() throws MalformedURLException {
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedPutObjectRequest);
        when(presignedPutObjectRequest.url()).thenReturn(new URL("https://upload.example.com/abc"));

        MediaPresignRequest request = new MediaPresignRequest();
        request.setFileName("a.png");
        request.setContentType("image/png");
        request.setSizeBytes(1024);
        request.setVisibility("public");

        var resp = service.createPresign("phone_13800138000", request);
        assertEquals("live-more-assets", resp.getBucket());
        assertEquals("PUT", resp.getUploadMethod());
        assertEquals("https://upload.example.com/abc", resp.getUploadUrl());
        assertNotNull(resp.getPublicUrl());
        assertEquals(true, resp.getPublicUrl().startsWith("https://cdn.example.com/"));
        verify(mediaStore).upsertMedia(any(CommunityMediaDocument.class));
    }

    @Test
    void createPresign_invalidVisibility_returns400() {
        MediaPresignRequest request = new MediaPresignRequest();
        request.setFileName("a.png");
        request.setContentType("image/png");
        request.setSizeBytes(1024);
        request.setVisibility("team-only");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createPresign("u1", request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void createPresign_storesPendingStatus() throws MalformedURLException {
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedPutObjectRequest);
        when(presignedPutObjectRequest.url()).thenReturn(new URL("https://upload.example.com/abc"));
        MediaPresignRequest request = new MediaPresignRequest();
        request.setFileName("a.mp4");
        request.setContentType("video/mp4");
        request.setSizeBytes(2048);

        service.createPresign("u2", request);
        ArgumentCaptor<CommunityMediaDocument> captor = ArgumentCaptor.forClass(CommunityMediaDocument.class);
        verify(mediaStore).upsertMedia(captor.capture());
        assertEquals("PENDING_UPLOAD", captor.getValue().getStatus());
    }

    @Test
    void markUploadComplete_success_updatesStore() {
        when(mediaStore.markMediaReadyIfPending("mid1", "u1")).thenReturn(1);
        service.markUploadComplete("u1", "mid1");
        verify(mediaStore).markMediaReadyIfPending("mid1", "u1");
    }

    @Test
    void markUploadComplete_notFound_returns404() {
        when(mediaStore.markMediaReadyIfPending("mid1", "u1")).thenReturn(0);
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.markUploadComplete("u1", "mid1")
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
