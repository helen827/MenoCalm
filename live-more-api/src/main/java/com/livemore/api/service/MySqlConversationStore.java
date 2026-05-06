package com.livemore.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livemore.api.web.dto.ConversationInsightDto;
import com.livemore.api.web.dto.ConversationMessageDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MySqlConversationStore implements ConversationStore {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final MySqlConnectionProvider connectionProvider;
    private final ObjectMapper objectMapper;

    public MySqlConversationStore(MySqlConnectionProvider connectionProvider, ObjectMapper objectMapper) {
        this.connectionProvider = connectionProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ConversationMessageDto> listMessages(String userId, String conversationId, int limit) {
        String sql = """
                SELECT id, conversation_id, role, content, created_at_ms, metadata_json
                FROM conversation_messages
                WHERE user_id = ? AND conversation_id = ?
                ORDER BY created_at_ms DESC
                LIMIT ?
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, conversationId);
            ps.setInt(3, limit);
            List<ConversationMessageDto> reversed = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ConversationMessageDto dto = new ConversationMessageDto();
                    dto.setId(rs.getString("id"));
                    dto.setConversationId(rs.getString("conversation_id"));
                    dto.setRole(rs.getString("role"));
                    dto.setContent(rs.getString("content"));
                    dto.setCreatedAtMs(rs.getLong("created_at_ms"));
                    dto.setMetadata(parseMetadata(rs.getString("metadata_json")));
                    reversed.add(dto);
                }
            }
            List<ConversationMessageDto> ordered = new ArrayList<>(reversed.size());
            for (int i = reversed.size() - 1; i >= 0; i--) {
                ordered.add(reversed.get(i));
            }
            return ordered;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    @Override
    public void appendMessage(String userId, ConversationMessageDto message) {
        String sql = """
                INSERT INTO conversation_messages (
                    id, user_id, conversation_id, role, content, created_at_ms, metadata_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, message.getId());
            ps.setString(2, userId);
            ps.setString(3, message.getConversationId());
            ps.setString(4, message.getRole());
            ps.setString(5, message.getContent());
            ps.setLong(6, message.getCreatedAtMs());
            ps.setString(7, toJson(message.getMetadata()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }

    @Override
    public Optional<ConversationInsightDto> findInsight(String userId, String conversationId) {
        String sql = """
                SELECT insight_json
                FROM conversation_insights
                WHERE user_id = ? AND conversation_id = ?
                LIMIT 1
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String json = rs.getString("insight_json");
                return Optional.of(parseInsight(json));
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    @Override
    public void upsertInsight(String userId, String conversationId, ConversationInsightDto insight) {
        String sql = """
                INSERT INTO conversation_insights (user_id, conversation_id, insight_json, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                    insight_json = VALUES(insight_json),
                    updated_at = CURRENT_TIMESTAMP
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, conversationId);
            ps.setString(3, toInsightJson(insight));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }

    private Map<String, Object> parseMetadata(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_metadata_decode_failed");
        }
    }

    private ConversationInsightDto parseInsight(String json) {
        if (json == null || json.isBlank()) {
            return new ConversationInsightDto();
        }
        try {
            return objectMapper.readValue(json, ConversationInsightDto.class);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_insight_decode_failed");
        }
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_message_metadata");
        }
    }

    private String toInsightJson(ConversationInsightDto insight) {
        try {
            return objectMapper.writeValueAsString(insight);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_insight_payload");
        }
    }
}
