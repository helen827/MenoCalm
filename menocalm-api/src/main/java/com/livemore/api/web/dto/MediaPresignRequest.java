package com.livemore.api.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class MediaPresignRequest {

    @NotBlank
    private String fileName;
    @NotBlank
    private String contentType;
    @Min(1)
    private long sizeBytes;
    private String sha256;
    private String visibility = "public";

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }
}
