package com.livemore.api.web.dto;

import java.util.List;

public class CommunityPostDto {
    private String id;
    private String authorUserId;
    private String title;
    private String content;
    private List<String> tags;
    private List<String> mediaIds;
    /** First public image/video URL for feed cards; null if none. */
    private String coverMediaUrl;
    /** Parallel to {@link #mediaIds}; null entries mean not exposable. */
    private List<String> mediaUrls;
    private Long createdAtMs;
    private Integer commentCount;
    private Integer likeCount;
    private Boolean likedByMe;
    private Integer favoriteCount;
    private Boolean favoritedByMe;
    /** Set only for admin moderation APIs when the post is soft-deleted. */
    private Boolean deleted;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthorUserId() {
        return authorUserId;
    }

    public void setAuthorUserId(String authorUserId) {
        this.authorUserId = authorUserId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getMediaIds() {
        return mediaIds;
    }

    public void setMediaIds(List<String> mediaIds) {
        this.mediaIds = mediaIds;
    }

    public String getCoverMediaUrl() {
        return coverMediaUrl;
    }

    public void setCoverMediaUrl(String coverMediaUrl) {
        this.coverMediaUrl = coverMediaUrl;
    }

    public List<String> getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    public Long getCreatedAtMs() {
        return createdAtMs;
    }

    public void setCreatedAtMs(Long createdAtMs) {
        this.createdAtMs = createdAtMs;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Boolean getLikedByMe() {
        return likedByMe;
    }

    public void setLikedByMe(Boolean likedByMe) {
        this.likedByMe = likedByMe;
    }

    public Integer getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(Integer favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public Boolean getFavoritedByMe() {
        return favoritedByMe;
    }

    public void setFavoritedByMe(Boolean favoritedByMe) {
        this.favoritedByMe = favoritedByMe;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
