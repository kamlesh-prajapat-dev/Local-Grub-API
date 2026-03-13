package com.codedbg.localgrub.model;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Data;

@Data
public class AdminUser {
    @DocumentId
    private String id;

    private String username;
    private String password;
}
