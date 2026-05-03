package com.livemore.api.support;

import java.security.SecureRandom;
import java.util.Base64;

public final class SecureTokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SecureTokenGenerator() {
    }

    /**
     * URL-safe opaque refresh token (256 bits entropy).
     */
    public static String nextRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
