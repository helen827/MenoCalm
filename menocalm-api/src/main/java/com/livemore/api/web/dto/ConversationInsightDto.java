package com.livemore.api.web.dto;

import java.util.List;

public class ConversationInsightDto {
    private String conversationId;
    private String summary;
    private String riskLevel;
    private List<String> symptomTags;
    private List<String> suggestions;
    private Long updatedAtMs;
    private String source;
    private String degradedReason;

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<String> getSymptomTags() {
        return symptomTags;
    }

    public void setSymptomTags(List<String> symptomTags) {
        this.symptomTags = symptomTags;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public Long getUpdatedAtMs() {
        return updatedAtMs;
    }

    public void setUpdatedAtMs(Long updatedAtMs) {
        this.updatedAtMs = updatedAtMs;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDegradedReason() {
        return degradedReason;
    }

    public void setDegradedReason(String degradedReason) {
        this.degradedReason = degradedReason;
    }
}
