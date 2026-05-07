package com.livemore.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.livemore.api.config.AppProperties;
import com.livemore.api.web.dto.WeeklyReportMarkdownDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Component C: synthesize last 7 days of symptoms_log into Markdown (temperature from app.ai.reportTemperature).
 */
@Service
public class MenopauseWeeklyReportService {

    private static final String REPORT_SYSTEM = """
            Task: Synthesize roughly 7 days of structured health logs into a user-friendly weekly report.
            Format: Markdown with light emoji (e.g. small section headers). Avoid clinical jargon.
            Structure:
            1) Summary — one sentence overview of the week.
            2) Symptom trend — most frequent symptoms or themes.
            3) Correlation insight — link lifestyle_factors to symptoms when evidence appears in the logs.
            4) Encouragement — one actionable goal for next week.
            If logs are sparse, still write a supportive short report and suggest consistent tracking.
            """;

    private final MySqlSymptomLogStore symptomLogStore;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final QiniuChatCompletionClient qiniuChatCompletionClient;

    public MenopauseWeeklyReportService(
            MySqlSymptomLogStore symptomLogStore,
            ObjectMapper objectMapper,
            AppProperties appProperties,
            @Autowired(required = false) QiniuChatCompletionClient qiniuChatCompletionClient
    ) {
        this.symptomLogStore = symptomLogStore;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.qiniuChatCompletionClient = qiniuChatCompletionClient;
    }

    public WeeklyReportMarkdownDto buildWeeklyMarkdown(String authenticatedUserId, String queryUserId) {
        if (qiniuChatCompletionClient == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ai_provider_not_configured");
        }
        assertSameUser(authenticatedUserId, queryUserId);
        long since = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        List<MySqlSymptomLogStore.SymptomLogEntryRow> rows = symptomLogStore.listSince(queryUserId, since);

        ArrayNode arr = objectMapper.createArrayNode();
        for (MySqlSymptomLogStore.SymptomLogEntryRow row : rows) {
            ObjectNode o = objectMapper.createObjectNode();
            o.put("created_at_ms", row.createdAtMs());
            o.put("source", row.source() == null ? "" : row.source());
            try {
                o.set("payload", objectMapper.readTree(row.payloadJson()));
            } catch (Exception ex) {
                o.put("payload_raw", row.payloadJson());
            }
            arr.add(o);
        }

        ObjectNode system = objectMapper.createObjectNode();
        system.put("role", "system");
        system.put("content", REPORT_SYSTEM);

        ObjectNode user = objectMapper.createObjectNode();
        user.put("role", "user");
        String logsBody;
        try {
            logsBody = objectMapper.writeValueAsString(arr);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "report_payload_encode_failed", ex);
        }
        user.put("content", "Last_7_Days_JSON_Logs:\n" + logsBody);

        List<JsonNode> messages = new ArrayList<>();
        messages.add(system);
        messages.add(user);

        AppProperties.Ai ai = appProperties.getAi();
        String model = ai.getModel() == null || ai.getModel().isBlank() ? "" : ai.getModel().trim();
        String markdown;
        try {
            markdown = qiniuChatCompletionClient.complete(messages, ai.getReportTemperature(), model, 4096);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ai_report_failed", ex);
        }
        WeeklyReportMarkdownDto dto = new WeeklyReportMarkdownDto();
        dto.setMarkdown(markdown);
        dto.setSource("qiniu-weekly");
        return dto;
    }

    private void assertSameUser(String authenticatedUserId, String queryUserId) {
        if (queryUserId == null || queryUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId_required");
        }
        if (!authenticatedUserId.equals(queryUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "user_mismatch");
        }
    }
}
