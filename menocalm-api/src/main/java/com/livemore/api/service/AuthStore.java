package com.livemore.api.service;

import com.livemore.api.domain.RefreshTokenEntity;
import com.livemore.api.domain.UserAccount;

import java.util.Optional;

public interface AuthStore {
    Optional<UserAccount> findUserById(String userId);
    Optional<UserAccount> findUserByIdentity(String provider, String providerUserId);

    UserAccount upsertUser(UserAccount user);
    void upsertUserIdentity(String userId, String provider, String providerUserId, String providerRawId, String displayName);

    void revokeAllRefreshTokens(String userId);

    Optional<RefreshTokenEntity> findRefreshTokenByHashAndUserId(String tokenHash, String userId);

    void upsertRefreshToken(RefreshTokenEntity token);
}
