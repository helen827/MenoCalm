package com.livemore.api.service;

import com.livemore.api.web.dto.ConversationInsightDto;
import com.livemore.api.web.dto.ConversationMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Best-effort persistence for downstream analytics (symptom prevalence, lifestyle correlation, etc.).
 * Failures are logged and never break the user-facing conversation path.
 */
@Component
public class MenopauseAnalyticsWriter {

    private static final Logger log = LoggerFactory.getLogger(MenopauseAnalyticsWriter.class);

    private final SymptomLogAsyncService symptomLogAsyncService;
    private final MySqlConversationInsightSnapshotStore insightSnapshotStore;

    public MenopauseAnalyticsWriter(
            SymptomLogAsyncService symptomLogAsyncService,
            MySqlConversationInsightSnapshotStore insightSnapshotStore
    ) {
        this.symptomLogAsyncService = symptomLogAsyncService;
        this.insightSnapshotStore = insightSnapshotStore;
    }

    public void recordAfterUserMessage(String userId, ConversationMessageDto message) {
        if (message == null || !"user".equalsIgnoreCase(message.getRole())) {
            return;
        }
        try {
            symptomLogAsyncService.persistAfterUserMessage(userId, message);
        } catch (Exception ex) {
            log.warn("symptoms_log_async_schedule_failed userId={} messageId={}", userId, message.getId(), ex);
        }
    }

    public void recordInsightSnapshot(String userId, String conversationId, ConversationInsightDto insight) {
        if (insight == null) {
            return;
        }
        try {
            String id = "isnap_" + java.util.UUID.randomUUID().toString().replace("-", "");
            long ts = insight.getUpdatedAtMs() != null && insight.getUpdatedAtMs() > 0
                    ? insight.getUpdatedAtMs()
                    : System.currentTimeMillis();
            insightSnapshotStore.insert(id, userId, conversationId, insight, ts);
        } catch (Exception ex) {
            log.warn("insight_snapshot_write_failed userId={} conversationId={}", userId, conversationId, ex);
        }
    }

}
