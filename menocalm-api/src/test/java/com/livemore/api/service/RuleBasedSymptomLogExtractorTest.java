package com.livemore.api.service;

import com.livemore.api.web.dto.SymptomLogPayloadDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleBasedSymptomLogExtractorTest {

    private final RuleBasedSymptomLogExtractor extractor = new RuleBasedSymptomLogExtractor();

    @Test
    void extractsSymptomsLifestyleAndSeverity() {
        SymptomLogPayloadDto dto = extractor.extract("最近潮热厉害，晚上喝咖啡后大概 7/10 分难受，有点焦虑");
        assertTrue(dto.getSymptomDetected().contains("潮热"));
        assertTrue(dto.getLifestyleFactors().contains("咖啡因"));
        assertEquals("7", dto.getSeverity());
        assertTrue(dto.isRelevantToMenopause());
    }
}
