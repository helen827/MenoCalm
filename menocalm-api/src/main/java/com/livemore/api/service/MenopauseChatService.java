package com.livemore.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.livemore.api.config.AppProperties;
import com.livemore.api.web.dto.ConversationMessageDto;
import com.livemore.api.web.dto.MenopauseChatReplyDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Component A target: Markdown chat using same calendar-day transcript as analyze.
 */
@Service
public class MenopauseChatService {

    /** Aligned with analyze path: Chinese-first, same disclaimer string as JSON insight workflow. */
    private static final String CHAT_SYSTEM_PROMPT = """
            # 角色
            你是擅长女性更年期（围绝经期）沟通的健康顾问：有妇科/内分泌常识，说话像可信亲友，医学边界清晰。

            # 输入
            用户**同一自然日**内的多条对话（可能分几次发送）。请把整段当作连续上下文，优先回应**最后一条**里的具体问题，同时不要「失忆」：若前文提过其他症状或场景，用一两句自然串起来。

            # 输出语言
            默认使用**简体中文**。若用户全文明显以其他语言书写，则跟随其主要语言。

            # 排版（便于手机阅读）
            - 段落之间**空一行**；不要把全文挤成一段。
            - 若回答较长，先用 **## 小标题** 分 2～4 段（如「先说说您的感受」「可能的原因」「您可以试试」「需要警惕的情况」），每段内再用短句或列表。
            - 列表用 `- ` 条目，每条一行，避免超长单句。

            # 结构与深度（Markdown，小标题与列表适度即可）
            1. 开头先共情，并用自己的话简要复述她当下最在意的一点（不要空泛口号）。
            2. 结合「可能与更年期激素波动相关」的机制，用**通俗比喻**解释，避免堆砌冷冰冰术语；不确定处用「可能与…有关」「常见情况下」等表述，**不下诊断**。
            3. 给出 **2～4 条**可执行的**非药物**生活建议（尽量含可尝试的强度、频次或时间窗口，避免「注意休息」式空话）。
            4. 若出现胸痛、呼吸困难、晕厥、大出血、高热、意识改变、偏瘫样症状等，须在建议前明确提示**尽快线下就医**。

            # 边界
            - 禁止推荐具体处方药品牌；必要时可写「是否用药请医生评估」。
            - 语气温柔、稳重、具体；避免模板化套话或与用户问题无关的长篇背景。

            # 免责声明（必须遵守）
            全文**最后一行且单独成行**，必须与下列字符串**逐字完全一致**（不得改写标点或增删字）：
            %s
            """.formatted(QiniuConversationAnalysisClient.MEDICAL_DISCLAIMER).stripIndent();

    private static final int CHAT_MAX_TOKENS = 2048;

    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final QiniuChatCompletionClient qiniuChatCompletionClient;
    private final java.time.ZoneId dailyContextZone;

    public MenopauseChatService(
            ConversationService conversationService,
            ObjectMapper objectMapper,
            AppProperties appProperties,
            @Autowired(required = false) QiniuChatCompletionClient qiniuChatCompletionClient,
            @Value("${app.conversations.daily-context-zone-id:Asia/Shanghai}") String dailyContextZoneId
    ) {
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.qiniuChatCompletionClient = qiniuChatCompletionClient;
        this.dailyContextZone = ConversationDailyContext.parseZoneId(dailyContextZoneId);
    }

    public MenopauseChatReplyDto generateMarkdownReply(
            String authenticatedUserId,
            String queryUserId,
            String conversationId
    ) {
        if (qiniuChatCompletionClient == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ai_provider_not_configured");
        }
        assertSameUser(authenticatedUserId, queryUserId);
        String convId = requireConversationId(conversationId);
        List<ConversationMessageDto> all = conversationService.listMessages(authenticatedUserId, queryUserId, convId, 200);
        if (all.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversation_empty");
        }
        List<ConversationMessageDto> day = ConversationDailyContext.sameCalendarDayAsLatest(all, dailyContextZone);
        if (day.isEmpty()) {
            day = all;
        }

        List<JsonNode> messages = new ArrayList<>();
        ObjectNode system = objectMapper.createObjectNode();
        system.put("role", "system");
        system.put("content", CHAT_SYSTEM_PROMPT);
        messages.add(system);

        for (ConversationMessageDto m : day) {
            String role = normalizeOpenAiRole(m.getRole());
            String content = m.getContent() == null ? "" : m.getContent().trim();
            if (content.isEmpty()) {
                continue;
            }
            ObjectNode node = objectMapper.createObjectNode();
            node.put("role", role);
            node.put("content", content);
            messages.add(node);
        }
        if (messages.size() <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversation_empty");
        }

        AppProperties.Ai ai = appProperties.getAi();
        String model = ai.getModel() == null || ai.getModel().isBlank() ? "" : ai.getModel().trim();
        String markdown;
        try {
            markdown = qiniuChatCompletionClient.complete(messages, ai.getChatTemperature(), model, CHAT_MAX_TOKENS);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ai_chat_failed", ex);
        }

        ConversationMessageDto assistant = new ConversationMessageDto();
        assistant.setId("msg_" + UUID.randomUUID().toString().replace("-", ""));
        assistant.setConversationId(convId);
        assistant.setRole("assistant");
        assistant.setContent(markdown);
        assistant.setCreatedAtMs(System.currentTimeMillis());
        conversationService.appendMessage(authenticatedUserId, queryUserId, assistant);

        MenopauseChatReplyDto dto = new MenopauseChatReplyDto();
        dto.setMarkdown(markdown);
        dto.setSource("qiniu-chat");
        return dto;
    }

    private static String normalizeOpenAiRole(String role) {
        if (role == null || role.isBlank()) {
            return "user";
        }
        String r = role.trim().toLowerCase();
        if ("assistant".equals(r) || "system".equals(r) || "user".equals(r)) {
            return r;
        }
        return "user";
    }

    private void assertSameUser(String authenticatedUserId, String queryUserId) {
        if (queryUserId == null || queryUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId_required");
        }
        if (!authenticatedUserId.equals(queryUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "user_mismatch");
        }
    }

    private String requireConversationId(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversationId_required");
        }
        return value.trim();
    }
}
