package com.livemore.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livemore.api.web.dto.SymptomLogPayloadDto;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Component
public class MySqlSymptomLogStore {

    private final MySqlConnectionProvider connectionProvider;
    private final ObjectMapper objectMapper;

    public MySqlSymptomLogStore(MySqlConnectionProvider connectionProvider, ObjectMapper objectMapper) {
        this.connectionProvider = connectionProvider;
        this.objectMapper = objectMapper;
    }

    public void insert(
            String id,
            String userId,
            String conversationId,
            String messageId,
            String excerpt,
            SymptomLogPayloadDto payload,
            String source,
            long createdAtMs
    ) {
        String sql = """
                INSERT INTO symptoms_log (
                    id, user_id, conversation_id, message_id, user_message_excerpt, payload_json, source, created_at_ms
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            ps.setString(3, conversationId);
            ps.setString(4, messageId);
            ps.setString(5, excerpt);
            ps.setString(6, objectMapper.writeValueAsString(payload));
            ps.setString(7, source);
            ps.setLong(8, createdAtMs);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("symptoms_log_insert_failed", ex);
        }
    }

    public record SymptomLogEntryRow(String payloadJson, long createdAtMs, String source) {
    }

    public List<SymptomLogEntryRow> listSince(String userId, long sinceMsInclusive) {
        String sql = """
                SELECT payload_json, created_at_ms, source
                FROM symptoms_log
                WHERE user_id = ? AND created_at_ms >= ?
                ORDER BY created_at_ms ASC
                """;
        List<SymptomLogEntryRow> out = new ArrayList<>();
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setLong(2, sinceMsInclusive);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new SymptomLogEntryRow(
                            rs.getString("payload_json"),
                            rs.getLong("created_at_ms"),
                            rs.getString("source")
                    ));
                }
            }
            return out;
        } catch (Exception ex) {
            throw new IllegalStateException("symptoms_log_list_failed", ex);
        }
    }
}
