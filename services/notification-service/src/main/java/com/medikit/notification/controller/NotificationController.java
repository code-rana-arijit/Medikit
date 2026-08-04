package com.medikit.notification.controller;

import com.medikit.notification.dto.NotificationRequest;
import com.medikit.notification.model.Notification;
import com.medikit.notification.service.NotificationSender;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationSender notificationSender;

    public NotificationController(NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    @GetMapping("/")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("notification service healthy");
    }

    @PostMapping("/send")
    public ResponseEntity<Notification> send(@RequestBody NotificationRequest request) {
        Notification notification = notificationSender.send(
                request.type(),
                request.type().name().toLowerCase(),
                request.recipient(),
                request.subject(),
                request.body(),
                Map.of("source", "rest-api")
        );
        return ResponseEntity.ok(notification);
    }
}
