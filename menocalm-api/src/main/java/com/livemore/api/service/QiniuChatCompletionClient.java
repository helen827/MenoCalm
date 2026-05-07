package com.livemore.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.livemore.api.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * OpenAI-compatible chat.completions call (Qiniu and similar providers).
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "qiniu")
public class QiniuChatCompletionClient {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public QiniuChatCompletionClient(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        AppProperties.Ai ai = appProperties.getAi();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(ai.getConnectTimeoutMs()))
                .build();
    }

    public String complete(List<JsonNode> messages, double temperature, String model) throws IOException, InterruptedException {
        return complete(messages, temperature, model, null);
    }

    /**
     * @param maxCompletionTokens optional; when set, sent as {@code max_tokens} so providers do not truncate too aggressively.
     */
    public String complete(List<JsonNode> messages, double temperature, String model, Integer maxCompletionTokens)
            throws IOException, InterruptedException {
        AppProperties.Ai ai = appProperties.getAi();
        if (isBlank(ai.getEndpoint()) || isBlank(ai.getApiKey()) || isBlank(model)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ai_provider_not_configured");
        }
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put("model", model);
        reqBody.put("temperature", temperature);
        if (maxCompletionTokens != null && maxCompletionTokens > 0) {
            reqBody.put("max_tokens", maxCompletionTokens);
        }
        ArrayNode arr = reqBody.putArray("messages");
        for (JsonNode m : messages) {
            arr.add(m);
        }
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(ai.getEndpoint()))
                .timeout(Duration.ofMillis(ai.getReadTimeoutMs()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + ai.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(reqBody.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ai_provider_failed_http_" + resp.statusCode());
        }
        return extractAssistantContent(resp.body());
    }

    private String extractAssistantContent(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ai_response_invalid");
        }
        JsonNode contentNode = choices.get(0).path("message").path("content");
        if (!contentNode.isTextual()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ai_response_invalid");
        }
        String content = contentNode.asText();
        if (content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ai_response_invalid");
        }
        return content;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
