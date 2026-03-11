package com.codedbg.localgrub.service;

import com.codedbg.localgrub.dto.User;
import com.codedbg.localgrub.exception.DatabaseOperationException;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private static final String USER_COLLECTION = "users";

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private FCMService fcmService;

    @Autowired
    private Firestore firestore;

    public String getDocumentId() {
        return firestore.collection(USER_COLLECTION).document().getId();
    }

    public User getUserByPhoneNumber(String phoneNumber) {
        CollectionReference users = firestore.collection(USER_COLLECTION);
        Query query = users.whereEqualTo("phoneNumber", phoneNumber).limit(1);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        try {
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
                return document.toObject(User.class);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while fetching user by phone number: {}", phoneNumber, e);
            throw new DatabaseOperationException("Interrupted while fetching user", e);
        } catch (ExecutionException e) {
            log.error("Execution exception occurred while fetching user by phone number: {}", phoneNumber, e);
            throw new DatabaseOperationException("Failed to retrieve user from Firestore", e);
        }

        return null;
    }

    public void createUserProfile(User newUser) {
        try {
            ApiFuture<WriteResult> future = firestore.collection(USER_COLLECTION).document(newUser.getUid()).set(newUser);
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while creating user profile for UID: {}", newUser.getUid(), e);
            throw new DatabaseOperationException("Interrupted while creating user profile", e);
        } catch (ExecutionException e) {
            log.error("Execution exception occurred while creating user profile for UID: {}", newUser.getUid(), e);
            throw new DatabaseOperationException("Failed to create user profile in Firestore", e);
        }
    }
}