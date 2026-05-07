package com.livemore.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livemore.api.web.dto.ConversationInsightDto;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;

@Component
public class MySqlConversationInsightSnapshotStore {

    private final MySqlConnectionProvider connectionProvider;
    private final ObjectMapper objectMapper;

    public MySqlConversationInsightSnapshotStore(MySqlConnectionProvider connectionProvider, ObjectMapper objectMapper) {
        this.connectionProvider = connectionProvider;
        this.objectMapper = objectMapper;
    }

    public void insert(
            String id,
            String userId,
            String conversationId,
            ConversationInsightDto insight,
            long createdAtMs
    ) {
        String sql = """
                INSERT INTO conversation_insight_snapshots (
                    id, user_id, conversation_id, insight_json, source, created_at_ms
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            ps.setString(3, conversationId);
            ps.setString(4, toJson(insight));
            String source = insight.getSource() == null || insight.getSource().isBlank() ? "unknown" : insight.getSource().trim();
            ps.setString(5, source);
            ps.setLong(6, createdAtMs);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("insight_snapshot_insert_failed", ex);
        }
    }

    private String toJson(ConversationInsightDto insight) throws JsonProcessingException {
        return objectMapper.writeValueAsString(insight);
    }
}
