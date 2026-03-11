package com.codedbg.localgrub.dto;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Data;

@Data
public class User {
    @DocumentId
    private String uid = "";

    private String name = "";
    private String phoneNumber = "";
    private String address = "";
//    private String streetAddress = "";
//    private String city = "";
//    private int pinCode = 0;
//    private String State = "";

//    @ServerTimestamp
    private Long createAt = System.currentTimeMillis();
//    @ServerTimestamp
//    private Timestamp updateAt;

    private Boolean profileCompleted = false;
}
