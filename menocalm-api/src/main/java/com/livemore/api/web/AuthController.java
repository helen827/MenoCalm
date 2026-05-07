package com.livemore.api.web;

import com.livemore.api.service.AuthService;
import com.livemore.api.web.dto.AuthTokenResponseDto;
import com.livemore.api.web.dto.PhoneCodeSendRequest;
import com.livemore.api.web.dto.PhoneCodeSendResponse;
import com.livemore.api.web.dto.PhoneCodeVerifyRequest;
import com.livemore.api.web.dto.PhoneLoginRequest;
import com.livemore.api.web.dto.RefreshTokenRequest;
import com.livemore.api.web.dto.TestAccountLoginRequest;
import com.livemore.api.web.dto.WechatLoginRequest;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping(path = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(path = "/phone/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuthTokenResponseDto login(@Valid @RequestBody PhoneLoginRequest request, HttpServletRequest httpRequest) {
        return authService.loginWithPhone(request, extractClientIp(httpRequest));
    }

    @PostMapping(path = "/phone/code/send", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PhoneCodeSendResponse sendPhoneCode(@Valid @RequestBody PhoneCodeSendRequest request, HttpServletRequest httpRequest) {
        return authService.sendPhoneCode(request, extractClientIp(httpRequest));
    }

    @PostMapping(path = "/phone/code/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuthTokenResponseDto verifyPhoneCode(@Valid @RequestBody PhoneCodeVerifyRequest request, HttpServletRequest httpRequest) {
        return authService.loginWithPhoneCode(request, extractClientIp(httpRequest));
    }

    @PostMapping(path = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuthTokenResponseDto refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping(path = "/wechat/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuthTokenResponseDto wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        return authService.loginWithWechat(request);
    }

    @PostMapping(path = "/test-account/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuthTokenResponseDto testAccountLogin(
            @Valid @RequestBody TestAccountLoginRequest request,
            @RequestHeader(value = "X-Test-Account-Secret", required = false) String testSecret
    ) {
        return authService.loginWithTestAccount(request, testSecret);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
