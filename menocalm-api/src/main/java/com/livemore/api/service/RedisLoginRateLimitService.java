package com.livemore.api.service;

import com.livemore.api.config.AppProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Service
public class RedisLoginRateLimitService implements LoginRateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;

    public RedisLoginRateLimitService(StringRedisTemplate redisTemplate, AppProperties appProperties) {
        this.redisTemplate = redisTemplate;
        this.appProperties = appProperties;
    }

    @Override
    public void checkAndConsume(String phoneDigits, String clientIp) {
        AppProperties.LoginRateLimit cfg = appProperties.getAuth().getLoginRateLimit();
        Duration window = Duration.ofSeconds(cfg.getWindowSeconds());
        String phoneKey = "ratelimit:login:phone:" + phoneDigits;
        long phoneCount = incrementWithExpiry(phoneKey, window);
        if (phoneCount > cfg.getPerPhoneMax()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "login_rate_limit_phone");
        }

        String normalizedIp = (clientIp == null || clientIp.isBlank()) ? "unknown" : clientIp.trim();
        String ipKey = "ratelimit:login:ip:" + normalizedIp;
        long ipCount = incrementWithExpiry(ipKey, window);
        if (ipCount > cfg.getPerIpMax()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "login_rate_limit_ip");
        }
    }

    private long incrementWithExpiry(String key, Duration ttl) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "redis_unavailable");
            }
            if (count == 1) {
                redisTemplate.expire(key, ttl);
            }
            return count;
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "redis_unavailable");
        }
    }
}
