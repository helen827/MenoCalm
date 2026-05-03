package com.livemore.api.web.dto;

import jakarta.validation.constraints.NotBlank;

public class PhoneCodeSendRequest {
    @NotBlank
    private String phone;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
