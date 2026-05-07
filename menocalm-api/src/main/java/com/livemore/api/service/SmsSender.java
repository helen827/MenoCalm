package com.livemore.api.service;

public interface SmsSender {
    void sendLoginCode(String phoneDigits, String code);
}
