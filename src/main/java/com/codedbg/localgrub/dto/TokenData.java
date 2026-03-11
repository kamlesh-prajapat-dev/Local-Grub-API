package com.codedbg.localgrub.dto;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.Data;

@Data
public class TokenData {
    private String token;

    @ServerTimestamp
    private Timestamp createAt;

    @ServerTimestamp
    private Timestamp updatedAt;

    private String platform;
}
