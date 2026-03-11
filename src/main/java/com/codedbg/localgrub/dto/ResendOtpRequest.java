package com.codedbg.localgrub.dto;

import lombok.Data;

@Data
public class ResendOtpRequest {
    private String requestId;
    private String phoneNumber;
}
