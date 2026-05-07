package com.livemore.api.web.dto;

public class PhoneCodeSendResponse {
    private boolean sent;
    private long expiresInSeconds;
    private long resendAfterSeconds;

    public PhoneCodeSendResponse() {
    }

    public PhoneCodeSendResponse(boolean sent, long expiresInSeconds, long resendAfterSeconds) {
        this.sent = sent;
        this.expiresInSeconds = expiresInSeconds;
        this.resendAfterSeconds = resendAfterSeconds;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public long getResendAfterSeconds() {
        return resendAfterSeconds;
    }

    public void setResendAfterSeconds(long resendAfterSeconds) {
        this.resendAfterSeconds = resendAfterSeconds;
    }
}
