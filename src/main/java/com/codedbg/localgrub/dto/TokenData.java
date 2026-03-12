package com.codedbg.localgrub.dto;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class TokenData {
    private String token;
    private String platform;

    @ServerTimestamp
    private Timestamp createAt;

    @ServerTimestamp
    private Timestamp updatedAt;
}
