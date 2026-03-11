package com.codedbg.localgrub.service;

import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OwnerService {

    private static final Logger logger = LoggerFactory.getLogger(OwnerService.class);

    @Autowired
    private Firestore firestore;

    private final static String COLLECTION_NAME = "admins";

    public String getAdminId() {
        try {
            return firestore.collection(COLLECTION_NAME).limit(1).get().get().getDocuments().getFirst().getId();
        } catch (Exception e) {
            logger.error("Failed to get admin ID: {}", e.getMessage(), e);
            return null;
        }
    }
}
