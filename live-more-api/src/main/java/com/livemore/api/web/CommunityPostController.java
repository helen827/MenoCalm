package com.livemore.api.web;

import com.livemore.api.service.CommunityPostService;
import com.livemore.api.web.dto.CommunityCommentDto;
import com.livemore.api.web.dto.CommunityPostPageDto;
import com.livemore.api.web.dto.CommunityPostDto;
import com.livemore.api.web.dto.CreateCommunityCommentRequest;
import com.livemore.api.web.dto.CreateCommunityPostRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/community/posts", produces = MediaType.APPLICATION_JSON_VALUE)
public class CommunityPostController {

    private final CommunityPostService communityPostService;

    public CommunityPostController(CommunityPostService communityPostService) {
        this.communityPostService = communityPostService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public CommunityPostDto createPost(
            @RequestParam("userId") String userId,
            @Valid @RequestBody CreateCommunityPostRequest request,
            Authentication authentication
    ) {
        return communityPostService.createPost(authentication.getName(), userId, request);
    }

    @GetMapping
    public List<CommunityPostDto> listPosts(
            @RequestParam("userId") String userId,
            @RequestParam(value = "limit", required = false) Integer limit,
            Authentication authentication
    ) {
        return communityPostService.listPosts(authentication.getName(), userId, limit);
    }

    @GetMapping("/page")
    public CommunityPostPageDto listPostsPage(
            @RequestParam("userId") String userId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false) Integer limit,
            Authentication authentication
    ) {
        return communityPostService.listPostsPage(authentication.getName(), userId, cursor, limit);
    }

    @GetMapping("/{postId}")
    public CommunityPostDto getPost(
            @RequestParam("userId") String userId,
            @PathVariable("postId") String postId,
            Authentication authentication
    ) {
        return communityPostService.getPost(authentication.getName(), userId, postId);
    }

    @PostMapping(path = "/{postId}/comments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CommunityCommentDto createComment(
            @RequestParam("userId") String userId,
            @PathVariable("postId") String postId,
            @Valid @RequestBody CreateCommunityCommentRequest request,
            Authentication authentication
    ) {
        return communityPostService.createComment(authentication.getName(), userId, postId, request);
    }

    @GetMapping("/{postId}/comments")
    public List<CommunityCommentDto> listComments(
            @RequestParam("userId") String userId,
            @PathVariable("postId") String postId,
            @RequestParam(value = "limit", required = false) Integer limit,
            Authentication authentication
    ) {
        return communityPostService.listComments(authentication.getName(), userId, postId, limit);
    }

    @PostMapping("/{postId}/like")
    public void likePost(
            @RequestParam("userId") String userId,
            @PathVariable("postId") String postId,
            Authentication authentication
    ) {
        communityPostService.likePost(authentication.getName(), userId, postId);
    }

    @DeleteMapping("/{postId}/like")
    public void unlikePost(
            @RequestParam("userId") String userId,
            @PathVariable("postId") String postId,
            Authentication authentication
    ) {
        communityPostService.unlikePost(authentication.getName(), userId, postId);
    }

    @PostMapping("/{postId}/favorite")
    public void favoritePost(
            @RequestParam("userId") String userId,
            @PathVariable("postId") String postId,
            Authentication authentication
    ) {
        communityPostService.favoritePost(authentication.getName(), userId, postId);
    }

    @DeleteMapping("/{postId}/favorite")
    public void unfavoritePost(
            @RequestParam("userId") String userId,
            @PathVariable("postId") String postId,
            Authentication authentication
    ) {
        communityPostService.unfavoritePost(authentication.getName(), userId, postId);
    }

    @DeleteMapping("/{postId}")
    public void deletePost(
            @RequestParam("userId") String userId,
            @PathVariable("postId") String postId,
            Authentication authentication
    ) {
        communityPostService.deletePost(authentication.getName(), userId, postId);
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public void deleteComment(
            @RequestParam("userId") String userId,
            @PathVariable("postId") String postId,
            @PathVariable("commentId") String commentId,
            Authentication authentication
    ) {
        communityPostService.deleteComment(authentication.getName(), userId, postId, commentId);
    }
}
