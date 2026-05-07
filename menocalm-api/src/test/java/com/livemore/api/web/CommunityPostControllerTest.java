package com.livemore.api.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livemore.api.config.RequestIdFilter;
import com.livemore.api.service.CommunityPostService;
import com.livemore.api.web.dto.CommunityPostDto;
import com.livemore.api.web.dto.CommunityPostPageDto;
import com.livemore.api.web.dto.CreateCommunityPostRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CommunityPostControllerTest {

    @Mock
    private CommunityPostService communityPostService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CommunityPostController(communityPostService))
                .setControllerAdvice(new RestExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void createPost_success_returnsPostPayload() throws Exception {
        CreateCommunityPostRequest request = new CreateCommunityPostRequest();
        request.setTitle("标题");
        request.setContent("内容");
        CommunityPostDto response = new CommunityPostDto();
        response.setId("post_1");
        response.setTitle("标题");
        response.setContent("内容");
        when(communityPostService.createPost(
                eq("u1"),
                eq("u1"),
                argThat(r -> r != null && "标题".equals(r.getTitle()) && "内容".equals(r.getContent()))
        )).thenReturn(response);

        mockMvc.perform(post("/api/v1/community/posts")
                        .principal(new TestingAuthenticationToken("u1", null))
                        .param("userId", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("post_1"))
                .andExpect(jsonPath("$.title").value("标题"));
    }

    @Test
    void createPost_invalidBody_returns400() throws Exception {
        CreateCommunityPostRequest request = new CreateCommunityPostRequest();
        request.setTitle("");
        request.setContent("");

        mockMvc.perform(post("/api/v1/community/posts")
                        .principal(new TestingAuthenticationToken("u1", null))
                        .param("userId", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void unlikePost_success_returns200() throws Exception {
        mockMvc.perform(delete("/api/v1/community/posts/p1/like")
                        .principal(new TestingAuthenticationToken("u1", null))
                        .param("userId", "u1"))
                .andExpect(status().isOk());
        verify(communityPostService).unlikePost("u1", "u1", "p1");
    }

    @Test
    void unfavoritePost_success_returns200() throws Exception {
        mockMvc.perform(delete("/api/v1/community/posts/p1/favorite")
                        .principal(new TestingAuthenticationToken("u1", null))
                        .param("userId", "u1"))
                .andExpect(status().isOk());
        verify(communityPostService).unfavoritePost("u1", "u1", "p1");
    }

    @Test
    void listPostsPage_success_returnsPayload() throws Exception {
        CommunityPostPageDto page = new CommunityPostPageDto();
        page.setItems(java.util.List.of());
        page.setHasMore(true);
        page.setNextCursor("abc");
        when(communityPostService.listPostsPage(eq("u1"), eq("u1"), isNull(), eq(20), isNull())).thenReturn(page);

        mockMvc.perform(get("/api/v1/community/posts/page")
                        .principal(new TestingAuthenticationToken("u1", null))
                        .param("userId", "u1")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.nextCursor").value("abc"));
    }

    @Test
    void deletePost_success_returns200() throws Exception {
        mockMvc.perform(delete("/api/v1/community/posts/p1")
                        .principal(new TestingAuthenticationToken("u1", null))
                        .param("userId", "u1"))
                .andExpect(status().isOk());
        verify(communityPostService).deletePost("u1", "u1", "p1");
    }

    @Test
    void deleteComment_success_returns200() throws Exception {
        mockMvc.perform(delete("/api/v1/community/posts/p1/comments/c1")
                        .principal(new TestingAuthenticationToken("u1", null))
                        .param("userId", "u1"))
                .andExpect(status().isOk());
        verify(communityPostService).deleteComment("u1", "u1", "p1", "c1");
    }
}
