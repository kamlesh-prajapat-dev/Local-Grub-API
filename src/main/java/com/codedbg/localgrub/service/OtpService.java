package com.codedbg.localgrub.service;

import com.codedbg.localgrub.config.Msg91Config;
import com.codedbg.localgrub.dto.VerifyOtpResponse;
import com.codedbg.localgrub.exception.OtpServiceException;
import com.codedbg.localgrub.exception.PhoneNumberBlockedException;
import com.codedbg.localgrub.exception.RateLimitExceededException;
import com.codedbg.localgrub.util.AppConstant;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^\\d{10}$");

    private static final String MSG91_WIDGET_SEND_OTP_URL = "https://api.msg91.com/api/v5/widget/sendOtp";
    private static final String MSG91_WIDGET_VERIFY_OTP_URL = "https://api.msg91.com/api/v5/widget/verifyOtp";
    private static final String MSG91_WIDGET_RETRY_OTP_URL = "https://api.msg91.com/api/v5/widget/retryOtp";
    private static final String MSG91_WIDGET_VERIFY_ACCESS_TOKEN = "https://api.msg91.com/api/v5/widget/verifyAccessToken";

    // Rate limiting
    private static final int OTP_COOLDOWN_SECONDS = 60;
    private static final int MAX_OTP_PER_DAY = 10;

    // Brute-force protection
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int BLOCK_DURATION_MINUTES = 15;

    private final Cache<String, Integer> otpRequestCountCache;
    private final Cache<String, Integer> failedAttemptsCache;
    private final Cache<String, Boolean> blockedNumbersCache;
    private final Cache<String, Long> otpCooldownCache;

    @Autowired
    private Msg91Config msg91Config;

    @Autowired
    private RestTemplate restTemplate;

    public OtpService() {
        otpRequestCountCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();
        failedAttemptsCache = CacheBuilder.newBuilder().expireAfterWrite(BLOCK_DURATION_MINUTES, TimeUnit.MINUTES).build();
        blockedNumbersCache = CacheBuilder.newBuilder().expireAfterWrite(BLOCK_DURATION_MINUTES, TimeUnit.MINUTES).build();
        otpCooldownCache = CacheBuilder.newBuilder().expireAfterWrite(OTP_COOLDOWN_SECONDS, TimeUnit.SECONDS).build();
    }

    public VerifyOtpResponse sendOtp(String phoneNumber) {
        validateAndCheckBlock(phoneNumber);
        checkRateLimiting(phoneNumber);

        String mobileNumber = AppConstant.INDIA_COUNTRY_CODE + phoneNumber;
        Map<String, Object> body = new HashMap<>();
        body.put(AppConstant.WIDGET_ID, msg91Config.getWidgetId());
        body.put(AppConstant.IDENTIFIER, mobileNumber);

        logger.info("Body is {}", body);

        VerifyOtpResponse response = performPostRequest(MSG91_WIDGET_SEND_OTP_URL, body, "sending OTP to " + mobileNumber);

        if (response != null && AppConstant.SUCCESS_RESULT.equals(response.getType())) {
            logger.info("OTP sent successfully to {}", mobileNumber);
            incrementOtpRequestCount(phoneNumber);
            otpCooldownCache.put(phoneNumber, System.currentTimeMillis());
            return response;
        } else {
            logAndThrowOtpError("sending OTP to " + mobileNumber, response);
            return null;
        }
    }

    public VerifyOtpResponse verifyOtp(String requestId, String otp, String phoneNumber) {
        validateAndCheckBlock(phoneNumber);

        String mobile = AppConstant.INDIA_COUNTRY_CODE + phoneNumber;
        Map<String, Object> body = new HashMap<>();
        body.put(AppConstant.WIDGET_ID, msg91Config.getWidgetId());
        body.put(AppConstant.REQUEST_ID, requestId);
        body.put(AppConstant.OTP, otp);

        VerifyOtpResponse response = performPostRequest(MSG91_WIDGET_VERIFY_OTP_URL, body, "verifying OTP for " + mobile);

        if (response != null && AppConstant.SUCCESS_RESULT.equals(response.getType())) {
            logger.info("OTP verified successfully for {}", mobile);
            failedAttemptsCache.invalidate(phoneNumber);
            return response;
        } else {
            handleFailedVerification(phoneNumber);
            logAndThrowOtpError("verifying OTP for " + mobile, response);
            return null;
        }
    }

    public void verifyToken(String token) {
        Map<String, Object> body = new HashMap<>();
        body.put(AppConstant.ACCESS_TOKEN, token);

        VerifyOtpResponse response = performPostRequest(MSG91_WIDGET_VERIFY_ACCESS_TOKEN, body, "verifying token " + token);

        if (response != null && AppConstant.SUCCESS_RESULT.equals(response.getType())) {
            logger.info("Token verified successfully for {}", token);
        } else {
            logAndThrowOtpError("verifying token " + token, response);
        }
    }

    public VerifyOtpResponse retryOtp(String phoneNumber, String requestId) {
        validateAndCheckBlock(phoneNumber);
        checkRateLimiting(phoneNumber);

        String mobileNumber = AppConstant.INDIA_COUNTRY_CODE + phoneNumber;
        Map<String, Object> body = new HashMap<>();
        body.put(AppConstant.WIDGET_ID, msg91Config.getWidgetId());
        body.put(AppConstant.REQUEST_ID, requestId);

        VerifyOtpResponse response = performPostRequest(MSG91_WIDGET_RETRY_OTP_URL, body, "resending OTP to " + mobileNumber);

        if (response != null && AppConstant.SUCCESS_RESULT.equals(response.getType())) {
            logger.info("OTP resent successfully to {}", mobileNumber);
            incrementOtpRequestCount(phoneNumber);
            otpCooldownCache.put(phoneNumber, System.currentTimeMillis());
            return response;
        } else {
            logAndThrowOtpError("resending OTP to " + mobileNumber, response);
            return null;
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(AppConstant.AUTH_KEY, msg91Config.getAuthKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        headers.set("Accept", "*/*");
        headers.set("Connection", "keep-alive");
        return headers;
    }

    private VerifyOtpResponse performPostRequest(String url, Map<String, Object> body, String actionDescription) {
        try {
            HttpEntity<?> entity = new HttpEntity<>(body, createHeaders());
            logger.info("Entity is: {}", entity);
            return restTemplate.postForObject(url, entity, VerifyOtpResponse.class);
        } catch (Exception e) {
            logger.error("Error {}: {}", actionDescription, e.getMessage());
            throw new OtpServiceException("Error " + actionDescription, e);
        }
    }

    private void validateAndCheckBlock(String phoneNumber) {
        validatePhoneNumber(phoneNumber);
        if (isBlocked(phoneNumber)) {
            throw new PhoneNumberBlockedException("This phone number is temporarily blocked due to too many failed attempts.");
        }
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || !PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches()) {
            throw new IllegalArgumentException("Invalid phone number format. Please provide a 10-digit number.");
        }
    }

    private void checkRateLimiting(String phoneNumber) {
        if (otpCooldownCache.getIfPresent(phoneNumber) != null) {
            throw new RateLimitExceededException("Please wait a moment before requesting another OTP.");
        }
        try {
            int requestsToday = otpRequestCountCache.get(phoneNumber, () -> 0);
            if (requestsToday >= MAX_OTP_PER_DAY) {
                throw new RateLimitExceededException("You have exceeded the daily limit for OTP requests.");
            }
        } catch (ExecutionException e) {
            throw new OtpServiceException("Error accessing OTP request count cache", e);
        }
    }

    private void incrementOtpRequestCount(String phoneNumber) {
        try {
            int requestsToday = otpRequestCountCache.get(phoneNumber, () -> 0);
            otpRequestCountCache.put(phoneNumber, requestsToday + 1);
        } catch (ExecutionException e) {
            throw new OtpServiceException("Error updating OTP request count cache", e);
        }
    }

    private void handleFailedVerification(String phoneNumber) {
        try {
            int attempts = failedAttemptsCache.get(phoneNumber, () -> 0) + 1;
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                blockedNumbersCache.put(phoneNumber, true);
                failedAttemptsCache.invalidate(phoneNumber);
                logger.warn("Phone number {} has been blocked for {} minutes.", phoneNumber, BLOCK_DURATION_MINUTES);
            } else {
                failedAttemptsCache.put(phoneNumber, attempts);
            }
        } catch (ExecutionException e) {
            throw new OtpServiceException("Error updating failed attempts cache", e);
        }
    }

    private boolean isBlocked(String phoneNumber) {
        return blockedNumbersCache.getIfPresent(phoneNumber) != null;
    }

    private void logAndThrowOtpError(String action, VerifyOtpResponse response) {
        String errorMessage = response != null ? response.getMessage() : "No response";
        logger.error("Error {}: {}", action, errorMessage);
        throw new OtpServiceException("Error " + action + ": " + errorMessage);
    }
}
