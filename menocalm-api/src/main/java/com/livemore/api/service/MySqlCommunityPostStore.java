package com.livemore.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livemore.api.web.dto.CommunityCommentDto;
import com.livemore.api.web.dto.CommunityPostDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

@Component
public class MySqlCommunityPostStore implements CommunityPostStore {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final MySqlConnectionProvider connectionProvider;
    private final ObjectMapper objectMapper;

    public MySqlCommunityPostStore(MySqlConnectionProvider connectionProvider, ObjectMapper objectMapper) {
        this.connectionProvider = connectionProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public void createPost(CommunityPostDto post) {
        String insertPostSql = """
                INSERT INTO community_posts (
                    id, author_user_id, title, content, tags_json, created_at_ms, comment_count, like_count, favorite_count
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String insertMediaSql = """
                INSERT INTO community_post_media (post_id, media_id, sort_order)
                VALUES (?, ?, ?)
                """;
        try (var conn = connectionProvider.openConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(insertPostSql)) {
                ps.setString(1, post.getId());
                ps.setString(2, post.getAuthorUserId());
                ps.setString(3, post.getTitle());
                ps.setString(4, post.getContent());
                ps.setString(5, toJson(post.getTags()));
                ps.setLong(6, post.getCreatedAtMs());
                ps.setInt(7, post.getCommentCount() == null ? 0 : post.getCommentCount());
                ps.setInt(8, post.getLikeCount() == null ? 0 : post.getLikeCount());
                ps.setInt(9, post.getFavoriteCount() == null ? 0 : post.getFavoriteCount());
                ps.executeUpdate();
            }
            if (post.getMediaIds() != null && !post.getMediaIds().isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement(insertMediaSql)) {
                    int sortOrder = 1;
                    for (String mediaId : post.getMediaIds()) {
                        ps.setString(1, post.getId());
                        ps.setString(2, mediaId);
                        ps.setInt(3, sortOrder++);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }
            conn.commit();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }

    @Override
    public List<CommunityPostDto> listPosts(int limit) {
        return listPostsByCursor(null, limit);
    }

    @Override
    public List<CommunityPostDto> listPostsByCursor(PostCursor cursor, int limit) {
        String sql = """
                SELECT id, author_user_id, title, content, tags_json, created_at_ms, comment_count, like_count, favorite_count
                FROM community_posts
                WHERE is_deleted = 0 %s
                ORDER BY created_at_ms DESC
                , id DESC
                LIMIT ?
                """;
        String cursorClause = cursor == null ? "" : "AND ((created_at_ms < ?) OR (created_at_ms = ? AND id < ?))";
        String finalSql = sql.formatted(cursorClause);
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(finalSql)) {
            int idx = 1;
            if (cursor != null) {
                ps.setLong(idx++, cursor.getCreatedAtMs());
                ps.setLong(idx++, cursor.getCreatedAtMs());
                ps.setString(idx++, cursor.getPostId());
            }
            ps.setInt(idx, limit);
            List<CommunityPostDto> posts = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapPost(rs, false));
                }
            }
            attachMediaIds(posts);
            return posts;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    @Override
    public List<CommunityPostDto> listPostsByRecommendedCursor(RecommendedPostCursor cursor, int limit) {
        String sql = """
                SELECT id, author_user_id, title, content, tags_json, created_at_ms, comment_count, like_count, favorite_count
                FROM community_posts
                WHERE is_deleted = 0 %s
                ORDER BY (like_count * 2 + comment_count) DESC, created_at_ms DESC, id DESC
                LIMIT ?
                """;
        String cursorClause = cursor == null
                ? ""
                : """
                AND (
                    (like_count * 2 + comment_count) < ?
                    OR (
                        (like_count * 2 + comment_count) = ?
                        AND created_at_ms < ?
                    )
                    OR (
                        (like_count * 2 + comment_count) = ?
                        AND created_at_ms = ?
                        AND id < ?
                    )
                )
                """;
        String finalSql = sql.formatted(cursorClause);
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(finalSql)) {
            int idx = 1;
            if (cursor != null) {
                ps.setInt(idx++, cursor.getHotness());
                ps.setInt(idx++, cursor.getHotness());
                ps.setLong(idx++, cursor.getCreatedAtMs());
                ps.setInt(idx++, cursor.getHotness());
                ps.setLong(idx++, cursor.getCreatedAtMs());
                ps.setString(idx++, cursor.getPostId());
            }
            ps.setInt(idx, limit);
            List<CommunityPostDto> posts = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapPost(rs, false));
                }
            }
            attachMediaIds(posts);
            return posts;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    @Override
    public List<CommunityPostDto> listPostsByCursorForAdmin(PostCursor cursor, int limit) {
        String sql = """
                SELECT id, author_user_id, title, content, tags_json, created_at_ms, comment_count, like_count, favorite_count, is_deleted
                FROM community_posts
                WHERE 1 = 1 %s
                ORDER BY created_at_ms DESC
                , id DESC
                LIMIT ?
                """;
        String cursorClause = cursor == null ? "" : "AND ((created_at_ms < ?) OR (created_at_ms = ? AND id < ?))";
        String finalSql = sql.formatted(cursorClause);
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(finalSql)) {
            int idx = 1;
            if (cursor != null) {
                ps.setLong(idx++, cursor.getCreatedAtMs());
                ps.setLong(idx++, cursor.getCreatedAtMs());
                ps.setString(idx++, cursor.getPostId());
            }
            ps.setInt(idx, limit);
            List<CommunityPostDto> posts = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapPost(rs, true));
                }
            }
            attachMediaIds(posts);
            return posts;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    @Override
    public Optional<CommunityPostDto> findPostById(String postId) {
        String sql = """
                SELECT id, author_user_id, title, content, tags_json, created_at_ms, comment_count, like_count, favorite_count
                FROM community_posts
                WHERE id = ? AND is_deleted = 0
                LIMIT 1
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                CommunityPostDto post = mapPost(rs, false);
                attachMediaIds(List.of(post));
                return Optional.of(post);
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    @Override
    public void createComment(CommunityCommentDto comment) {
        String insertCommentSql = """
                INSERT INTO community_comments (
                    id, post_id, author_user_id, content, created_at_ms
                ) VALUES (?, ?, ?, ?, ?)
                """;
        String updateCountSql = """
                UPDATE community_posts
                SET comment_count = comment_count + 1
                WHERE id = ?
                """;
        try (var conn = connectionProvider.openConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(insertCommentSql)) {
                ps.setString(1, comment.getId());
                ps.setString(2, comment.getPostId());
                ps.setString(3, comment.getAuthorUserId());
                ps.setString(4, comment.getContent());
                ps.setLong(5, comment.getCreatedAtMs());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(updateCountSql)) {
                ps.setString(1, comment.getPostId());
                int updated = ps.executeUpdate();
                if (updated < 1) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post_not_found");
                }
            }
            conn.commit();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }

    @Override
    public List<CommunityCommentDto> listComments(String postId, int limit) {
        String sql = """
                SELECT id, post_id, author_user_id, content, created_at_ms
                FROM community_comments
                WHERE post_id = ? AND is_deleted = 0
                ORDER BY created_at_ms ASC
                LIMIT ?
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, postId);
            ps.setInt(2, limit);
            List<CommunityCommentDto> comments = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CommunityCommentDto dto = new CommunityCommentDto();
                    dto.setId(rs.getString("id"));
                    dto.setPostId(rs.getString("post_id"));
                    dto.setAuthorUserId(rs.getString("author_user_id"));
                    dto.setContent(rs.getString("content"));
                    dto.setCreatedAtMs(rs.getLong("created_at_ms"));
                    comments.add(dto);
                }
            }
            return comments;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    @Override
    public Optional<CommunityCommentDto> findCommentById(String postId, String commentId) {
        String sql = """
                SELECT id, post_id, author_user_id, content, created_at_ms
                FROM community_comments
                WHERE post_id = ? AND id = ? AND is_deleted = 0
                LIMIT 1
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, postId);
            ps.setString(2, commentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                CommunityCommentDto dto = new CommunityCommentDto();
                dto.setId(rs.getString("id"));
                dto.setPostId(rs.getString("post_id"));
                dto.setAuthorUserId(rs.getString("author_user_id"));
                dto.setContent(rs.getString("content"));
                dto.setCreatedAtMs(rs.getLong("created_at_ms"));
                return Optional.of(dto);
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    @Override
    public void likePost(String postId, String userId) {
        String insertLikeSql = """
                INSERT IGNORE INTO community_post_likes (post_id, user_id, created_at_ms)
                VALUES (?, ?, ?)
                """;
        String updateCountSql = """
                UPDATE community_posts
                SET like_count = like_count + 1
                WHERE id = ?
                """;
        try (var conn = connectionProvider.openConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(insertLikeSql)) {
                ps.setString(1, postId);
                ps.setString(2, userId);
                ps.setLong(3, System.currentTimeMillis());
                int inserted = ps.executeUpdate();
                if (inserted > 0) {
                    try (PreparedStatement ups = conn.prepareStatement(updateCountSql)) {
                        ups.setString(1, postId);
                        ups.executeUpdate();
                    }
                }
            }
            conn.commit();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }

    @Override
    public void unlikePost(String postId, String userId) {
        String deleteLikeSql = """
                DELETE FROM community_post_likes
                WHERE post_id = ? AND user_id = ?
                """;
        String updateCountSql = """
                UPDATE community_posts
                SET like_count = GREATEST(like_count - 1, 0)
                WHERE id = ?
                """;
        try (var conn = connectionProvider.openConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(deleteLikeSql)) {
                ps.setString(1, postId);
                ps.setString(2, userId);
                int deleted = ps.executeUpdate();
                if (deleted > 0) {
                    try (PreparedStatement ups = conn.prepareStatement(updateCountSql)) {
                        ups.setString(1, postId);
                        ups.executeUpdate();
                    }
                }
            }
            conn.commit();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }

    @Override
    public Set<String> findLikedPostIds(String userId, List<String> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Set.of();
        }
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < postIds.size(); i++) {
            if (i > 0) {
                inClause.append(",");
            }
            inClause.append("?");
        }
        String sql = "SELECT post_id FROM community_post_likes WHERE user_id = ? AND post_id IN (" + inClause + ")";
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            int idx = 2;
            for (String postId : postIds) {
                ps.setString(idx++, postId);
            }
            Set<String> result = new HashSet<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("post_id"));
                }
            }
            return result;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    @Override
    public void favoritePost(String postId, String userId) {
        String insertSql = """
                INSERT IGNORE INTO community_post_favorites (post_id, user_id, created_at_ms)
                VALUES (?, ?, ?)
                """;
        String updateCountSql = """
                UPDATE community_posts
                SET favorite_count = favorite_count + 1
                WHERE id = ?
                """;
        try (var conn = connectionProvider.openConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, postId);
                ps.setString(2, userId);
                ps.setLong(3, System.currentTimeMillis());
                int inserted = ps.executeUpdate();
                if (inserted > 0) {
                    try (PreparedStatement ups = conn.prepareStatement(updateCountSql)) {
                        ups.setString(1, postId);
                        ups.executeUpdate();
                    }
                }
            }
            conn.commit();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }

    @Override
    public void unfavoritePost(String postId, String userId) {
        String deleteSql = """
                DELETE FROM community_post_favorites
                WHERE post_id = ? AND user_id = ?
                """;
        String updateCountSql = """
                UPDATE community_posts
                SET favorite_count = GREATEST(favorite_count - 1, 0)
                WHERE id = ?
                """;
        try (var conn = connectionProvider.openConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                ps.setString(1, postId);
                ps.setString(2, userId);
                int deleted = ps.executeUpdate();
                if (deleted > 0) {
                    try (PreparedStatement ups = conn.prepareStatement(updateCountSql)) {
                        ups.setString(1, postId);
                        ups.executeUpdate();
                    }
                }
            }
            conn.commit();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }

    @Override
    public Set<String> findFavoritedPostIds(String userId, List<String> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Set.of();
        }
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < postIds.size(); i++) {
            if (i > 0) {
                inClause.append(",");
            }
            inClause.append("?");
        }
        String sql = "SELECT post_id FROM community_post_favorites WHERE user_id = ? AND post_id IN (" + inClause + ")";
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            int idx = 2;
            for (String postId : postIds) {
                ps.setString(idx++, postId);
            }
            Set<String> result = new HashSet<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("post_id"));
                }
            }
            return result;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    @Override
    public void softDeletePost(String postId, String operatorUserId) {
        String sql = """
                UPDATE community_posts
                SET is_deleted = 1,
                    deleted_at_ms = ?,
                    deleted_by_user_id = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_deleted = 0
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, operatorUserId);
            ps.setString(3, postId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }

    @Override
    public void softDeleteComment(String postId, String commentId, String operatorUserId) {
        String deleteCommentSql = """
                UPDATE community_comments
                SET is_deleted = 1,
                    deleted_at_ms = ?,
                    deleted_by_user_id = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE post_id = ? AND id = ? AND is_deleted = 0
                """;
        String updatePostSql = """
                UPDATE community_posts
                SET comment_count = GREATEST(comment_count - 1, 0),
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_deleted = 0
                """;
        try (var conn = connectionProvider.openConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(deleteCommentSql)) {
                ps.setLong(1, System.currentTimeMillis());
                ps.setString(2, operatorUserId);
                ps.setString(3, postId);
                ps.setString(4, commentId);
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    try (PreparedStatement ups = conn.prepareStatement(updatePostSql)) {
                        ups.setString(1, postId);
                        ups.executeUpdate();
                    }
                }
            }
            conn.commit();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }

    private CommunityPostDto mapPost(ResultSet rs, boolean includeDeletedFlag) throws SQLException {
        CommunityPostDto dto = new CommunityPostDto();
        dto.setId(rs.getString("id"));
        dto.setAuthorUserId(rs.getString("author_user_id"));
        dto.setTitle(rs.getString("title"));
        dto.setContent(rs.getString("content"));
        dto.setTags(parseTags(rs.getString("tags_json")));
        dto.setCreatedAtMs(rs.getLong("created_at_ms"));
        dto.setCommentCount(rs.getInt("comment_count"));
        dto.setLikeCount(rs.getInt("like_count"));
        dto.setFavoriteCount(rs.getInt("favorite_count"));
        dto.setLikedByMe(false);
        dto.setFavoritedByMe(false);
        dto.setMediaIds(List.of());
        if (includeDeletedFlag) {
            dto.setDeleted(rs.getInt("is_deleted") == 1);
        }
        return dto;
    }

    private void attachMediaIds(List<CommunityPostDto> posts) {
        if (posts.isEmpty()) {
            return;
        }
        Map<String, CommunityPostDto> postMap = new LinkedHashMap<>();
        for (CommunityPostDto post : posts) {
            postMap.put(post.getId(), post);
        }
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < posts.size(); i++) {
            if (i > 0) {
                inClause.append(",");
            }
            inClause.append("?");
        }
        String sql = "SELECT post_id, media_id FROM community_post_media WHERE post_id IN (" + inClause
                + ") ORDER BY post_id, sort_order ASC";
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (CommunityPostDto post : posts) {
                ps.setString(idx++, post.getId());
            }
            Map<String, List<String>> mediaMap = new HashMap<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String postId = rs.getString("post_id");
                    mediaMap.computeIfAbsent(postId, ignored -> new ArrayList<>()).add(rs.getString("media_id"));
                }
            }
            for (CommunityPostDto post : posts) {
                post.setMediaIds(mediaMap.getOrDefault(post.getId(), List.of()));
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    private List<String> parseTags(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_tags_decode_failed");
        }
    }

    private String toJson(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags == null ? List.of() : tags);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_tags_payload");
        }
    }
}
