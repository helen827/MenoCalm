package com.livemore.api.web.dto;

import jakarta.validation.constraints.NotBlank;

public class PhoneLoginRequest {

    @NotBlank
    private String phone;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
