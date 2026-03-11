package com.codedbg.localgrub.service;

import com.codedbg.localgrub.dto.TokenData;
import com.codedbg.localgrub.exception.DatabaseOperationException;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.transform.Result;
import java.util.concurrent.ExecutionException;

@Service
public class TokenService {

    @Autowired
    private Firestore firestore;

    private static final String COLLECTION_NAME = "tokens";
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);


    public String getToken(String id) {
        try {
            DocumentSnapshot document = firestore.collection(COLLECTION_NAME).document(id).get().get();
            if (document.exists()) {
                return document.getString("token");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while fetching FCM token by ID: {}", id, e);
            throw new DatabaseOperationException("Interrupted while fetching token", e);
        } catch (ExecutionException e) {
            log.error("Execution exception occurred while fetching FCM token by ID: {}", id, e);
            throw new DatabaseOperationException("Failed to retrieve token from Firestore", e);
        }
        return null;
    }

    public void saveToken(String userId, TokenData data) {
        try {
            firestore.collection(COLLECTION_NAME).document(userId).set(data).get();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to save token for user ID: {}", userId, e);
            throw new DatabaseOperationException("Failed to save token", e);
        }
    }

    public void updateToken(String userId, String token) {
        try {
            firestore.collection(COLLECTION_NAME).document(userId).update("token", token).get();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to update token for user ID: {}", userId, e);
            throw new DatabaseOperationException("Failed to update token", e);
        }
    }

    public void deleteToken(String userId) {
        try {
            WriteResult result = firestore.collection(COLLECTION_NAME).document(userId).delete().get();
            log.error("Deleted token for user ID: {}{}", userId, result);
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to delete token for user ID: {}", userId, e);
            throw new DatabaseOperationException("Failed to delete token", e);
        }
    }
}
