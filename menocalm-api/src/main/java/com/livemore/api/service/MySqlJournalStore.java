package com.livemore.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livemore.api.web.dto.ExtractedDataDto;
import com.livemore.api.web.dto.JournalEntryDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class MySqlJournalStore implements JournalStore {

    private static final String UPSERT_SQL = """
            INSERT INTO journal_entries (
                user_id, entry_id, date_text, raw_text, created_at_ms, revision, extracted_json
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                date_text = VALUES(date_text),
                raw_text = VALUES(raw_text),
                created_at_ms = VALUES(created_at_ms),
                revision = VALUES(revision),
                extracted_json = VALUES(extracted_json),
                updated_at = CURRENT_TIMESTAMP
            """;

    private final MySqlConnectionProvider connectionProvider;
    private final ObjectMapper objectMapper;

    public MySqlJournalStore(MySqlConnectionProvider connectionProvider, ObjectMapper objectMapper) {
        this.connectionProvider = connectionProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<JournalEntryDto> listForUser(String userId) {
        String sql = """
                SELECT entry_id, date_text, raw_text, created_at_ms, revision, extracted_json
                FROM journal_entries
                WHERE user_id = ?
                ORDER BY created_at_ms ASC
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<JournalEntryDto> result = new ArrayList<>();
                while (rs.next()) {
                    JournalEntryDto dto = new JournalEntryDto();
                    dto.setId(rs.getString("entry_id"));
                    dto.setDate(rs.getString("date_text"));
                    dto.setRawText(rs.getString("raw_text"));
                    dto.setCreatedAt(rs.getDouble("created_at_ms"));
                    int revision = rs.getInt("revision");
                    dto.setRevision(rs.wasNull() ? null : revision);
                    dto.setExtracted(parseExtracted(rs.getString("extracted_json")));
                    result.add(dto);
                }
                return result;
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    @Override
    public void replaceAllForUser(String userId, List<JournalEntryDto> entries) {
        try (var conn = connectionProvider.openConnection()) {
            conn.setAutoCommit(false);
            Set<String> keepIds = new HashSet<>();
            for (JournalEntryDto entry : entries) {
                keepIds.add(entry.getId());
            }
            deleteRemoved(conn, userId, keepIds);
            upsertAll(conn, userId, entries);
            conn.commit();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }

    private void deleteRemoved(java.sql.Connection conn, String userId, Set<String> keepIds) throws SQLException {
        if (keepIds.isEmpty()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM journal_entries WHERE user_id = ?")) {
                ps.setString(1, userId);
                ps.executeUpdate();
            }
            return;
        }
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < keepIds.size(); i++) {
            if (i > 0) {
                inClause.append(",");
            }
            inClause.append("?");
        }
        String sql = "DELETE FROM journal_entries WHERE user_id = ? AND entry_id NOT IN (" + inClause + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            int idx = 2;
            for (String id : keepIds) {
                ps.setString(idx++, id);
            }
            ps.executeUpdate();
        }
    }

    private void upsertAll(java.sql.Connection conn, String userId, List<JournalEntryDto> entries) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
            for (JournalEntryDto entry : entries) {
                ps.setString(1, userId);
                ps.setString(2, entry.getId());
                ps.setString(3, entry.getDate());
                ps.setString(4, entry.getRawText());
                ps.setDouble(5, entry.getCreatedAt());
                if (entry.getRevision() == null) {
                    ps.setNull(6, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(6, entry.getRevision());
                }
                ps.setString(7, toJson(entry.getExtracted() != null ? entry.getExtracted() : new ExtractedDataDto()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private ExtractedDataDto parseExtracted(String extractedJson) {
        if (extractedJson == null || extractedJson.isBlank()) {
            return new ExtractedDataDto();
        }
        try {
            return objectMapper.readValue(extractedJson, ExtractedDataDto.class);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_extracted_decode_failed");
        }
    }

    private String toJson(ExtractedDataDto extractedDataDto) {
        try {
            return objectMapper.writeValueAsString(extractedDataDto);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_extracted_payload");
        }
    }
}
