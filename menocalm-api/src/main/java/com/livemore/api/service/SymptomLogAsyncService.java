package com.livemore.api.service;

import com.livemore.api.web.dto.ConversationMessageDto;
import com.livemore.api.web.dto.SymptomLogPayloadDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SymptomLogAsyncService {

    private static final Logger log = LoggerFactory.getLogger(SymptomLogAsyncService.class);

    private final RuleBasedSymptomLogExtractor ruleBasedSymptomLogExtractor;
    private final MySqlSymptomLogStore symptomLogStore;
    private final QiniuSymptomExtractionClient qiniuSymptomExtractionClient;

    public SymptomLogAsyncService(
            RuleBasedSymptomLogExtractor ruleBasedSymptomLogExtractor,
            MySqlSymptomLogStore symptomLogStore,
            @Autowired(required = false) QiniuSymptomExtractionClient qiniuSymptomExtractionClient
    ) {
        this.ruleBasedSymptomLogExtractor = ruleBasedSymptomLogExtractor;
        this.symptomLogStore = symptomLogStore;
        this.qiniuSymptomExtractionClient = qiniuSymptomExtractionClient;
    }

    @Async
    public void persistAfterUserMessage(String userId, ConversationMessageDto message) {
        if (message == null || !"user".equalsIgnoreCase(message.getRole())) {
            return;
        }
        String content = message.getContent() == null ? "" : message.getContent();
        SymptomLogPayloadDto payload;
        String source;
        try {
            if (qiniuSymptomExtractionClient != null) {
                payload = qiniuSymptomExtractionClient.extract(content);
                source = "llm";
            } else {
                payload = ruleBasedSymptomLogExtractor.extract(content);
                source = "rule_based";
            }
        } catch (Exception ex) {
            log.warn("symptom_llm_extraction_failed userId={} messageId={}", userId, message.getId(), ex);
            payload = ruleBasedSymptomLogExtractor.extract(content);
            source = "rule_based";
        }
        try {
            String id = "slog_" + UUID.randomUUID().toString().replace("-", "");
            long ts = message.getCreatedAtMs() != null && message.getCreatedAtMs() > 0
                    ? message.getCreatedAtMs()
                    : System.currentTimeMillis();
            String excerpt = trimExcerpt(content, 512);
            symptomLogStore.insert(
                    id,
                    userId,
                    message.getConversationId(),
                    message.getId(),
                    excerpt,
                    payload,
                    source,
                    ts
            );
        } catch (Exception ex) {
            log.warn("symptoms_log_async_insert_failed userId={} messageId={}", userId, message.getId(), ex);
        }
    }

    private static String trimExcerpt(String content, int max) {
        String t = content.trim().replace('\n', ' ').replace('\r', ' ');
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max);
    }
}
