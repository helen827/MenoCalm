package com.livemore.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.livemore.api.config.AppProperties;
import com.livemore.api.web.dto.ConversationInsightDto;
import com.livemore.api.web.dto.ConversationMessageDto;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "qiniu")
public class QiniuConversationAnalysisClient implements ConversationAnalysisClient {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public QiniuConversationAnalysisClient(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        AppProperties.Ai ai = appProperties.getAi();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(ai.getConnectTimeoutMs()))
                .build();
    }

    @Override
    public ConversationInsightDto analyze(List<ConversationMessageDto> messages) {
        AppProperties.Ai ai = appProperties.getAi();
        validateConfig(ai);

        List<String> models = buildModelCandidates(ai);
        ResponseStatusException lastStatus = null;
        for (String model : models) {
            try {
                String body = callOnceWithRetry(ai, model, messages);
                String content = extractAssistantContent(body);
                ConversationInsightDto dto = parseInsightContent(content);
                dto.setSource("qiniu");
                return dto;
            } catch (ResponseStatusException ex) {
                lastStatus = ex;
                if (ex.getStatusCode().value() == 503 || ex.getStatusCode().value() == 429) {
                    continue;
                }
                throw ex;
            } catch (IOException ex) {
                lastStatus = new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "ai_response_invalid" + (ex.getMessage() == null || ex.getMessage().isBlank() ? "" : ":" + ex.getMessage())
                );
                continue;
            }
        }
        if (lastStatus != null) {
            throw lastStatus;
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ai_provider_failed");
    }

    private String callOnceWithRetry(AppProperties.Ai ai, String model, List<ConversationMessageDto> messages) {
        int maxAttempts = 3;
        String lastDetail = null;
        int lastStatus = 0;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ProviderResponse resp = callProvider(ai, model, messages);
                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    return resp.body();
                }
                lastStatus = resp.statusCode();
                lastDetail = summarizeProviderError(resp.body());
                if (shouldRetryStatus(resp.statusCode()) && attempt < maxAttempts) {
                    sleepBackoff(attempt);
                    continue;
                }
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "ai_provider_failed_http_" + resp.statusCode() + (lastDetail.isEmpty() ? "" : ":" + lastDetail)
                );
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ai_provider_failed_interrupted");
            } catch (IOException ex) {
                lastDetail = ex.getMessage();
                if (attempt < maxAttempts) {
                    sleepBackoff(attempt);
                    continue;
                }
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "ai_provider_failed_io" + (lastDetail == null || lastDetail.isBlank() ? "" : ":" + lastDetail)
                );
            }
        }
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "ai_provider_failed_http_" + lastStatus + (lastDetail == null || lastDetail.isBlank() ? "" : ":" + lastDetail)
        );
    }

    private ProviderResponse callProvider(AppProperties.Ai ai, String model, List<ConversationMessageDto> messages)
            throws IOException, InterruptedException {
        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put("model", model);
        reqBody.put("temperature", 0.2);
        ArrayNode reqMessages = reqBody.putArray("messages");
        reqMessages.add(systemPromptNode());
        reqMessages.add(userPromptNode(messages));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(ai.getEndpoint()))
                .timeout(Duration.ofMillis(ai.getReadTimeoutMs()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + ai.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(reqBody.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new ProviderResponse(resp.statusCode(), resp.body() == null ? "" : resp.body());
    }

    private boolean shouldRetryStatus(int statusCode) {
        return statusCode == 429 || statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(250L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String summarizeProviderError(String body) {
        if (body == null) {
            return "";
        }
        String trimmed = body.trim().replace('\n', ' ').replace('\r', ' ');
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.length() > 240) {
            return trimmed.substring(0, 240);
        }
        return trimmed;
    }

    private List<String> buildModelCandidates(AppProperties.Ai ai) {
        Set<String> out = new LinkedHashSet<>();
        if (!isBlank(ai.getModel())) {
            out.add(ai.getModel().trim());
        }
        String fallbacks = ai.getModelFallbacks();
        if (!isBlank(fallbacks)) {
            for (String item : fallbacks.split(",")) {
                if (item != null && !item.trim().isEmpty()) {
                    out.add(item.trim());
                }
            }
        }
        return List.copyOf(out);
    }

    private JsonNode systemPromptNode() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", "system");
        node.put("content",
                "你是更年期女性健康助手，语气要有温度且专业，目标是先理解用户意图，再给可执行建议。"
                        + "请仅输出严格 JSON，不要包含 markdown 或额外文本。"
                        + "字段与要求："
                        + "summary(string，2-4句，包含：用户核心诉求+可能原因解释+当前判断),"
                        + "riskLevel(string:low|medium|high),"
                        + "symptomTags(string[]，3-6个),"
                        + "suggestions(string[]，3-5条，每条都要具体可执行，尽量含时间或步骤)。");
        return node;
    }

    private JsonNode userPromptNode(List<ConversationMessageDto> messages) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", "user");
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下会话生成分析，并特别关注用户想解决的问题与可执行下一步：\n");
        for (ConversationMessageDto m : messages) {
            String role = m.getRole() == null ? "unknown" : m.getRole().trim();
            String content = m.getContent() == null ? "" : m.getContent().trim();
            sb.append(role).append(": ").append(content).append('\n');
        }
        node.put("content", sb.toString());
        return node;
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

    private ConversationInsightDto parseInsightContent(String content) throws IOException {
        String jsonText = stripCodeFence(content.trim());
        JsonNode node = objectMapper.readTree(jsonText);
        ConversationInsightDto dto = new ConversationInsightDto();
        dto.setSummary(textOr(node, "summary", ""));
        dto.setRiskLevel(textOr(node, "riskLevel", "medium"));
        dto.setSymptomTags(readStringArray(node.get("symptomTags")));
        dto.setSuggestions(readStringArray(node.get("suggestions")));
        if (dto.getSummary() == null || dto.getSummary().isBlank()) {
            dto.setSummary("我已理解你的主要困扰。你可以继续补充症状出现频率和触发因素，我会给出更具体的建议。");
        }
        if (dto.getSuggestions() == null || dto.getSuggestions().isEmpty()) {
            dto.setSuggestions(List.of(
                    "连续7天记录症状出现时间、强度和触发因素",
                    "优先调整睡眠节律、饮食刺激和压力管理",
                    "若症状持续加重或影响生活，尽快到妇科/更年期门诊评估"
            ));
        }
        return dto;
    }

    private List<String> readStringArray(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return out;
        }
        for (JsonNode item : node) {
            if (item.isTextual() && !item.asText().isBlank()) {
                out.add(item.asText().trim());
            }
        }
        return out;
    }

    private String textOr(JsonNode node, String field, String fallback) {
        JsonNode v = node.get(field);
        if (v == null || !v.isTextual() || v.asText().isBlank()) {
            return fallback;
        }
        return v.asText().trim();
    }

    private String stripCodeFence(String raw) {
        if (raw.startsWith("```")) {
            int firstNewLine = raw.indexOf('\n');
            int lastFence = raw.lastIndexOf("```");
            if (firstNewLine > 0 && lastFence > firstNewLine) {
                return raw.substring(firstNewLine + 1, lastFence).trim();
            }
        }
        return raw;
    }

    private void validateConfig(AppProperties.Ai ai) {
        if (isBlank(ai.getEndpoint()) || isBlank(ai.getApiKey()) || isBlank(ai.getModel())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ai_provider_not_configured");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record ProviderResponse(int statusCode, String body) {
    }
}
