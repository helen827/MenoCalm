package com.livemore.api.service;

import com.livemore.api.config.AppProperties;
import com.livemore.api.support.TokenHasher;
import com.livemore.api.web.dto.PhoneCodeSendResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@SuppressWarnings("null")
public class SmsCodeService {

    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;
    private final SmsSender smsSender;
    private final SecureRandom secureRandom = new SecureRandom();

    public SmsCodeService(StringRedisTemplate redisTemplate, AppProperties appProperties, SmsSender smsSender) {
        this.redisTemplate = redisTemplate;
        this.appProperties = appProperties;
        this.smsSender = smsSender;
    }

    public PhoneCodeSendResponse sendLoginCode(String phoneDigits, String clientIp) {
        AppProperties.Verification cfg = appProperties.getAuth().getSms().getVerification();
        Duration hour = Duration.ofHours(1);
        String normalizedIp = normalizeIp(clientIp);
        long phoneHourly = incrementWithExpiry("ratelimit:sms:send:phone:" + phoneDigits, hour);
        if (phoneHourly > cfg.getPerPhonePerHourMax()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "sms_send_rate_limit_phone");
        }
        long ipHourly = incrementWithExpiry("ratelimit:sms:send:ip:" + normalizedIp, hour);
        if (ipHourly > cfg.getPerIpPerHourMax()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "sms_send_rate_limit_ip");
        }
        String cooldownKey = "auth:sms:cooldown:" + phoneDigits;
        boolean allowed = setIfAbsent(cooldownKey, "1", Duration.ofSeconds(cfg.getResendCooldownSeconds()));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "sms_send_too_frequent");
        }

        String code = nextSixDigitCode();
        String codeHash = hashCode(phoneDigits, code);
        setValue("auth:sms:code:" + phoneDigits, codeHash, Duration.ofSeconds(cfg.getCodeTtlSeconds()));
        redisTemplate.delete("auth:sms:verify_attempts:" + phoneDigits);
        smsSender.sendLoginCode(phoneDigits, code);
        return new PhoneCodeSendResponse(true, cfg.getCodeTtlSeconds(), cfg.getResendCooldownSeconds());
    }

    public void verifyCodeOrThrow(String phoneDigits, String code, String clientIp) {
        AppProperties.Verification cfg = appProperties.getAuth().getSms().getVerification();
        String normalizedIp = normalizeIp(clientIp);
        long ipVerify = incrementWithExpiry("ratelimit:sms:verify:ip:" + normalizedIp, Duration.ofHours(1));
        if (ipVerify > cfg.getPerIpPerHourMax()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "sms_verify_rate_limit_ip");
        }
        String key = "auth:sms:code:" + phoneDigits;
        String storedHash = getValue(key);
        if (storedHash == null || storedHash.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "sms_code_expired");
        }
        String requestHash = hashCode(phoneDigits, code);
        if (!storedHash.equals(requestHash)) {
            long attempts = incrementWithExpiry(
                    "auth:sms:verify_attempts:" + phoneDigits,
                    Duration.ofSeconds(cfg.getCodeTtlSeconds())
            );
            if (attempts >= cfg.getMaxVerifyAttempts()) {
                redisTemplate.delete(key);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "sms_code_verify_too_many_attempts");
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "sms_code_invalid");
        }
        redisTemplate.delete(key);
        redisTemplate.delete("auth:sms:verify_attempts:" + phoneDigits);
    }

    private String hashCode(String phoneDigits, String code) {
        return TokenHasher.sha256Hex(phoneDigits + ":" + code.trim());
    }

    private String nextSixDigitCode() {
        int value = secureRandom.nextInt(1_000_000);
        return String.format("%06d", value);
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

    private boolean setIfAbsent(String key, String value, Duration ttl) {
        try {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
            return Boolean.TRUE.equals(ok);
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "redis_unavailable");
        }
    }

    private void setValue(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "redis_unavailable");
        }
    }

    private String getValue(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "redis_unavailable");
        }
    }

    private String normalizeIp(String clientIp) {
        return (clientIp == null || clientIp.isBlank()) ? "unknown" : clientIp.trim();
    }
}
