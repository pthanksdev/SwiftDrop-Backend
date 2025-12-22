package com.delivery.RouteX.dto.analytics;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderStatistics {
    private Long totalOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private Long failedOrders;
    private Double completionRate;
    private Double cancellationRate;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}