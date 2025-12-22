package com.delivery.RouteX.service;

import com.delivery.RouteX.Repository.NotificationRepository;
import com.delivery.RouteX.model.Notification;
import com.delivery.RouteX.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void sendOrderCreatedNotification(Order order) {
        Notification notification = Notification.builder()
                .user(order.getCustomer().getUser())
                .type(Notification.NotificationType.ORDER_CREATED)
                .title("Order Created Successfully")
                .message(String.format("Your order %s has been created and is awaiting driver assignment.",
                        order.getOrderNumber()))
                .relatedEntityId(order.getId().toString())
                .actionUrl("/customer/orders/" + order.getId())
                .build();

        notificationRepository.save(notification);
        log.info("Order created notification sent to user {}", order.getCustomer().getUser().getId());
    }

    @Transactional
    public void sendDriverAssignedNotification(Order order) {
        Notification customerNotification = Notification.builder()
                .user(order.getCustomer().getUser())
                .type(Notification.NotificationType.DRIVER_ASSIGNED)
                .title("Driver Assigned")
                .message(String.format("Driver %s has been assigned to your order %s.",
                        order.getDriver().getUser().getFirstName(), order.getOrderNumber()))
                .relatedEntityId(order.getId().toString())
                .actionUrl("/customer/orders/" + order.getId())
                .build();

        Notification driverNotification = Notification.builder()
                .user(order.getDriver().getUser())
                .type(Notification.NotificationType.ORDER_ASSIGNED)
                .title("New Order Assigned")
                .message(String.format("You have been assigned order %s. Pickup from %s",
                        order.getOrderNumber(), order.getPickupAddress()))
                .relatedEntityId(order.getId().toString())
                .actionUrl("/driver/orders/" + order.getId())
                .build();

        notificationRepository.save(customerNotification);
        notificationRepository.save(driverNotification);
        log.info("Driver assigned notifications sent");
    }

    @Transactional
    public void sendOrderStatusUpdateNotification(Order order) {
        String message = switch (order.getStatus()) {
            case PICKED_UP -> "Your package has been picked up and is on its way.";
            case IN_TRANSIT -> "Your package is in transit to the destination.";
            case DELIVERED -> "Your package has been delivered successfully!";
            case FAILED -> "Delivery attempt failed. Please contact support.";
            default -> "Order status updated.";
        };

        Notification notification = Notification.builder()
                .user(order.getCustomer().getUser())
                .type(getNotificationTypeForStatus(order.getStatus()))
                .title("Order Update: " + order.getStatus())
                .message(String.format("Order %s: %s", order.getOrderNumber(), message))
                .relatedEntityId(order.getId().toString())
                .actionUrl("/customer/orders/" + order.getId())
                .build();

        notificationRepository.save(notification);
        log.info("Order status notification sent for order {}", order.getOrderNumber());
    }

    @Transactional
    public void sendOrderCancelledNotification(Order order) {
        Notification customerNotification = Notification.builder()
                .user(order.getCustomer().getUser())
                .type(Notification.NotificationType.ORDER_CANCELLED)
                .title("Order Cancelled")
                .message(String.format("Your order %s has been cancelled. Reason: %s",
                        order.getOrderNumber(), order.getCancellationReason()))
                .relatedEntityId(order.getId().toString())
                .build();

        notificationRepository.save(customerNotification);

        if (order.getDriver() != null) {
            Notification driverNotification = Notification.builder()
                    .user(order.getDriver().getUser())
                    .type(Notification.NotificationType.ORDER_CANCELLED)
                    .title("Order Cancelled")
                    .message(String.format("Order %s has been cancelled.", order.getOrderNumber()))
                    .relatedEntityId(order.getId().toString())
                    .build();

            notificationRepository.save(driverNotification);
        }

        log.info("Order cancelled notifications sent for order {}", order.getOrderNumber());
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications =
                notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        unreadNotifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unreadNotifications);

        log.info("Marked {} notifications as read for user {}", unreadNotifications.size(), userId);
    }

    private Notification.NotificationType getNotificationTypeForStatus(Order.OrderStatus status) {
        return switch (status) {
            case PICKED_UP -> Notification.NotificationType.ORDER_PICKED_UP;
            case IN_TRANSIT -> Notification.NotificationType.ORDER_IN_TRANSIT;
            case DELIVERED -> Notification.NotificationType.ORDER_DELIVERED;
            case FAILED -> Notification.NotificationType.ORDER_FAILED;
            default -> Notification.NotificationType.SYSTEM_ALERT;
        };
    }
}
