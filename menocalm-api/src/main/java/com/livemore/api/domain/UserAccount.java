package com.livemore.api.domain;

import org.springframework.data.annotation.Id;

import java.time.Instant;

public class UserAccount {

    @Id
    private String id;

    private String phoneDigits;

    private Instant createdAt;
    private Instant updatedAt;

    public UserAccount() {
    }

    public UserAccount(String id, String phoneDigits, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.phoneDigits = phoneDigits;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static String userIdFromPhoneDigits(String digits) {
        return "phone_" + digits;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhoneDigits() {
        return phoneDigits;
    }

    public void setPhoneDigits(String phoneDigits) {
        this.phoneDigits = phoneDigits;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
