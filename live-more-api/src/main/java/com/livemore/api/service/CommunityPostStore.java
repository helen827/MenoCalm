package com.livemore.api.service;

import com.livemore.api.web.dto.CommunityCommentDto;
import com.livemore.api.web.dto.CommunityPostDto;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CommunityPostStore {
    final class PostCursor {
        private final long createdAtMs;
        private final String postId;

        public PostCursor(long createdAtMs, String postId) {
            this.createdAtMs = createdAtMs;
            this.postId = postId;
        }

        public long getCreatedAtMs() {
            return createdAtMs;
        }

        public String getPostId() {
            return postId;
        }
    }

    void createPost(CommunityPostDto post);

    List<CommunityPostDto> listPosts(int limit);

    Optional<CommunityPostDto> findPostById(String postId);

    void createComment(CommunityCommentDto comment);

    List<CommunityCommentDto> listComments(String postId, int limit);

    Optional<CommunityCommentDto> findCommentById(String postId, String commentId);

    void likePost(String postId, String userId);

    void unlikePost(String postId, String userId);

    Set<String> findLikedPostIds(String userId, List<String> postIds);

    void favoritePost(String postId, String userId);

    void unfavoritePost(String postId, String userId);

    Set<String> findFavoritedPostIds(String userId, List<String> postIds);

    List<CommunityPostDto> listPostsByCursor(PostCursor cursor, int limit);

    void softDeletePost(String postId, String operatorUserId);

    void softDeleteComment(String postId, String commentId, String operatorUserId);
}
