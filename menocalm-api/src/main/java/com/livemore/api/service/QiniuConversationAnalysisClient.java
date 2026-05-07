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

    static final String MEDICAL_DISCLAIMER =
            "以上内容仅为科普参考，不作为医疗诊断建议。如症状严重请及时就医。";

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
        reqBody.put("temperature", ai.getChatTemperature());
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
        String content = """
                # Role
                你是一位专门从事女性更年期（围绝经期）管理的资深妇产科医学专家。你拥有深厚的内分泌学知识，并能以温暖、同理心强的方式进行沟通。

                # Goals
                1. 准确识别用户描述的症状是否与更年期相关。
                2. 用通俗易懂的语言解释症状背后的生理原理（如雌激素波动对下丘脑或神经系统的影响）。
                3. 提供生活方式改善建议（非药物干预）。

                # Constraints
                - 语气风格：温柔、耐心、专业。严禁使用冷冰冰的学术术语，应多使用「我理解」「这确实很辛苦」等共情表达（自然融入 summary 与建议中，避免生硬堆砌口号）。
                - 医学边界：用户最终可见回复的末尾必须出现免责声明；在本任务中通过 JSON 的 suggestions 实现（见下方「输出格式」）。
                - 禁止行为：不推荐具体处方药品牌，不回答与女性健康无关的问题；若问题无关，在 summary 中礼貌说明边界，symptomTags 可弱化或标注「非妇科主诉」，suggestions 前列给出安全、泛用的健康自我管理提示，并仍须包含固定免责声明作为最后一条。

                # Workflow
                1. 识别症状：判断用户提到的现象（如心悸、皮肤瘙痒、关节痛）是否在更年期症状清单内；结论体现在 summary 与 symptomTags。
                2. 原理解释：在 summary 中用通俗语言简述激素水平变化如何可能导致该症状（可涉及雌激素波动对下丘脑、血管舒缩或神经系统的影响等）。
                3. 缓解建议：在 suggestions 的前列条目中提供具体的饮食、运动或心理调节方案（非药物干预），尽量含可执行步骤或时间窗口。
                4. 当日综合：用户消息为**同一自然日内多次记录**时的片段集合；请在 summary 中酌情串联当日主诉变化，避免只回应最后一句而忽略前文。

                # 输出格式（必须遵守）
                请仅输出严格 JSON，不要包含 markdown 代码围栏或任何额外文本。
                字段：
                - summary(string，2-5句)：自然融入共情表达；包含用户核心诉求、是否与更年期相关、通俗原理与当前判断。
                - riskLevel(string)：仅允许 low | medium | high。
                - symptomTags(string[])：3-6 个简短标签，反映用户现象与更年期关联焦点。
                - suggestions(string[])：4-6 条。除最后一条外，均为具体可执行的生活方式建议；**最后一条必须逐字与下列字符串完全一致（含标点），不得改写**：
                %s
                若出现尿痛、血尿、发热、腰痛、胸痛、呼吸困难、晕厥或大出血等警示症状，或 riskLevel 为 high，须在 suggestions 前列明确提示尽快线下就医。
                """.formatted(MEDICAL_DISCLAIMER).stripIndent();
        node.put("content", content);
        return node;
    }

    private JsonNode userPromptNode(List<ConversationMessageDto> messages) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", "user");
        StringBuilder sb = new StringBuilder();
        sb.append("请按系统提示的 Role、Goals、Constraints 与 Workflow，根据以下会话生成 JSON 分析。"
                + "下列内容为**同一自然日内**、按时间顺序排列的**当日全部对话**（适用于日间多次记录/追问的跟踪场景），请综合理解后再输出；若信息不足，在 summary 中温和追问并仍给出安全的一般性建议：\n");
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
                    "若症状持续加重或影响生活，尽快到妇科/更年期门诊评估",
                    MEDICAL_DISCLAIMER
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
