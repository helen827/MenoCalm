package com.livemore.api.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches the analytics schema for symptoms_log.payload_json (Component B style).
 * severity: numeric string 1–10 or null when unknown.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SymptomLogPayloadDto {

    @JsonProperty("symptom_detected")
    private List<String> symptomDetected = new ArrayList<>();

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("lifestyle_factors")
    private List<String> lifestyleFactors = new ArrayList<>();

    @JsonProperty("emotional_status")
    private String emotionalStatus;

    @JsonProperty("is_relevant_to_menopause")
    private boolean relevantToMenopause;

    public List<String> getSymptomDetected() {
        return symptomDetected;
    }

    public void setSymptomDetected(List<String> symptomDetected) {
        this.symptomDetected = symptomDetected;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public List<String> getLifestyleFactors() {
        return lifestyleFactors;
    }

    public void setLifestyleFactors(List<String> lifestyleFactors) {
        this.lifestyleFactors = lifestyleFactors;
    }

    public String getEmotionalStatus() {
        return emotionalStatus;
    }

    public void setEmotionalStatus(String emotionalStatus) {
        this.emotionalStatus = emotionalStatus;
    }

    public boolean isRelevantToMenopause() {
        return relevantToMenopause;
    }

    public void setRelevantToMenopause(boolean relevantToMenopause) {
        this.relevantToMenopause = relevantToMenopause;
    }
}
