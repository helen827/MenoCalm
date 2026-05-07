package com.livemore.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.livemore.api.config.AppProperties;
import com.livemore.api.web.dto.SymptomLogPayloadDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM-based silent extraction (Component B) — strict JSON matching {@link SymptomLogPayloadDto} field names.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "qiniu")
public class QiniuSymptomExtractionClient {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final QiniuChatCompletionClient chatCompletionClient;

    public QiniuSymptomExtractionClient(
            AppProperties appProperties,
            ObjectMapper objectMapper,
            QiniuChatCompletionClient chatCompletionClient
    ) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.chatCompletionClient = chatCompletionClient;
    }

    public SymptomLogPayloadDto extract(String userMessage) throws Exception {
        AppProperties.Ai ai = appProperties.getAi();
        String model = ai.getModel() == null || ai.getModel().isBlank() ? "" : ai.getModel().trim();
        ObjectNode system = objectMapper.createObjectNode();
        system.put("role", "system");
        system.put("content", """
                Task: Extract structured health entities from the user message.
                Output: Strict JSON only, no markdown fences, no extra text.
                Schema keys (exact):
                symptom_detected (string array),
                severity (string: a number 1-10 as string, or null as JSON null),
                lifestyle_factors (string array),
                emotional_status (string),
                is_relevant_to_menopause (boolean).
                Use empty arrays where nothing applies. severity must be null unless user clearly rates intensity.
                """);

        ObjectNode user = objectMapper.createObjectNode();
        user.put("role", "user");
        user.put("content", "User message:\n" + (userMessage == null ? "" : userMessage));

        List<JsonNode> messages = new ArrayList<>();
        messages.add(system);
        messages.add(user);

        String raw = chatCompletionClient.complete(messages, ai.getExtractionTemperature(), model);
        String json = stripCodeFence(raw.trim());
        JsonNode node = objectMapper.readTree(json);
        return objectMapper.treeToValue(node, SymptomLogPayloadDto.class);
    }

    private static String stripCodeFence(String raw) {
        if (raw.startsWith("```")) {
            int firstNewLine = raw.indexOf('\n');
            int lastFence = raw.lastIndexOf("```");
            if (firstNewLine > 0 && lastFence > firstNewLine) {
                return raw.substring(firstNewLine + 1, lastFence).trim();
            }
        }
        return raw;
    }
}
