package com.livemore.api.web;

import com.livemore.api.service.CommunityMediaService;
import com.livemore.api.web.dto.MediaPresignRequest;
import com.livemore.api.web.dto.MediaPresignResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/community/media", produces = MediaType.APPLICATION_JSON_VALUE)
public class CommunityMediaController {

    private final CommunityMediaService communityMediaService;

    public CommunityMediaController(CommunityMediaService communityMediaService) {
        this.communityMediaService = communityMediaService;
    }

    @PostMapping(path = "/presign", consumes = MediaType.APPLICATION_JSON_VALUE)
    public MediaPresignResponse presign(
            @Valid @RequestBody MediaPresignRequest request,
            Authentication authentication
    ) {
        return communityMediaService.createPresign(authentication.getName(), request);
    }

    @PostMapping("/{mediaId}/complete")
    public void markUploadComplete(
            @PathVariable("mediaId") String mediaId,
            Authentication authentication
    ) {
        communityMediaService.markUploadComplete(authentication.getName(), mediaId);
    }
}
