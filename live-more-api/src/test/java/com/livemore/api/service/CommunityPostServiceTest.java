package com.livemore.api.service;

import com.livemore.api.web.dto.CommunityCommentDto;
import com.livemore.api.web.dto.CommunityPostDto;
import com.livemore.api.web.dto.CreateCommunityCommentRequest;
import com.livemore.api.web.dto.CreateCommunityPostRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityPostServiceTest {

    @Mock
    private CommunityPostStore communityPostStore;
    @Mock
    private CommunityMediaStore communityMediaStore;

    private CommunityPostService service() {
        return new CommunityPostService(communityPostStore, communityMediaStore);
    }

    @Test
    void createPost_userMismatch_returns403() {
        CommunityPostService service = service();
        CreateCommunityPostRequest request = new CreateCommunityPostRequest();
        request.setTitle("t");
        request.setContent("c");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.createPost("u1", "u2", request)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void createPost_withInvalidMediaIds_returns400() {
        CommunityPostService service = service();
        CreateCommunityPostRequest request = new CreateCommunityPostRequest();
        request.setTitle("标题");
        request.setContent("内容");
        request.setMediaIds(List.of("m1", "m2"));
        when(communityMediaStore.countOwnedMedia("u1", List.of("m1", "m2"))).thenReturn(1);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.createPost("u1", "u1", request)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void createPost_success_persistsAndReturnsPost() {
        CommunityPostService service = service();
        CreateCommunityPostRequest request = new CreateCommunityPostRequest();
        request.setTitle(" 标题 ");
        request.setContent(" 内容 ");
        request.setTags(List.of("更年期", " 睡眠 ", "更年期"));
        request.setMediaIds(List.of("m1"));
        when(communityMediaStore.countOwnedMedia("u1", List.of("m1"))).thenReturn(1);

        CommunityPostDto created = service.createPost("u1", "u1", request);

        assertNotNull(created.getId());
        assertEquals("标题", created.getTitle());
        assertEquals(List.of("更年期", "睡眠"), created.getTags());
        ArgumentCaptor<CommunityPostDto> captor = ArgumentCaptor.forClass(CommunityPostDto.class);
        verify(communityPostStore).createPost(captor.capture());
        assertEquals("u1", captor.getValue().getAuthorUserId());
    }

    @Test
    void createComment_postNotFound_returns404() {
        CommunityPostService service = service();
        when(communityPostStore.findPostById("p1")).thenReturn(Optional.empty());
        CreateCommunityCommentRequest request = new CreateCommunityCommentRequest();
        request.setContent("加油");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.createComment("u1", "u1", "p1", request)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void listComments_postExists_delegatesToStore() {
        CommunityPostService service = service();
        CommunityPostDto post = new CommunityPostDto();
        post.setId("p1");
        when(communityPostStore.findPostById("p1")).thenReturn(Optional.of(post));
        when(communityPostStore.listComments("p1", 50)).thenReturn(List.of());

        service.listComments("u1", "u1", "p1", null);

        verify(communityPostStore).listComments("p1", 50);
        verify(communityMediaStore, org.mockito.Mockito.never()).countOwnedMedia(any(), any());
    }

    @Test
    void listPosts_fillsLikedByMeFlag() {
        CommunityPostService service = service();
        CommunityPostDto post = new CommunityPostDto();
        post.setId("p1");
        post.setLikeCount(3);
        when(communityPostStore.listPosts(20)).thenReturn(List.of(post));
        when(communityPostStore.findLikedPostIds("u1", List.of("p1"))).thenReturn(Set.of("p1"));

        List<CommunityPostDto> result = service.listPosts("u1", "u1", null);

        assertEquals(1, result.size());
        assertEquals(true, result.get(0).getLikedByMe());
        assertEquals(3, result.get(0).getLikeCount());
    }

    @Test
    void likePost_postExists_delegatesToStore() {
        CommunityPostService service = service();
        CommunityPostDto post = new CommunityPostDto();
        post.setId("p1");
        when(communityPostStore.findPostById("p1")).thenReturn(Optional.of(post));

        service.likePost("u1", "u1", "p1");

        verify(communityPostStore).likePost("p1", "u1");
    }

    @Test
    void listPosts_fillsFavoritedByMeFlag() {
        CommunityPostService service = service();
        CommunityPostDto post = new CommunityPostDto();
        post.setId("p2");
        post.setFavoriteCount(2);
        when(communityPostStore.listPosts(20)).thenReturn(List.of(post));
        when(communityPostStore.findLikedPostIds("u1", List.of("p2"))).thenReturn(Set.of());
        when(communityPostStore.findFavoritedPostIds("u1", List.of("p2"))).thenReturn(Set.of("p2"));

        List<CommunityPostDto> result = service.listPosts("u1", "u1", null);

        assertEquals(1, result.size());
        assertEquals(true, result.get(0).getFavoritedByMe());
        assertEquals(2, result.get(0).getFavoriteCount());
    }

    @Test
    void listPostsPage_invalidCursor_returns400() {
        CommunityPostService service = service();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.listPostsPage("u1", "u1", "bad_cursor", 20)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void listPostsPage_hasMore_returnsNextCursor() {
        CommunityPostService service = service();
        CommunityPostDto p1 = new CommunityPostDto();
        p1.setId("p1");
        p1.setCreatedAtMs(100L);
        CommunityPostDto p2 = new CommunityPostDto();
        p2.setId("p2");
        p2.setCreatedAtMs(90L);
        when(communityPostStore.listPostsByCursor(null, 2)).thenReturn(List.of(p1, p2));
        when(communityPostStore.findLikedPostIds("u1", List.of("p1"))).thenReturn(Set.of());
        when(communityPostStore.findFavoritedPostIds("u1", List.of("p1"))).thenReturn(Set.of());

        var page = service.listPostsPage("u1", "u1", null, 1);

        assertEquals(true, page.isHasMore());
        assertEquals(1, page.getItems().size());
        assertNotNull(page.getNextCursor());
    }

    @Test
    void deletePost_notAuthor_returns403() {
        CommunityPostService service = service();
        CommunityPostDto post = new CommunityPostDto();
        post.setId("p1");
        post.setAuthorUserId("u2");
        when(communityPostStore.findPostById("p1")).thenReturn(Optional.of(post));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.deletePost("u1", "u1", "p1")
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void deleteComment_author_delegatesToStore() {
        CommunityPostService service = service();
        CommunityCommentDto comment = new CommunityCommentDto();
        comment.setId("c1");
        comment.setPostId("p1");
        comment.setAuthorUserId("u1");
        when(communityPostStore.findCommentById("p1", "c1")).thenReturn(Optional.of(comment));

        service.deleteComment("u1", "u1", "p1", "c1");

        verify(communityPostStore).softDeleteComment("p1", "c1", "u1");
    }
}
