package com.livemore.api.domain;

import com.livemore.api.web.dto.ExtractedDataDto;
import org.springframework.data.annotation.Id;

import java.time.Instant;

public class JournalEntryDocument {

    @Id
    private String id;

    private String userId;
    private String entryId;
    private String date;
    private String rawText;
    private double createdAt;
    private Integer revision;
    private ExtractedDataDto extracted;
    private Instant updatedAtServer;

    public JournalEntryDocument() {
    }

    public static String composePrimaryId(String userId, String entryId) {
        return userId + ":" + entryId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

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

    public Instant getUpdatedAtServer() {
        return updatedAtServer;
    }

    public void setUpdatedAtServer(Instant updatedAtServer) {
        this.updatedAtServer = updatedAtServer;
    }
}
