package com.livemore.api.service;

import com.livemore.api.web.dto.ConversationInsightDto;
import com.livemore.api.web.dto.ConversationMessageDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ConversationService {

    private static final Set<String> ALLOWED_ROLES = Set.of("user", "assistant", "system");

    private final ConversationStore conversationStore;
    private final ConversationAnalysisClient analysisClient;
    private final FallbackConversationInsightGenerator fallbackGenerator;

    public ConversationService(
            ConversationStore conversationStore,
            ConversationAnalysisClient analysisClient,
            FallbackConversationInsightGenerator fallbackGenerator
    ) {
        this.conversationStore = conversationStore;
        this.analysisClient = analysisClient;
        this.fallbackGenerator = fallbackGenerator;
    }

    public List<ConversationMessageDto> listMessages(
            String authenticatedUserId,
            String queryUserId,
            String conversationId,
            Integer limit
    ) {
        assertSameUser(authenticatedUserId, queryUserId);
        String normalizedConversationId = requireConversationId(conversationId);
        int normalizedLimit = normalizeLimit(limit);
        return conversationStore.listMessages(queryUserId, normalizedConversationId, normalizedLimit);
    }

    public ConversationMessageDto appendMessage(
            String authenticatedUserId,
            String queryUserId,
            ConversationMessageDto message
    ) {
        assertSameUser(authenticatedUserId, queryUserId);
        if (message == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body_required");
        }
        String conversationId = requireConversationId(message.getConversationId());
        String role = normalizeRole(message.getRole());
        String content = requireNonBlank(message.getContent(), "content_required");
        long now = System.currentTimeMillis();
        if (message.getCreatedAtMs() == null || message.getCreatedAtMs() <= 0) {
            message.setCreatedAtMs(now);
        }
        if (message.getId() == null || message.getId().isBlank()) {
            message.setId("msg_" + UUID.randomUUID().toString().replace("-", ""));
        }
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        conversationStore.appendMessage(queryUserId, message);
        return message;
    }

    public ConversationInsightDto getInsight(
            String authenticatedUserId,
            String queryUserId,
            String conversationId
    ) {
        assertSameUser(authenticatedUserId, queryUserId);
        String normalizedConversationId = requireConversationId(conversationId);
        return conversationStore.findInsight(queryUserId, normalizedConversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "insight_not_found"));
    }

    public ConversationInsightDto upsertInsight(
            String authenticatedUserId,
            String queryUserId,
            String conversationId,
            ConversationInsightDto insight
    ) {
        assertSameUser(authenticatedUserId, queryUserId);
        String normalizedConversationId = requireConversationId(conversationId);
        if (insight == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body_required");
        }
        insight.setConversationId(normalizedConversationId);
        if (insight.getUpdatedAtMs() == null || insight.getUpdatedAtMs() <= 0) {
            insight.setUpdatedAtMs(System.currentTimeMillis());
        }
        conversationStore.upsertInsight(queryUserId, normalizedConversationId, insight);
        return insight;
    }

    public ConversationInsightDto analyzeAndUpsertInsight(
            String authenticatedUserId,
            String queryUserId,
            String conversationId
    ) {
        assertSameUser(authenticatedUserId, queryUserId);
        String normalizedConversationId = requireConversationId(conversationId);
        List<ConversationMessageDto> messages = conversationStore.listMessages(queryUserId, normalizedConversationId, 200);
        if (messages.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversation_empty");
        }
        try {
            ConversationInsightDto insight = analysisClient.analyze(messages);
            insight.setConversationId(normalizedConversationId);
            insight.setUpdatedAtMs(System.currentTimeMillis());
            if (insight.getSource() == null || insight.getSource().isBlank()) {
                insight.setSource("remote");
            }
            conversationStore.upsertInsight(queryUserId, normalizedConversationId, insight);
            return insight;
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 503) {
                return conversationStore.findInsight(queryUserId, normalizedConversationId)
                        .orElseGet(() -> {
                            ConversationInsightDto fallback = fallbackGenerator.generate(messages);
                            fallback.setConversationId(normalizedConversationId);
                            fallback.setUpdatedAtMs(System.currentTimeMillis());
                            fallback.setDegradedReason(ex.getReason());
                            conversationStore.upsertInsight(queryUserId, normalizedConversationId, fallback);
                            return fallback;
                        });
            }
            throw ex;
        }
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
        return requireNonBlank(value, "conversationId_required");
    }

    private String requireNonBlank(String value, String errorCode) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
        }
        return value.trim();
    }

    private String normalizeRole(String value) {
        String role = requireNonBlank(value, "role_required").toLowerCase();
        if (!ALLOWED_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_message_role");
        }
        return role;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 100;
        }
        if (limit < 1 || limit > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_limit");
        }
        return limit;
    }
}
