package com.codedbg.localgrub.dto;

import lombok.Data;

@Data
public class VerifyOtpRequest {
    private String otp;
    private String requestId;
    private String phoneNumber;
}