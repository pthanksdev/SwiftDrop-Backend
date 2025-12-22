package com.delivery.RouteX.dto.notification;

import com.delivery.RouteX.model.Notification;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private Notification.NotificationType type;
    private String title;
    private String message;
    private Boolean isRead;
    private String relatedEntityId;
    private String actionUrl;
    private LocalDateTime createdAt;
}
