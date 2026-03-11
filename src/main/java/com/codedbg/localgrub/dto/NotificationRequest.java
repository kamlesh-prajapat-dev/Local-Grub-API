package com.codedbg.localgrub.dto;

import lombok.Data;

@Data
public class NotificationRequest {
    private String userId = null;
    private String orderId;
    private String status;
    private String userName;
}
