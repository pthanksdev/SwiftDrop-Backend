package com.delivery.RouteX.dto.analytics;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class RevenueReport {
    private Double totalRevenue;
    private Long totalOrders;
    private Double averageOrderValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Map<String, Double> dailyBreakdown;
}