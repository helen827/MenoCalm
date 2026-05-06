package com.livemore.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livemore.api.web.dto.KnowledgeDocDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class MySqlKnowledgeDocStore implements KnowledgeDocStore {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final MySqlConnectionProvider connectionProvider;
    private final ObjectMapper objectMapper;

    public MySqlKnowledgeDocStore(MySqlConnectionProvider connectionProvider, ObjectMapper objectMapper) {
        this.connectionProvider = connectionProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<KnowledgeDocDto> list(Integer limit) {
        int normalized = (limit == null) ? 100 : Math.min(Math.max(limit, 1), 500);
        String sql = """
                SELECT id, title, content, tags_json, source, updated_at_ms
                FROM knowledge_docs
                ORDER BY updated_at_ms DESC
                LIMIT ?
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, normalized);
            try (ResultSet rs = ps.executeQuery()) {
                List<KnowledgeDocDto> docs = new ArrayList<>();
                while (rs.next()) {
                    docs.add(readRow(rs));
                }
                return docs;
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    @Override
    public Optional<KnowledgeDocDto> findById(String id) {
        String sql = """
                SELECT id, title, content, tags_json, source, updated_at_ms
                FROM knowledge_docs
                WHERE id = ?
                LIMIT 1
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(readRow(rs));
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    @Override
    public KnowledgeDocDto upsert(KnowledgeDocDto doc) {
        String sql = """
                INSERT INTO knowledge_docs (id, title, content, tags_json, source, updated_at_ms, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    content = VALUES(content),
                    tags_json = VALUES(tags_json),
                    source = VALUES(source),
                    updated_at_ms = VALUES(updated_at_ms),
                    updated_at = CURRENT_TIMESTAMP
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, doc.getId());
            ps.setString(2, doc.getTitle());
            ps.setString(3, doc.getContent());
            ps.setString(4, writeTags(doc.getTags()));
            ps.setString(5, doc.getSource());
            ps.setLong(6, doc.getUpdatedAtMs());
            ps.executeUpdate();
            return doc;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM knowledge_docs WHERE id = ?";
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }

    private KnowledgeDocDto readRow(ResultSet rs) throws SQLException {
        KnowledgeDocDto dto = new KnowledgeDocDto();
        dto.setId(rs.getString("id"));
        dto.setTitle(rs.getString("title"));
        dto.setContent(rs.getString("content"));
        dto.setTags(readTags(rs.getString("tags_json")));
        dto.setSource(rs.getString("source"));
        dto.setUpdatedAtMs(rs.getLong("updated_at_ms"));
        return dto;
    }

    private List<String> readTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(tagsJson, STRING_LIST);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private String writeTags(List<String> tags) {
        List<String> normalized = tags == null ? Collections.emptyList() : tags;
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_tags");
        }
    }
}
