package com.livemore.api.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livemore.api.config.RequestIdFilter;
import com.livemore.api.service.AuthService;
import com.livemore.api.web.dto.AdminPanelLoginRequest;
import com.livemore.api.web.dto.AuthTokenResponseDto;
import com.livemore.api.web.dto.PhoneCodeSendRequest;
import com.livemore.api.web.dto.PhoneCodeSendResponse;
import com.livemore.api.web.dto.PhoneCodeVerifyRequest;
import com.livemore.api.web.dto.TestAccountLoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new RestExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void sendPhoneCode_success_returns200AndPayload() throws Exception {
        PhoneCodeSendRequest request = new PhoneCodeSendRequest();
        request.setPhone("13800138000");
        when(authService.sendPhoneCode(
                argThat(r -> r != null && "13800138000".equals(r.getPhone())),
                eq("1.2.3.4")
        ))
                .thenReturn(new PhoneCodeSendResponse(true, 300, 60));

        mockMvc.perform(post("/api/v1/auth/phone/code/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "1.2.3.4, 10.0.0.1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(true))
                .andExpect(jsonPath("$.expiresInSeconds").value(300))
                .andExpect(jsonPath("$.resendAfterSeconds").value(60));

        verify(authService).sendPhoneCode(
                argThat(r -> r != null && "13800138000".equals(r.getPhone())),
                eq("1.2.3.4")
        );
    }

    @Test
    void verifyPhoneCode_success_returnsTokenResponse() throws Exception {
        PhoneCodeVerifyRequest request = new PhoneCodeVerifyRequest();
        request.setPhone("13800138000");
        request.setCode("123456");
        when(authService.loginWithPhoneCode(
                argThat(r -> r != null
                        && "13800138000".equals(r.getPhone())
                        && "123456".equals(r.getCode())),
                eq("127.0.0.1")
        ))
                .thenReturn(new AuthTokenResponseDto("access", "refresh", 1800, "phone_13800138000"));

        mockMvc.perform(post("/api/v1/auth/phone/code/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.userID").value("phone_13800138000"));
    }

    @Test
    void sendPhoneCode_invalidBody_returns400() throws Exception {
        PhoneCodeSendRequest request = new PhoneCodeSendRequest();
        request.setPhone("");

        mockMvc.perform(post("/api/v1/auth/phone/code/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void verifyPhoneCode_invalidSmsCode_returns401WithMessage() throws Exception {
        PhoneCodeVerifyRequest request = new PhoneCodeVerifyRequest();
        request.setPhone("13800138000");
        request.setCode("000000");
        when(authService.loginWithPhoneCode(
                argThat(r -> r != null
                        && "13800138000".equals(r.getPhone())
                        && "000000".equals(r.getCode())),
                eq("127.0.0.1")
        ))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "sms_code_invalid"));

        mockMvc.perform(post("/api/v1/auth/phone/code/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("sms_code_invalid"));
    }

    @Test
    void testAccountLogin_success_returnsTokenResponse() throws Exception {
        TestAccountLoginRequest request = new TestAccountLoginRequest();
        request.setPhone("15121150684");
        when(authService.loginWithTestAccount(
                argThat(r -> r != null && "15121150684".equals(r.getPhone())),
                eq("secret-1")
        )).thenReturn(new AuthTokenResponseDto("access", "refresh", 1800, "phone_15121150684"));

        mockMvc.perform(post("/api/v1/auth/test-account/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Test-Account-Secret", "secret-1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.userID").value("phone_15121150684"));
    }

    @Test
    void adminPanelLogin_success_returnsTokenResponse() throws Exception {
        AdminPanelLoginRequest request = new AdminPanelLoginRequest();
        request.setPhone("15121150684");
        request.setPassword("secret-pw");
        when(authService.loginWithAdminPanel(
                argThat(r -> r != null
                        && "15121150684".equals(r.getPhone())
                        && "secret-pw".equals(r.getPassword())),
                eq("10.0.0.2")
        )).thenReturn(new AuthTokenResponseDto("access", "refresh", 1800, "phone_15121150684"));

        mockMvc.perform(post("/api/v1/auth/admin-panel/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "10.0.0.2, 192.168.1.1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.userID").value("phone_15121150684"));
    }
}
