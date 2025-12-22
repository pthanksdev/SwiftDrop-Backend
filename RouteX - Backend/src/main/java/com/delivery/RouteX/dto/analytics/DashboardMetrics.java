package com.delivery.RouteX.dto.analytics;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardMetrics {
    private Long activeDeliveries;
    private Long pendingOrders;
    private Long ordersToday;
    private Double revenueToday;
    private Long availableDrivers;
    private Double averageDeliveryTime;
    private Double successRate;
}
