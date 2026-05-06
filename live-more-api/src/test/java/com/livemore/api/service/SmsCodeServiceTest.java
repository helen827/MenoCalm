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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsCodeServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SmsSender smsSender;

    private SmsCodeService smsCodeService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getAuth().getSms().getVerification().setCodeTtlSeconds(300);
        properties.getAuth().getSms().getVerification().setResendCooldownSeconds(60);
        properties.getAuth().getSms().getVerification().setPerPhonePerHourMax(6);
        properties.getAuth().getSms().getVerification().setPerIpPerHourMax(20);
        properties.getAuth().getSms().getVerification().setMaxVerifyAttempts(2);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        smsCodeService = new SmsCodeService(redisTemplate, properties, smsSender);
    }

    @Test
    void sendLoginCode_whenCooldownActive_returnsTooManyRequests() {
        String phone = "13800138000";
        when(valueOperations.increment("ratelimit:sms:send:phone:" + phone)).thenReturn(1L);
        when(valueOperations.increment("ratelimit:sms:send:ip:127.0.0.1")).thenReturn(1L);
        when(valueOperations.setIfAbsent(
                eq("auth:sms:cooldown:" + phone),
                eq("1"),
                eq(Duration.ofSeconds(60))
        )).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> smsCodeService.sendLoginCode(phone, "127.0.0.1")
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        verify(smsSender, never()).sendLoginCode(any(), any());
    }

    @Test
    void verifyCodeOrThrow_whenCodeExpired_returnsUnauthorized() {
        String phone = "13800138000";
        when(valueOperations.increment("ratelimit:sms:verify:ip:127.0.0.1")).thenReturn(1L);
        when(valueOperations.get("auth:sms:code:" + phone)).thenReturn(null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> smsCodeService.verifyCodeOrThrow(phone, "123456", "127.0.0.1")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void verifyCodeOrThrow_whenTooManyAttempts_deletesCodeAndReturns429() {
        String phone = "13800138000";
        when(valueOperations.increment("ratelimit:sms:verify:ip:127.0.0.1")).thenReturn(1L);
        when(valueOperations.get("auth:sms:code:" + phone)).thenReturn("stored-hash");
        when(valueOperations.increment("auth:sms:verify_attempts:" + phone)).thenReturn(2L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> smsCodeService.verifyCodeOrThrow(phone, "000000", "127.0.0.1")
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        verify(redisTemplate).delete("auth:sms:code:" + phone);
    }

    @Test
    void verifyCodeOrThrow_whenCodeMatches_deletesCodeAndAttempts() {
        String phone = "13800138000";
        String code = "123456";
        when(valueOperations.increment("ratelimit:sms:verify:ip:127.0.0.1")).thenReturn(1L);
        when(valueOperations.get("auth:sms:code:" + phone))
                .thenReturn(com.livemore.api.support.TokenHasher.sha256Hex(phone + ":" + code));

        smsCodeService.verifyCodeOrThrow(phone, code, "127.0.0.1");

        verify(redisTemplate).delete("auth:sms:code:" + phone);
        verify(redisTemplate).delete("auth:sms:verify_attempts:" + phone);
    }
}
