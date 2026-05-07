package com.livemore.api.service;

import com.livemore.api.domain.CommunityMediaDocument;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Override
    public Map<String, CommunityMediaDocument> findByIds(Collection<String> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return Map.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String id : mediaIds) {
            if (id != null && !id.isBlank()) {
                unique.add(id.trim());
            }
        }
        if (unique.isEmpty()) {
            return Map.of();
        }
        List<String> idList = new ArrayList<>(unique);
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < idList.size(); i++) {
            if (i > 0) {
                inClause.append(",");
            }
            inClause.append("?");
        }
        String sql = """
                SELECT id, owner_user_id, bucket, object_key, mime_type, size_bytes, sha256, visibility, status, created_at, updated_at
                FROM community_media
                WHERE id IN (""" + inClause + ")";
        Map<String, CommunityMediaDocument> out = new HashMap<>();
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < idList.size(); i++) {
                ps.setString(i + 1, idList.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CommunityMediaDocument doc = mapRow(rs);
                    out.put(doc.getId(), doc);
                }
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
        return out;
    }

    private static CommunityMediaDocument mapRow(ResultSet rs) throws SQLException {
        CommunityMediaDocument doc = new CommunityMediaDocument();
        doc.setId(rs.getString("id"));
        doc.setOwnerUserId(rs.getString("owner_user_id"));
        doc.setBucket(rs.getString("bucket"));
        doc.setObjectKey(rs.getString("object_key"));
        doc.setMimeType(rs.getString("mime_type"));
        doc.setSizeBytes(rs.getLong("size_bytes"));
        doc.setSha256(rs.getString("sha256"));
        doc.setVisibility(rs.getString("visibility"));
        doc.setStatus(rs.getString("status"));
        Timestamp created = rs.getTimestamp("created_at");
        doc.setCreatedAt(created != null ? created.toInstant() : Instant.EPOCH);
        Timestamp updated = rs.getTimestamp("updated_at");
        doc.setUpdatedAt(updated != null ? updated.toInstant() : Instant.EPOCH);
        return doc;
    }

    @Override
    public int markMediaReadyIfPending(String mediaId, String ownerUserId) {
        if (mediaId == null || mediaId.isBlank() || ownerUserId == null || ownerUserId.isBlank()) {
            return 0;
        }
        String sql = """
                UPDATE community_media
                SET status = 'READY', updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND owner_user_id = ? AND status = 'PENDING_UPLOAD'
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mediaId.trim());
            ps.setString(2, ownerUserId.trim());
            return ps.executeUpdate();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }
}
