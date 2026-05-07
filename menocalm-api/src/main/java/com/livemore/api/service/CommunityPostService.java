package com.livemore.api.service;

import com.livemore.api.config.AppProperties;
import com.livemore.api.domain.CommunityMediaDocument;
import com.livemore.api.web.dto.CommunityCommentDto;
import com.livemore.api.web.dto.CommunityPostPageDto;
import com.livemore.api.web.dto.CommunityPostDto;
import com.livemore.api.web.dto.CreateCommunityCommentRequest;
import com.livemore.api.web.dto.CreateCommunityPostRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Service
public class CommunityPostService {

    private static final Logger LOG = LoggerFactory.getLogger(CommunityPostService.class);

    private final CommunityPostStore communityPostStore;
    private final CommunityMediaStore communityMediaStore;
    private final CommunityMediaPublicUrlBuilder mediaPublicUrlBuilder;
    private final AppProperties appProperties;

    public CommunityPostService(
            CommunityPostStore communityPostStore,
            CommunityMediaStore communityMediaStore,
            CommunityMediaPublicUrlBuilder mediaPublicUrlBuilder,
            AppProperties appProperties
    ) {
        this.communityPostStore = communityPostStore;
        this.communityMediaStore = communityMediaStore;
        this.mediaPublicUrlBuilder = mediaPublicUrlBuilder;
        this.appProperties = appProperties;
    }

