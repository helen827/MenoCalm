package com.livemore.api.service;

import com.livemore.api.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisLoginRateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisLoginRateLimitService service;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getAuth().getLoginRateLimit().setPerPhoneMax(2);
        appProperties.getAuth().getLoginRateLimit().setPerIpMax(2);
        appProperties.getAuth().getLoginRateLimit().setWindowSeconds(120);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new RedisLoginRateLimitService(redisTemplate, appProperties);
    }

    @Test
    void checkAndConsume_firstAttempt_setsExpire() {
        when(valueOperations.increment("ratelimit:login:phone:13800138000")).thenReturn(1L);
        when(valueOperations.increment("ratelimit:login:ip:127.0.0.1")).thenReturn(1L);

        service.checkAndConsume("13800138000", "127.0.0.1");

        verify(redisTemplate).expire("ratelimit:login:phone:13800138000", Duration.ofSeconds(120));
        verify(redisTemplate).expire("ratelimit:login:ip:127.0.0.1", Duration.ofSeconds(120));
    }

    @Test
    void checkAndConsume_phoneExceeded_returns429() {
        when(valueOperations.increment("ratelimit:login:phone:13800138000")).thenReturn(3L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.checkAndConsume("13800138000", "127.0.0.1")
        );
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    }
}
