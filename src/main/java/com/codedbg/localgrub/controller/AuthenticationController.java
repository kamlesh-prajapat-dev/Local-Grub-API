package com.codedbg.localgrub.controller;

import com.codedbg.localgrub.dto.*;
import com.codedbg.localgrub.exception.DatabaseOperationException;
import com.codedbg.localgrub.service.OtpService;
import com.codedbg.localgrub.service.UserService;
import com.codedbg.localgrub.util.JwtUtil;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/auth"})
public class AuthenticationController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;


    @PostMapping({"/send-otp"})
    public ResponseEntity<?> sendOtp(@RequestBody OtpRequest otpRequest) {
        VerifyOtpResponse response = this.otpService.sendOtp(otpRequest.getPhoneNumber());
        Map<String, String> responseData = new HashMap<>();
        responseData.put("message", response.getMessage());
        responseData.put("type", response.getType());
        return ResponseEntity.ok(new ApiResponse<>(true, "OTP sent successfully", responseData));
    }

    @PostMapping({"/verify-otp"})
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {
        VerifyOtpResponse response = this.otpService.verifyOtp(request.getRequestId(), request.getOtp(), request.getPhoneNumber());
        String token = response.getMessage();
        this.otpService.verifyToken(token);
        User user = this.userService.getUserByPhoneNumber(request.getPhoneNumber());
        boolean isNewUser = user == null;
        if (isNewUser) {
            user = new User();
            user.setUid(this.userService.getDocumentId());
            user.setPhoneNumber(request.getPhoneNumber());

            try {
                this.userService.createUserProfile(user);
            } catch (DatabaseOperationException var8) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(false, "Failed to create user profile.", null));
            }
        }

        String jwtToken = this.jwtUtil.generateToken(user.getUid());
        AuthResponse authResponse = new AuthResponse(jwtToken, isNewUser, user);
        return ResponseEntity.ok(new ApiResponse<>(true, "OTP verified successfully", authResponse));
    }

    @PostMapping({"/retry-otp"})
    public ResponseEntity<?> retryOtp(@RequestBody ResendOtpRequest request) {
        VerifyOtpResponse response = this.otpService.retryOtp(request.getPhoneNumber(), request.getRequestId());
        Map<String, String> responseData = new HashMap<>();
        responseData.put("message", response.getMessage());
        responseData.put("type", response.getType());
        return ResponseEntity.ok(new ApiResponse<>(true, "OTP resent successfully", responseData));
    }
}