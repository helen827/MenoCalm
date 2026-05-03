package com.livemore.api.service;

import com.livemore.api.config.AppProperties;
import com.livemore.api.domain.RefreshTokenEntity;
import com.livemore.api.domain.UserAccount;
import com.livemore.api.security.JwtService;
import com.livemore.api.support.PhoneNormalizer;
import com.livemore.api.support.SecureTokenGenerator;
import com.livemore.api.support.TokenHasher;
import com.livemore.api.web.dto.AuthTokenResponseDto;
import com.livemore.api.web.dto.PhoneCodeSendRequest;
import com.livemore.api.web.dto.PhoneCodeSendResponse;
import com.livemore.api.web.dto.PhoneCodeVerifyRequest;
import com.livemore.api.web.dto.PhoneLoginRequest;
import com.livemore.api.web.dto.RefreshTokenRequest;
import com.livemore.api.web.dto.WechatLoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthStore authStore;
    private final JwtService jwtService;
    private final AppProperties appProperties;
    private final LoginRateLimitService loginRateLimitService;
    private final WechatOAuthClient wechatOAuthClient;
    private final SmsCodeService smsCodeService;

    public AuthService(
            AuthStore authStore,
            JwtService jwtService,
            AppProperties appProperties,
            LoginRateLimitService loginRateLimitService,
            WechatOAuthClient wechatOAuthClient,
            SmsCodeService smsCodeService
    ) {
        this.authStore = authStore;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
        this.loginRateLimitService = loginRateLimitService;
        this.wechatOAuthClient = wechatOAuthClient;
        this.smsCodeService = smsCodeService;
    }

    public AuthTokenResponseDto loginWithPhone(PhoneLoginRequest request, String clientIp) {
        String digits = PhoneNormalizer.normalizeCnMobile(request.getPhone())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_phone"));
        loginRateLimitService.checkAndConsume(digits, clientIp);
        return loginByPhoneDigits(digits);
    }

    public PhoneCodeSendResponse sendPhoneCode(PhoneCodeSendRequest request, String clientIp) {
        String digits = PhoneNormalizer.normalizeCnMobile(request.getPhone())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_phone"));
        return smsCodeService.sendLoginCode(digits, clientIp);
    }

    public AuthTokenResponseDto loginWithPhoneCode(PhoneCodeVerifyRequest request, String clientIp) {
        String digits = PhoneNormalizer.normalizeCnMobile(request.getPhone())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_phone"));
        loginRateLimitService.checkAndConsume(digits, clientIp);
        smsCodeService.verifyCodeOrThrow(digits, request.getCode(), clientIp);
        return loginByPhoneDigits(digits);
    }

    private AuthTokenResponseDto loginByPhoneDigits(String digits) {
        String userId = UserAccount.userIdFromPhoneDigits(digits);
        Instant now = Instant.now();
        UserAccount user = authStore.findUserById(userId).orElse(null);
        if (user == null) {
            user = authStore.upsertUser(new UserAccount(userId, digits, now, now));
        } else {
            user.setUpdatedAt(now);
            user = authStore.upsertUser(user);
        }

        authStore.revokeAllRefreshTokens(userId);
        return issueTokens(userId);
    }

    public AuthTokenResponseDto loginWithWechat(WechatLoginRequest request) {
        WechatOAuthProfile profile = wechatOAuthClient.exchangeCode(request.getCode());
        String providerUserId = profile.getUnionId() != null ? profile.getUnionId() : profile.getOpenId();
        UserAccount user = authStore.findUserByIdentity("wechat", providerUserId).orElse(null);
        Instant now = Instant.now();
        if (user == null) {
            String userId = "wx_" + TokenHasher.sha256Hex(providerUserId).substring(0, 24);
            user = authStore.upsertUser(new UserAccount(userId, null, now, now));
        } else {
            user.setUpdatedAt(now);
            user = authStore.upsertUser(user);
        }
        authStore.upsertUserIdentity(
                user.getId(),
                "wechat",
                providerUserId,
                profile.getOpenId(),
                profile.getNickname()
        );
        authStore.revokeAllRefreshTokens(user.getId());
        return issueTokens(user.getId());
    }

    public AuthTokenResponseDto refresh(RefreshTokenRequest request) {
        String hash = TokenHasher.sha256Hex(request.getRefreshToken());
        RefreshTokenEntity entity = authStore
                .findRefreshTokenByHashAndUserId(hash, request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_refresh_token"));

        if (entity.isRevoked() || entity.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh_token_expired");
        }

        entity.setRevoked(true);
        authStore.upsertRefreshToken(entity);

        return issueTokens(request.getUserId());
    }

    private AuthTokenResponseDto issueTokens(String userId) {
        String access = jwtService.createAccessToken(userId);
        long accessTtl = appProperties.getJwt().getAccessTokenSeconds();

        String rawRefresh = SecureTokenGenerator.nextRefreshToken();
        String refreshHash = TokenHasher.sha256Hex(rawRefresh);
        long refreshTtlSec = appProperties.getRefreshToken().getTtlSeconds();

        RefreshTokenEntity stored = new RefreshTokenEntity();
        stored.setId(UUID.randomUUID().toString());
        stored.setTokenHash(refreshHash);
        stored.setUserId(userId);
        stored.setExpiresAt(Instant.now().plusSeconds(refreshTtlSec));
        stored.setRevoked(false);
        stored.setCreatedAt(Instant.now());
        authStore.upsertRefreshToken(stored);

        return new AuthTokenResponseDto(access, rawRefresh, accessTtl, userId);
    }
}