    public CommunityPostDto createPost(
            String authenticatedUserId,
            String queryUserId,
            CreateCommunityPostRequest request
    ) {
        assertSameUser(authenticatedUserId, queryUserId);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body_required");
        }
        CommunityPostDto post = new CommunityPostDto();
        post.setId("post_" + UUID.randomUUID().toString().replace("-", ""));
        post.setAuthorUserId(queryUserId);
        post.setTitle(requireNonBlank(request.getTitle(), "title_required"));
        post.setContent(requireNonBlank(request.getContent(), "content_required"));
        post.setTags(normalizeStringList(request.getTags(), 10, "too_many_tags"));
        List<String> mediaIds = normalizeStringList(request.getMediaIds(), 9, "too_many_media_ids");
        if (!mediaIds.isEmpty()) {
            int ownedCount = communityMediaStore.countOwnedMedia(queryUserId, mediaIds);
            if (ownedCount != mediaIds.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_media_ids");
            }
        }
        post.setMediaIds(mediaIds);
        post.setCreatedAtMs(System.currentTimeMillis());
        post.setCommentCount(0);
        post.setLikeCount(0);
        post.setLikedByMe(false);
        post.setFavoriteCount(0);
        post.setFavoritedByMe(false);
        communityPostStore.createPost(post);
        fillMediaPublicUrls(List.of(post));
        return post;
    }

    public List<CommunityPostDto> listPosts(
            String authenticatedUserId,
            String queryUserId,
            Integer limit
    ) {
        assertSameUser(authenticatedUserId, queryUserId);
        List<CommunityPostDto> posts = communityPostStore.listPosts(normalizeLimit(limit, 20, 100));
        fillInteractionFlags(queryUserId, posts);
        fillMediaPublicUrls(posts);
        return posts;
    }

    public CommunityPostPageDto listPostsPage(
            String authenticatedUserId,
            String queryUserId,
            String cursor,
            Integer limit,
            String sort
    ) {
        assertSameUser(authenticatedUserId, queryUserId);
        int pageSize = normalizeLimit(limit, 20, 50);
        String sortMode = sort == null || sort.isBlank() ? "latest" : sort.trim().toLowerCase(Locale.ROOT);
        List<CommunityPostDto> fetched;
        if ("latest".equals(sortMode)) {
            CommunityPostStore.PostCursor decodedCursor = decodeCursor(cursor);
            fetched = communityPostStore.listPostsByCursor(decodedCursor, pageSize + 1);
        } else if ("recommended".equals(sortMode)) {
            CommunityPostStore.RecommendedPostCursor recCursor = decodeRecommendedCursor(cursor);
            fetched = communityPostStore.listPostsByRecommendedCursor(recCursor, pageSize + 1);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_sort");
        }
        boolean hasMore = fetched.size() > pageSize;
        List<CommunityPostDto> items = hasMore ? new ArrayList<>(fetched.subList(0, pageSize)) : fetched;
        fillInteractionFlags(queryUserId, items);

        CommunityPostPageDto page = new CommunityPostPageDto();
        page.setItems(items);
        page.setHasMore(hasMore);
        if (hasMore && !items.isEmpty()) {
            CommunityPostDto tail = items.get(items.size() - 1);
            if ("recommended".equals(sortMode)) {
                page.setNextCursor(encodeRecommendedCursor(tail));
            } else {
                page.setNextCursor(encodeCursor(tail.getCreatedAtMs(), tail.getId()));
            }
        } else {
            page.setNextCursor(null);
        }
        fillMediaPublicUrls(items);
        return page;
    }

    public CommunityPostDto getPost(
            String authenticatedUserId,
            String queryUserId,
            String postId
    ) {
        assertSameUser(authenticatedUserId, queryUserId);
        String normalizedPostId = requireNonBlank(postId, "post_id_required");
        CommunityPostDto post = communityPostStore.findPostById(normalizedPostId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post_not_found"));
        fillInteractionFlags(queryUserId, List.of(post));
        fillMediaPublicUrls(List.of(post));
        return post;
    }

    public CommunityCommentDto createComment(
            String authenticatedUserId,
            String queryUserId,
            String postId,
            CreateCommunityCommentRequest request
    ) {
        assertSameUser(authenticatedUserId, queryUserId);
        String normalizedPostId = requireNonBlank(postId, "post_id_required");
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body_required");
        }
        if (communityPostStore.findPostById(normalizedPostId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post_not_found");
        }
        CommunityCommentDto comment = new CommunityCommentDto();
        comment.setId("cmt_" + UUID.randomUUID().toString().replace("-", ""));
        comment.setPostId(normalizedPostId);
        comment.setAuthorUserId(queryUserId);
        comment.setContent(requireNonBlank(request.getContent(), "content_required"));
        comment.setCreatedAtMs(System.currentTimeMillis());
        communityPostStore.createComment(comment);
        return comment;
    }

    public List<CommunityCommentDto> listComments(
            String authenticatedUserId,
            String queryUserId,
            String postId,
            Integer limit
    ) {
        assertSameUser(authenticatedUserId, queryUserId);
        String normalizedPostId = requireNonBlank(postId, "post_id_required");
        if (communityPostStore.findPostById(normalizedPostId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post_not_found");
        }
        return communityPostStore.listComments(normalizedPostId, normalizeLimit(limit, 50, 200));
    }

    public void likePost(
            String authenticatedUserId,
            String queryUserId,
            String postId
    ) {
        assertSameUser(authenticatedUserId, queryUserId);
        String normalizedPostId = requireNonBlank(postId, "post_id_required");
        if (communityPostStore.findPostById(normalizedPostId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post_not_found");
        }
        communityPostStore.likePost(normalizedPostId, queryUserId);
    }

    public void unlikePost(
            String authenticatedUserId,
            String queryUserId,
            String postId
    ) {
        assertSameUser(authenticatedUserId, queryUserId);
        String normalizedPostId = requireNonBlank(postId, "post_id_required");
        if (communityPostStore.findPostById(normalizedPostId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post_not_found");
        }
        communityPostStore.unlikePost(normalizedPostId, queryUserId);
    }

    public void favoritePost(
            String authenticatedUserId,
            String queryUserId,
            String postId
    ) {
        assertSameUser(authenticatedUserId, queryUserId);
        String normalizedPostId = requireNonBlank(postId, "post_id_required");
        if (communityPostStore.findPostById(normalizedPostId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post_not_found");
        }
        communityPostStore.favoritePost(normalizedPostId, queryUserId);
    }

    public void unfavoritePost(
            String authenticatedUserId,
            String queryUserId,
            String postId
    ) {
        assertSameUser(authenticatedUserId, queryUserId);
        String normalizedPostId = requireNonBlank(postId, "post_id_required");
        if (communityPostStore.findPostById(normalizedPostId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post_not_found");
        }
        communityPostStore.unfavoritePost(normalizedPostId, queryUserId);
    }

    public void deletePost(
            String authenticatedUserId,
            String queryUserId,
            String postId
    ) {
        assertSameUser(authenticatedUserId, queryUserId);
        String normalizedPostId = requireNonBlank(postId, "post_id_required");
        CommunityPostDto post = communityPostStore.findPostById(normalizedPostId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post_not_found"));
        if (!queryUserId.equals(post.getAuthorUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "post_delete_forbidden");
        }
        communityPostStore.softDeletePost(normalizedPostId, queryUserId);
    }

    public void deleteComment(
            String authenticatedUserId,
            String queryUserId,
            String postId,
            String commentId
    ) {
        assertSameUser(authenticatedUserId, queryUserId);
        String normalizedPostId = requireNonBlank(postId, "post_id_required");
        String normalizedCommentId = requireNonBlank(commentId, "comment_id_required");
        CommunityCommentDto comment = communityPostStore.findCommentById(normalizedPostId, normalizedCommentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "comment_not_found"));
        if (!queryUserId.equals(comment.getAuthorUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "comment_delete_forbidden");
        }
        communityPostStore.softDeleteComment(normalizedPostId, normalizedCommentId, queryUserId);
    }

    public CommunityPostPageDto adminListPostsPage(String operatorUserId, String cursor, Integer limit) {
        requireCommunityAdmin(operatorUserId);
        int pageSize = normalizeLimit(limit, 20, 200);
        CommunityPostStore.PostCursor decoded = decodeCursor(cursor);
        List<CommunityPostDto> fetched = communityPostStore.listPostsByCursorForAdmin(decoded, pageSize + 1);
        boolean hasMore = fetched.size() > pageSize;
        List<CommunityPostDto> items = hasMore ? new ArrayList<>(fetched.subList(0, pageSize)) : fetched;
        fillInteractionFlags(operatorUserId, items);
        CommunityPostPageDto page = new CommunityPostPageDto();
        page.setItems(items);
        page.setHasMore(hasMore);
        if (hasMore && !items.isEmpty()) {
            CommunityPostDto tail = items.get(items.size() - 1);
            page.setNextCursor(encodeCursor(tail.getCreatedAtMs(), tail.getId()));
        } else {
            page.setNextCursor(null);
        }
        fillMediaPublicUrls(items);
        return page;
    }

    public void adminSoftDeletePost(String operatorUserId, String postId) {
        requireCommunityAdmin(operatorUserId);
        String normalizedPostId = requireNonBlank(postId, "post_id_required");
        if (communityPostStore.findPostById(normalizedPostId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post_not_found");
        }
        communityPostStore.softDeletePost(normalizedPostId, operatorUserId);
        LOG.warn("community_moderation action=post_hide operatorUserId={} postId={}", operatorUserId, normalizedPostId);
    }

    public void adminSoftDeleteComment(String operatorUserId, String postId, String commentId) {
        requireCommunityAdmin(operatorUserId);
        String normalizedPostId = requireNonBlank(postId, "post_id_required");
        String normalizedCommentId = requireNonBlank(commentId, "comment_id_required");
        communityPostStore.findCommentById(normalizedPostId, normalizedCommentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "comment_not_found"));
        communityPostStore.softDeleteComment(normalizedPostId, normalizedCommentId, operatorUserId);
        LOG.warn(
                "community_moderation action=comment_hide operatorUserId={} postId={} commentId={}",
                operatorUserId,
                normalizedPostId,
                normalizedCommentId
        );
    }

    private void requireCommunityAdmin(String userId) {
        String raw = appProperties.getSecurity().getCommunityAdminUserIds();
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "admin_access_disabled");
        }
        Set<String> allowed = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (!allowed.contains(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "admin_forbidden");
        }
    }

    private void assertSameUser(String authenticatedUserId, String queryUserId) {
        if (queryUserId == null || queryUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId_required");
        }
        if (!authenticatedUserId.equals(queryUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "user_mismatch");
        }
    }

    private String requireNonBlank(String value, String errorCode) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
        }
        return value.trim();
    }

    private int normalizeLimit(Integer limit, int defaultLimit, int maxLimit) {
        if (limit == null) {
            return defaultLimit;
        }
        if (limit < 1 || limit > maxLimit) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_limit");
        }
        return limit;
    }

    private List<String> normalizeStringList(List<String> values, int maxSize, String overflowCode) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> dedup = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            dedup.add(value.trim());
        }
        if (dedup.size() > maxSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, overflowCode);
        }
        return new ArrayList<>(dedup);
    }

    private void fillInteractionFlags(String queryUserId, List<CommunityPostDto> posts) {
        if (posts.isEmpty()) {
            return;
        }
        List<String> postIds = new ArrayList<>(posts.size());
        for (CommunityPostDto post : posts) {
            postIds.add(post.getId());
            if (post.getLikeCount() == null) {
                post.setLikeCount(0);
            }
            if (post.getFavoriteCount() == null) {
                post.setFavoriteCount(0);
            }
            post.setLikedByMe(false);
            post.setFavoritedByMe(false);
        }
        Set<String> likedIds = communityPostStore.findLikedPostIds(queryUserId, postIds);
        Set<String> liked = likedIds == null ? Set.of() : new HashSet<>(likedIds);
        Set<String> favoritedIds = communityPostStore.findFavoritedPostIds(queryUserId, postIds);
        Set<String> favorited = favoritedIds == null ? Set.of() : new HashSet<>(favoritedIds);
        for (CommunityPostDto post : posts) {
            post.setLikedByMe(liked.contains(post.getId()));
            post.setFavoritedByMe(favorited.contains(post.getId()));
        }
    }

    private void fillMediaPublicUrls(List<CommunityPostDto> posts) {
        if (posts.isEmpty()) {
            return;
        }
        List<String> allIds = new ArrayList<>();
        for (CommunityPostDto post : posts) {
            List<String> ids = post.getMediaIds();
            if (ids == null) {
                continue;
            }
            for (String id : ids) {
                if (id != null && !id.isBlank()) {
                    allIds.add(id.trim());
                }
            }
        }
        if (allIds.isEmpty()) {
            for (CommunityPostDto post : posts) {
                post.setMediaUrls(List.of());
                post.setCoverMediaUrl(null);
            }
            return;
        }
        Map<String, CommunityMediaDocument> byId = communityMediaStore.findByIds(allIds);
        if (byId == null) {
            byId = Map.of();
        }
        for (CommunityPostDto post : posts) {
            List<String> ids = post.getMediaIds();
            if (ids == null || ids.isEmpty()) {
                post.setMediaUrls(List.of());
                post.setCoverMediaUrl(null);
                continue;
            }
            List<String> urls = new ArrayList<>(ids.size());
            String cover = null;
            for (String rawId : ids) {
                if (rawId == null || rawId.isBlank()) {
                    urls.add(null);
                    continue;
                }
                String id = rawId.trim();
                CommunityMediaDocument doc = byId.get(id);
                String url = mediaPublicUrlBuilder.publicUrlFor(doc);
                urls.add(url);
                if (cover == null && url != null) {
                    cover = url;
                }
            }
            post.setMediaUrls(urls);
            post.setCoverMediaUrl(cover);
        }
    }

    private CommunityPostStore.PostCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor.trim()), StandardCharsets.UTF_8);
            String[] parts = raw.split(":", 2);
            if (parts.length != 2 || parts[1].isBlank()) {
                throw new IllegalArgumentException("invalid");
            }
            long createdAtMs = Long.parseLong(parts[0]);
            return new CommunityPostStore.PostCursor(createdAtMs, parts[1]);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_cursor");
        }
    }

    private static int postHotness(CommunityPostDto post) {
        int likes = post.getLikeCount() == null ? 0 : post.getLikeCount();
        int comments = post.getCommentCount() == null ? 0 : post.getCommentCount();
        return likes * 2 + comments;
    }

    private CommunityPostStore.RecommendedPostCursor decodeRecommendedCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor.trim()), StandardCharsets.UTF_8);
            String[] parts = raw.split(":", 3);
            if (parts.length != 3 || parts[2].isBlank()) {
                throw new IllegalArgumentException("invalid");
            }
            int hotness = Integer.parseInt(parts[0]);
            long createdAtMs = Long.parseLong(parts[1]);
            return new CommunityPostStore.RecommendedPostCursor(hotness, createdAtMs, parts[2]);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_cursor");
        }
    }

    private String encodeRecommendedCursor(CommunityPostDto tail) {
        if (tail == null || tail.getId() == null || tail.getCreatedAtMs() == null) {
            return null;
        }
        int h = postHotness(tail);
        String raw = h + ":" + tail.getCreatedAtMs() + ":" + tail.getId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String encodeCursor(Long createdAtMs, String postId) {
        if (createdAtMs == null || postId == null || postId.isBlank()) {
            return null;
        }
        String raw = createdAtMs + ":" + postId;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
