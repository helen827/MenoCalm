package com.livemore.api.security;

import com.livemore.api.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final AppProperties appProperties;
    private final SecretKey secretKey;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.secretKey = Keys.hmacShaKeyFor(appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String userId) {
        Instant now = Instant.now();
        long ttl = appProperties.getJwt().getAccessTokenSeconds();
        return Jwts.builder()
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttl)))
                .signWith(secretKey)
                .compact();
    }

    public String parseUserId(String bearerToken) {
        String token = stripBearer(bearerToken);
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public void assertValid(String bearerToken) {
        String token = stripBearer(bearerToken);
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Invalid access token", e);
        }
    }

    private static String stripBearer(String headerValue) {
        if (headerValue == null || !headerValue.startsWith("Bearer ")) {
            throw new JwtException("Missing or invalid Authorization header");
        }
        return headerValue.substring(7).trim();
    }
}
