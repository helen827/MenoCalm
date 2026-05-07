package com.livemore.api.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JournalEntryDto {

    private String id;
    private String date;
    private String rawText;
    private double createdAt;
    private Integer revision;
    private ExtractedDataDto extracted = new ExtractedDataDto();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @JsonProperty("rawText")
    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    @JsonProperty("createdAt")
    public double getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(double createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getRevision() {
        return revision;
    }

    public void setRevision(Integer revision) {
        this.revision = revision;
    }

    public ExtractedDataDto getExtracted() {
        return extracted;
    }

    public void setExtracted(ExtractedDataDto extracted) {
        this.extracted = extracted;
    }
}
