package com.livemore.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livemore.api.web.dto.CommunityFeedFileDto;
import com.livemore.api.web.dto.CommunityFeedItemDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class MySqlCommunityFeedStore implements CommunityFeedStore {

    private static final String CURRENT_ID = "current";
    private static final TypeReference<List<CommunityFeedItemDto>> FEED_LIST_TYPE = new TypeReference<>() {
    };

    private final MySqlConnectionProvider connectionProvider;
    private final ObjectMapper objectMapper;

    public MySqlCommunityFeedStore(MySqlConnectionProvider connectionProvider, ObjectMapper objectMapper) {
        this.connectionProvider = connectionProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<CommunityFeedFileDto> loadCurrent() {
        String sql = "SELECT official_json, user_json FROM community_feed_snapshot WHERE snapshot_id = ?";
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, CURRENT_ID);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                CommunityFeedFileDto dto = new CommunityFeedFileDto();
                dto.setOfficial(parseFeedList(rs.getString("official_json")));
                dto.setUser(parseFeedList(rs.getString("user_json")));
                return Optional.of(dto);
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    @Override
    public boolean existsCurrent() {
        String sql = "SELECT 1 FROM community_feed_snapshot WHERE snapshot_id = ? LIMIT 1";
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, CURRENT_ID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    @Override
    public void upsertCurrent(CommunityFeedFileDto feed) {
        String sql = """
                INSERT INTO community_feed_snapshot (snapshot_id, official_json, user_json, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                    official_json = VALUES(official_json),
                    user_json = VALUES(user_json),
                    updated_at = CURRENT_TIMESTAMP
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, CURRENT_ID);
            ps.setString(2, toJson(feed.getOfficial()));
            ps.setString(3, toJson(feed.getUser()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }

    private List<CommunityFeedItemDto> parseFeedList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, FEED_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_feed_decode_failed");
        }
    }

    private String toJson(List<CommunityFeedItemDto> list) {
        try {
            return objectMapper.writeValueAsString(list == null ? List.of() : list);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_feed_payload");
        }
    }
}
