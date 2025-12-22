package com.delivery.RouteX.dto.websocket;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class LocationUpdateMessage {
    private Long orderId;
    private Long driverId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
}