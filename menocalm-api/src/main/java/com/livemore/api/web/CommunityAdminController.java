package com.livemore.api.web;

import com.livemore.api.service.CommunityPostService;
import com.livemore.api.web.dto.CommunityPostPageDto;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/admin/community", produces = MediaType.APPLICATION_JSON_VALUE)
public class CommunityAdminController {

    private final CommunityPostService communityPostService;

    public CommunityAdminController(CommunityPostService communityPostService) {
        this.communityPostService = communityPostService;
    }

    @GetMapping("/posts/page")
    public CommunityPostPageDto listPostsPage(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false) Integer limit,
            Authentication authentication
    ) {
        return communityPostService.adminListPostsPage(authentication.getName(), cursor, limit);
    }

    @DeleteMapping("/posts/{postId}")
    public void hidePost(@PathVariable("postId") String postId, Authentication authentication) {
        communityPostService.adminSoftDeletePost(authentication.getName(), postId);
    }

    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public void hideComment(
            @PathVariable("postId") String postId,
            @PathVariable("commentId") String commentId,
            Authentication authentication
    ) {
        communityPostService.adminSoftDeleteComment(authentication.getName(), postId, commentId);
    }
}
