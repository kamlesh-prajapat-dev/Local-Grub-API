package com.codedbg.localgrub.controller;

import com.codedbg.localgrub.dto.ApiResponse;
import com.codedbg.localgrub.dto.NotificationContent;
import com.codedbg.localgrub.dto.NotificationRequest;
import com.codedbg.localgrub.dto.TokenData;
import com.codedbg.localgrub.service.FCMService;
import com.codedbg.localgrub.service.OwnerService;
import com.codedbg.localgrub.service.TokenService;
import com.codedbg.localgrub.util.AppConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private FCMService fcmService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private OwnerService ownerService;

    private final static String COLLECTION_NAME = "tokens";

    @PostMapping("id/{id}")
    public ResponseEntity<?> saveToken(@PathVariable String id, @RequestBody TokenData data) {
        tokenService.saveToken(id, data);
        return ResponseEntity.ok(new ApiResponse<>(true, "Token saved successfully", null));
    }

    @PutMapping("/id/{id}")
    public ResponseEntity<?> updateToken(@PathVariable String id, @RequestBody String token) {
        tokenService.updateToken(id, token);
        return ResponseEntity.ok(new ApiResponse<>(true, "Token updated successfully", null));
    }

    @DeleteMapping("/id/{id}")
    public ResponseEntity<?> deleteToken(@PathVariable String id) {
        tokenService.deleteToken(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Token deleted successfully", null));
    }

    @PostMapping("/send-notification")
    public ResponseEntity<?> sendNotification(@RequestBody NotificationRequest request) {
        String token = tokenService.getToken(request.getUserId());
        if (token != null && !token.isBlank()) {
            NotificationContent notificationContent = getNotificationContent(request.getStatus(), request.getOrderId(), request.getUserName());
            try {
                fcmService.sendNotification(notificationContent.getTitle(), notificationContent.getBody(), token);
                return ResponseEntity.ok(new ApiResponse<>(true, "Notification sent successfully", null));
            } catch (Exception e) {
                logger.error("Failed to send notification: {}", e.getMessage(), e);
                return ResponseEntity.status(500).body(new ApiResponse<>(false, "Failed to send notification", e));
            }
        } else {
            logger.error("Fcm token not found.");
            return ResponseEntity.status(404).body(new ApiResponse<>(false, "Fcm token not found.", null));
        }
    }

    @PostMapping("/send-notification-to-owner")
    public ResponseEntity<?> sendNotificationToOwner(@RequestBody NotificationRequest request) {
        String adminId = ownerService.getAdminId();
        if (adminId != null && !adminId.isBlank()) {
            String token = tokenService.getToken(adminId);
            if (token != null && !token.isBlank()) {
                NotificationContent notificationContent = getNotificationContent(request.getStatus(), request.getOrderId(), request.getUserName());
                try {
                    fcmService.sendNotification(notificationContent.getTitle(), notificationContent.getBody(), token);
                    return ResponseEntity.ok(new ApiResponse<>(true, "Notification sent successfully", null));
                } catch (Exception e) {
                    logger.error("Failed to send notification: {}", e.getMessage(), e);
                    return ResponseEntity.status(500).body(new ApiResponse<>(false, "Failed to send notification", e));
                }
            } else {
                logger.error("Fcm token not found.");
                return ResponseEntity.status(404).body(new ApiResponse<>(false, "Fcm token not found.", null));
            }
        } else {
            logger.error("Admin ID not found.");
            return ResponseEntity.status(404).body(new ApiResponse<>(false, "Admin ID not found.", null));
        }
    }

    private NotificationContent getNotificationContent(String status, String orderId, String username) {
        return switch (status) {
            case AppConstant.PLACED -> new NotificationContent("New Order Received.", "You have received a new order from " + username + ".\nOrder #" + orderId + ".\nCheck the order details and confirm it.");
            case AppConstant.CONFIRMED -> new NotificationContent("Order Confirmed", "Your order has been confirmed by the shop and is now being prepared.");
            case AppConstant.PREPARING -> new NotificationContent("Order Preparing", "The shop has started preparing your order. We'll notify you when it's out for delivery.");
            case AppConstant.OUT_FOR_DELIVERY ->
                    new NotificationContent("Out for Delivery", "The shop has started preparing your order. We'll notify you when it's ready.");
            case AppConstant.DELIVERED -> new NotificationContent("Order Delivered", "Your order has been delivered. We hope you enjoy it!.");
            case AppConstant.CANCELLED -> new NotificationContent("Order Cancelled by " + username, "The customer has cancelled their order. No further action is required.");

            default -> throw new IllegalStateException("Unexpected value: " + status);
        };
    }
}
