package com.livemore.api.service;

import com.livemore.api.config.AppProperties;
import com.livemore.api.domain.RefreshTokenEntity;
import com.livemore.api.domain.UserAccount;
import com.livemore.api.security.JwtService;
import com.livemore.api.support.TokenHasher;
import com.livemore.api.web.dto.PhoneCodeVerifyRequest;
import com.livemore.api.web.dto.PhoneLoginRequest;
import com.livemore.api.web.dto.RefreshTokenRequest;
import com.livemore.api.web.dto.TestAccountLoginRequest;
import com.livemore.api.web.dto.WechatLoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthStore authStore;
    @Mock
    private JwtService jwtService;
    @Mock
    private LoginRateLimitService loginRateLimitService;
    @Mock
    private WechatOAuthClient wechatOAuthClient;
    @Mock
    private SmsCodeService smsCodeService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setSecret("dev-only-do-not-use-in-shared-or-production-env-min-32-chars");
        appProperties.getJwt().setAccessTokenSeconds(1800);
        appProperties.getRefreshToken().setTtlSeconds(2592000);
        appProperties.getAuth().setLegacyPhoneLoginEnabled(true);
        authService = new AuthService(
                authStore,
                jwtService,
                appProperties,
                loginRateLimitService,
                wechatOAuthClient,
                smsCodeService
        );
        lenient().when(authStore.upsertUser(any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void loginWithPhone_createsUserAndIssuesTokens() {
        when(authStore.findUserById("phone_13800138000")).thenReturn(Optional.empty());
        when(jwtService.createAccessToken("phone_13800138000")).thenReturn("access-token");

        PhoneLoginRequest request = new PhoneLoginRequest();
        request.setPhone("138 0013 8000");

        var response = authService.loginWithPhone(request, "127.0.0.1");

        assertEquals("phone_13800138000", response.getUserId());
        assertEquals("access-token", response.getAccessToken());
        assertFalse(response.getRefreshToken().isBlank());

        verify(authStore).upsertUser(any(UserAccount.class));
        verify(loginRateLimitService).checkAndConsume("13800138000", "127.0.0.1");
        verify(authStore).revokeAllRefreshTokens("phone_13800138000");
        verify(authStore, atLeastOnce()).upsertRefreshToken(any(RefreshTokenEntity.class));
    }

    @Test
    void loginWithPhone_revokesOldTokensAndWritesNewToken() {
        when(authStore.findUserById("phone_13800138000")).thenReturn(Optional.empty());
        when(jwtService.createAccessToken("phone_13800138000")).thenReturn("access-token");
        PhoneLoginRequest request = new PhoneLoginRequest();
        request.setPhone("13800138000");

        authService.loginWithPhone(request, "127.0.0.1");

        verify(authStore).upsertUser(any(UserAccount.class));
        verify(authStore).revokeAllRefreshTokens("phone_13800138000");
        verify(authStore).upsertRefreshToken(any(RefreshTokenEntity.class));
    }

    @Test
    void refresh_invalidToken_returnsUnauthorized() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setUserId("phone_13800138000");
        request.setRefreshToken("bad-token");

        when(authStore.findRefreshTokenByHashAndUserId(
                TokenHasher.sha256Hex("bad-token"),
                "phone_13800138000"
        )).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.refresh(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void refresh_validToken_revokesOldAndIssuesNew() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setUserId("phone_13800138000");
        request.setRefreshToken("refresh-old");

        RefreshTokenEntity existing = new RefreshTokenEntity();
        existing.setId("rt-1");
        existing.setUserId("phone_13800138000");
        existing.setTokenHash(TokenHasher.sha256Hex("refresh-old"));
        existing.setExpiresAt(Instant.now().plusSeconds(600));
        existing.setRevoked(false);

        when(authStore.findRefreshTokenByHashAndUserId(existing.getTokenHash(), "phone_13800138000"))
                .thenReturn(Optional.of(existing));
        when(jwtService.createAccessToken("phone_13800138000")).thenReturn("access-new");

        var response = authService.refresh(request);

        assertEquals("phone_13800138000", response.getUserId());
        assertEquals("access-new", response.getAccessToken());
        assertFalse(response.getRefreshToken().isBlank());

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(authStore, times(2)).upsertRefreshToken(captor.capture());
        assertEquals("rt-1", captor.getAllValues().get(0).getId());
        assertTrue(captor.getAllValues().get(0).isRevoked());
        verify(authStore).findRefreshTokenByHashAndUserId(eq(existing.getTokenHash()), eq("phone_13800138000"));
    }

    @Test
    void loginWithWechat_firstLogin_createsUserAndIdentity() {
        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("wx-code");
        WechatOAuthProfile profile = new WechatOAuthProfile();
        profile.setOpenId("openid-1");
        profile.setUnionId("unionid-1");
        profile.setNickname("wx-user");
        when(wechatOAuthClient.exchangeCode("wx-code")).thenReturn(profile);
        when(authStore.findUserByIdentity("wechat", "unionid-1")).thenReturn(Optional.empty());
        lenient().when(jwtService.createAccessToken(any())).thenReturn("access-token");

        var response = authService.loginWithWechat(request);

        assertFalse(response.getUserId().isBlank());
        verify(authStore).upsertUserIdentity(
                eq(response.getUserId()),
                eq("wechat"),
                eq("unionid-1"),
                eq("openid-1"),
                eq("wx-user")
        );
        verify(authStore).revokeAllRefreshTokens(response.getUserId());
    }

    @Test
    void loginWithWechat_withoutUnionId_fallsBackToOpenId() {
        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("wx-code");
        WechatOAuthProfile profile = new WechatOAuthProfile();
        profile.setOpenId("openid-only");
        profile.setNickname("wx-user");
        when(wechatOAuthClient.exchangeCode("wx-code")).thenReturn(profile);
        when(authStore.findUserByIdentity("wechat", "openid-only")).thenReturn(Optional.empty());
        when(jwtService.createAccessToken(any())).thenReturn("access-token");

        var response = authService.loginWithWechat(request);

        verify(authStore).findUserByIdentity("wechat", "openid-only");
        verify(authStore).upsertUserIdentity(
                eq(response.getUserId()),
                eq("wechat"),
                eq("openid-only"),
                eq("openid-only"),
                eq("wx-user")
        );
    }

    @Test
    void loginWithWechat_whenUpstreamUnavailable_propagates503() {
        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("wx-code");
        when(wechatOAuthClient.exchangeCode("wx-code"))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "wechat_upstream_failed"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.loginWithWechat(request));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    void loginWithPhoneCode_verifiesOtpAndIssuesTokens() {
        PhoneCodeVerifyRequest request = new PhoneCodeVerifyRequest();
        request.setPhone("13800138000");
        request.setCode("123456");
        when(authStore.findUserById("phone_13800138000")).thenReturn(Optional.empty());
        when(jwtService.createAccessToken("phone_13800138000")).thenReturn("access-token");

        var response = authService.loginWithPhoneCode(request, "127.0.0.1");

        assertEquals("phone_13800138000", response.getUserId());
        verify(smsCodeService).verifyCodeOrThrow("13800138000", "123456", "127.0.0.1");
        verify(authStore).revokeAllRefreshTokens("phone_13800138000");
    }

    @Test
    void loginWithPhone_whenLegacyDisabled_returnsGone() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setSecret("dev-only-do-not-use-in-shared-or-production-env-min-32-chars");
        appProperties.getAuth().setLegacyPhoneLoginEnabled(false);
        AuthService disabled = new AuthService(
                authStore,
                jwtService,
                appProperties,
                loginRateLimitService,
                wechatOAuthClient,
                smsCodeService
        );
        PhoneLoginRequest request = new PhoneLoginRequest();
        request.setPhone("13800138000");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> disabled.loginWithPhone(request, "127.0.0.1"));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }

    @Test
    void loginWithTestAccount_whenEnabledAndSecretValid_returnsTokens() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setSecret("dev-only-do-not-use-in-shared-or-production-env-min-32-chars");
        appProperties.getRefreshToken().setTtlSeconds(2592000);
        appProperties.getAuth().setTestAccountLoginEnabled(true);
        appProperties.getAuth().setTestAccountSecret("s3cr3t");
        AuthService service = new AuthService(
                authStore,
                jwtService,
                appProperties,
                loginRateLimitService,
                wechatOAuthClient,
                smsCodeService
        );
        when(authStore.findUserById("phone_13800138000")).thenReturn(Optional.empty());
        when(jwtService.createAccessToken("phone_13800138000")).thenReturn("access-token");
        TestAccountLoginRequest request = new TestAccountLoginRequest();
        request.setPhone("13800138000");

        var response = service.loginWithTestAccount(request, "s3cr3t");

        assertEquals("phone_13800138000", response.getUserId());
        assertEquals("access-token", response.getAccessToken());
    }

    @Test
    void loginWithTestAccount_whenSecretInvalid_returns401() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setSecret("dev-only-do-not-use-in-shared-or-production-env-min-32-chars");
        appProperties.getAuth().setTestAccountLoginEnabled(true);
        appProperties.getAuth().setTestAccountSecret("s3cr3t");
        AuthService service = new AuthService(
                authStore,
                jwtService,
                appProperties,
                loginRateLimitService,
                wechatOAuthClient,
                smsCodeService
        );
        TestAccountLoginRequest request = new TestAccountLoginRequest();
        request.setPhone("13800138000");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.loginWithTestAccount(request, "bad"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}
