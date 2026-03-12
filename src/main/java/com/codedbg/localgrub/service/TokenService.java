package com.codedbg.localgrub.service;

import com.codedbg.localgrub.dto.TokenData;
import com.codedbg.localgrub.dto.TokenRequest;
import com.codedbg.localgrub.exception.DatabaseOperationException;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class TokenService {

    @Autowired
    private Firestore firestore;

    private static final String COLLECTION_NAME = "tokens";
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);


    public String getToken(String userId) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("UserId must not be null or empty");
        }

        try {

            DocumentReference documentRef =
                    firestore.collection(COLLECTION_NAME).document(userId);

            DocumentSnapshot document = documentRef.get().get();

            if (!document.exists()) {
                log.warn("No token found for userId: {}", userId);
                return null;
            }

            String token = document.getString("token");

            log.debug("Token fetched successfully for userId: {}", userId);

            return token;

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();
            log.error("Thread interrupted while fetching token for userId: {}", userId, ex);

            throw new DatabaseOperationException(
                    "Thread interrupted while fetching token", ex
            );

        } catch (ExecutionException ex) {

            log.error("Firestore execution error while fetching token for userId: {}", userId, ex);

            throw new DatabaseOperationException(
                    "Failed to retrieve token from Firestore", ex
            );
        }
    }

    public void saveToken(String userId, TokenRequest data) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("UserId must not be null or empty");
        }

        if (data == null || data.getToken() == null || data.getToken().isBlank()) {
            throw new IllegalArgumentException("Token request must contain a valid token");
        }

        try {

            TokenData tokenData = new TokenData(
                    data.getToken(),
                    data.getPlatform(),
                    null,
                    null
            );

            DocumentReference documentRef =
                    firestore.collection(COLLECTION_NAME).document(userId);

            WriteResult result = documentRef.set(tokenData).get();

            log.info(
                    "Token saved successfully for userId: {} at {}",
                    userId,
                    result.getUpdateTime()
            );

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();
            log.error("Thread interrupted while saving token for userId: {}", userId, ex);

            throw new DatabaseOperationException(
                    "Thread interrupted while saving token", ex
            );

        } catch (ExecutionException ex) {

            log.error("Firestore execution error while saving token for userId: {}", userId, ex);

            throw new DatabaseOperationException(
                    "Failed to save token in Firestore", ex
            );
        }
    }

    public void updateToken(String userId, String token) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("UserId must not be null or empty");
        }

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token must not be null or empty");
        }

        try {

            DocumentReference documentRef =
                    firestore.collection(COLLECTION_NAME).document(userId);

            Map<String, Object> updates = new HashMap<>();
            updates.put("token", token);
            updates.put("updatedAt", FieldValue.serverTimestamp());

            WriteResult result = documentRef.update(updates).get();

            log.info(
                    "Token updated successfully for userId: {} at {}",
                    userId,
                    result.getUpdateTime()
            );

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();
            log.error("Thread interrupted while updating token for userId: {}", userId, ex);

            throw new DatabaseOperationException(
                    "Thread interrupted while updating token", ex
            );

        } catch (ExecutionException ex) {

            log.error("Firestore execution error while updating token for userId: {}", userId, ex);

            throw new DatabaseOperationException(
                    "Failed to update token in Firestore", ex
            );
        }
    }

    public void deleteToken(String userId) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("UserId must not be null or empty");
        }

        try {

            DocumentReference documentRef =
                    firestore.collection(COLLECTION_NAME).document(userId);

            WriteResult result = documentRef.delete().get();

            log.info(
                    "Token deleted successfully for userId: {} at {}",
                    userId,
                    result.getUpdateTime()
            );

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();
            log.error("Thread interrupted while deleting token for userId: {}", userId, ex);

            throw new DatabaseOperationException(
                    "Thread interrupted while deleting token", ex
            );

        } catch (ExecutionException ex) {

            log.error("Firestore execution error while deleting token for userId: {}", userId, ex);

            throw new DatabaseOperationException(
                    "Failed to delete token from Firestore", ex
            );
        }
    }
}
