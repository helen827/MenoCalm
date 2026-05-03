package com.livemore.api.service;

public interface LoginRateLimitService {
    void checkAndConsume(String phoneDigits, String clientIp);
}
