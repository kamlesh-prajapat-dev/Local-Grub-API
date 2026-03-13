package com.codedbg.localgrub.service;

import com.codedbg.localgrub.dto.LoginResponse;
import com.codedbg.localgrub.exception.DatabaseOperationException;
import com.codedbg.localgrub.util.JwtUtil;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import java.util.concurrent.ExecutionException;

@Service
public class AdminService {

    private final static String ADMIN_USER_COLLECTION = "admins";
    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    @Autowired
    private Firestore firestore;

    @Autowired
    private JwtUtil jwtUtil;

    public LoginResponse login(String username, String password) {

        CollectionReference collection = firestore.collection(ADMIN_USER_COLLECTION);
        Query query = collection.whereEqualTo("username", username).limit(1);

        try {
            QuerySnapshot querySnapshot = query.get().get();

            if (querySnapshot.isEmpty()) {
                throw new AuthenticationException("Invalid username or password");
            }

            QueryDocumentSnapshot document = querySnapshot.getDocuments().getFirst();
            com.codedbg.localgrub.model.AdminUser adminUser = document.toObject(com.codedbg.localgrub.model.AdminUser.class);

            if (!password.equals(adminUser.getPassword())) {
                throw new AuthenticationException("Invalid username or password");
            }

            String token = jwtUtil.generateToken(adminUser.getId());

            return new LoginResponse(
                    adminUser.getId(),
                    token
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while fetching user by username: {}", username, e);
            throw new DatabaseOperationException("Interrupted while fetching user", e);

        } catch (ExecutionException e) {
            log.error("Execution exception occurred while fetching user by username: {}", username, e);
            throw new DatabaseOperationException("Failed to retrieve user from Firestore", e);
        } catch (AuthenticationException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}