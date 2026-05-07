package com.livemore.api.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCommunityCommentRequest {
    @NotBlank(message = "content_required")
    @Size(max = 1000, message = "content_too_long")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
