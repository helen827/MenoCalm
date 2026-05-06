package com.livemore.api.service;

import com.livemore.api.domain.CommunityMediaDocument;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Component
public class MySqlCommunityMediaStore implements CommunityMediaStore {

    private final MySqlConnectionProvider connectionProvider;

    public MySqlCommunityMediaStore(MySqlConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public void upsertMedia(CommunityMediaDocument media) {
        String sql = """
                INSERT INTO community_media (
                    id, owner_user_id, bucket, object_key, mime_type, size_bytes, sha256, visibility, status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    owner_user_id = VALUES(owner_user_id),
                    mime_type = VALUES(mime_type),
                    size_bytes = VALUES(size_bytes),
                    sha256 = VALUES(sha256),
                    visibility = VALUES(visibility),
                    status = VALUES(status),
                    updated_at = VALUES(updated_at)
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, media.getId());
            ps.setString(2, media.getOwnerUserId());
            ps.setString(3, media.getBucket());
            ps.setString(4, media.getObjectKey());
            ps.setString(5, media.getMimeType());
            ps.setLong(6, media.getSizeBytes());
            ps.setString(7, media.getSha256());
            ps.setString(8, media.getVisibility());
            ps.setString(9, media.getStatus());
            ps.setTimestamp(10, Timestamp.from(media.getCreatedAt()));
            ps.setTimestamp(11, Timestamp.from(media.getUpdatedAt()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }

    @Override
    public int countOwnedMedia(String ownerUserId, List<String> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return 0;
        }
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < mediaIds.size(); i++) {
            if (i > 0) {
                inClause.append(",");
            }
            inClause.append("?");
        }
        String sql = "SELECT COUNT(*) AS c FROM community_media WHERE owner_user_id = ? AND id IN (" + inClause + ")";
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerUserId);
            int idx = 2;
            for (String mediaId : mediaIds) {
                ps.setString(idx++, mediaId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return 0;
                }
                return rs.getInt("c");
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }
}
