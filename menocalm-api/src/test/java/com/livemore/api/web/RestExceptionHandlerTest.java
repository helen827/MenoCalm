package com.livemore.api.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livemore.api.config.RequestIdFilter;
import com.livemore.api.web.dto.PhoneLoginRequest;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestExceptionHandlerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new RestExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void statusException_returnsStructuredError() throws Exception {
        mockMvc.perform(post("/test/status").header("X-Request-Id", "rid-1"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Request-Id", "rid-1"))
                .andExpect(jsonPath("$.code").value("forbidden"))
                .andExpect(jsonPath("$.message").value("user_mismatch"))
                .andExpect(jsonPath("$.requestId").value("rid-1"))
                .andExpect(jsonPath("$.path").value("/test/status"))
                .andExpect(jsonPath("$.timestamp").isNumber());
    }

    @Test
    void validationException_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PhoneLoginRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.path").value("/test/validate"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void unexpectedException_returnsInternalServerError() throws Exception {
        mockMvc.perform(post("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("internal_server_error"))
                .andExpect(jsonPath("$.message").value("internal_server_error"));
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {
        @PostMapping("/status")
        void statusError() {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "user_mismatch");
        }

        @PostMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("boom");
        }

        @PostMapping("/validate")
        void validate(@Valid @RequestBody PhoneLoginRequest request) {
        }
    }
}
