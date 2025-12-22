package com.delivery.RouteX.dto.driver;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DriverStatistics {
    private Integer totalDeliveries;
    private Integer completedDeliveries;
    private Integer cancelledDeliveries;
    private Double rating;
    private Double totalEarnings;
    private Double successRate;
}