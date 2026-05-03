package com.livemore.api.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthTokenResponseDto {

    private String accessToken;
    private String refreshToken;
    private double expiresIn;
    @JsonProperty("userID")
    private String userId;

    public AuthTokenResponseDto() {
    }

    public AuthTokenResponseDto(String accessToken, String refreshToken, double expiresIn, String userId) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.userId = userId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public double getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(double expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
