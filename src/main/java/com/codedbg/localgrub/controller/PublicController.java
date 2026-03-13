package com.codedbg.localgrub.controller;

import com.codedbg.localgrub.dto.LoginResponse;
import com.codedbg.localgrub.dto.ApiResponse;
import com.codedbg.localgrub.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public")
public class PublicController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/health-check")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok("Ok");
    }

    @PostMapping("/admin/login")
    public ResponseEntity<?> login(@RequestBody String username, @RequestBody String password) {
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Invalid Credentials.");
        }

        LoginResponse user = adminService.login(username, password);
        ApiResponse<LoginResponse> response = new ApiResponse<>(true, "Successfully login admin user.", user);
        return ResponseEntity.ok(response);
    }
}