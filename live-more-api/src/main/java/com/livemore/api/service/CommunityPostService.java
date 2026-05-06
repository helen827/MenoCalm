package com.livemore.api.service;

import com.livemore.api.web.dto.CommunityCommentDto;
import com.livemore.api.web.dto.CommunityPostPageDto;
import com.livemore.api.web.dto.CommunityPostDto;
import com.livemore.api.web.dto.CreateCommunityCommentRequest;
import com.livemore.api.web.dto.CreateCommunityPostRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

@Service
public class CommunityPostService {

    private final CommunityPostStore communityPostStore;
    private final CommunityMediaStore communityMediaStore;

    public CommunityPostService(CommunityPostStore communityPostStore, CommunityMediaStore communityMediaStore) {
        this.communityPostStore = communityPostStore;
        this.communityMediaStore = communityMediaStore;
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
        return posts;
    }

    public CommunityPostPageDto listPostsPage(
            String authenticatedUserId,
            String queryUserId,
            String cursor,
            Integer limit
    ) {
        assertSameUser(authenticatedUserId, queryUserId);
        int pageSize = normalizeLimit(limit, 20, 50);
        CommunityPostStore.PostCursor decodedCursor = decodeCursor(cursor);
        List<CommunityPostDto> fetched = communityPostStore.listPostsByCursor(decodedCursor, pageSize + 1);
        boolean hasMore = fetched.size() > pageSize;
        List<CommunityPostDto> items = hasMore ? new ArrayList<>(fetched.subList(0, pageSize)) : fetched;
        fillInteractionFlags(queryUserId, items);

        CommunityPostPageDto page = new CommunityPostPageDto();
        page.setItems(items);
        page.setHasMore(hasMore);
        if (hasMore && !items.isEmpty()) {
            CommunityPostDto tail = items.get(items.size() - 1);
            page.setNextCursor(encodeCursor(tail.getCreatedAtMs(), tail.getId()));
        } else {
            page.setNextCursor(null);
        }
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

    private String encodeCursor(Long createdAtMs, String postId) {
        if (createdAtMs == null || postId == null || postId.isBlank()) {
            return null;
        }
        String raw = createdAtMs + ":" + postId;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
