package com.livemore.api.web.dto;

import jakarta.validation.constraints.NotBlank;

public class PhoneCodeVerifyRequest {
    @NotBlank
    private String phone;
    @NotBlank
    private String code;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
