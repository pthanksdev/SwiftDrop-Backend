package com.delivery.RouteX.dto.websocket;

import com.delivery.RouteX.model.Order;
import lombok.*  ;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderUpdateMessage {
    private Long orderId;
    private String orderNumber;
    private Order.OrderStatus status;
    private String message;
    private LocalDateTime timestamp;
}
