package com.livemore.api.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateCommunityPostRequest {
    @NotBlank(message = "title_required")
    @Size(max = 80, message = "title_too_long")
    private String title;
    @NotBlank(message = "content_required")
    @Size(max = 5000, message = "content_too_long")
    private String content;
    private List<String> tags;
    private List<String> mediaIds;

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
}
