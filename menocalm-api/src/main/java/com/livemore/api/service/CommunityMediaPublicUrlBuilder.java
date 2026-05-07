package com.livemore.api.service;

import com.livemore.api.config.AppProperties;
import com.livemore.api.domain.CommunityMediaDocument;
import org.springframework.stereotype.Component;

@Component
public class CommunityMediaPublicUrlBuilder {

    private final AppProperties appProperties;

    public CommunityMediaPublicUrlBuilder(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * Exposes a public HTTPS URL only for public media in an upload-complete or pending-upload state.
     */
    public String publicUrlFor(CommunityMediaDocument media) {
        if (media == null) {
            return null;
        }
        return buildPublicUrl(media.getVisibility(), media.getStatus(), media.getObjectKey());
    }

    public String buildPublicUrl(String visibility, String status, String objectKey) {
        if (!"public".equalsIgnoreCase(safeTrim(visibility))) {
            return null;
        }
        if (!isUrlAllowedStatus(status)) {
            return null;
        }
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        AppProperties.Storage storage = appProperties.getStorage();
        String base = storage.getPublicBaseUrl();
        if (isBlank(base)) {
            return null;
        }
        String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalized + "/" + objectKey;
    }

    private static boolean isUrlAllowedStatus(String status) {
        String s = safeTrim(status);
        if (s.isEmpty()) {
            return false;
        }
        if ("FAILED".equalsIgnoreCase(s) || "DELETED".equalsIgnoreCase(s)) {
            return false;
        }
        return "READY".equalsIgnoreCase(s) || "PENDING_UPLOAD".equalsIgnoreCase(s);
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
