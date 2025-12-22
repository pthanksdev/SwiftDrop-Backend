package com.delivery.RouteX.dto.order;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PriceEstimateResponse {
    private Double distanceKm;
    private Double baseFare;
    private Double distanceCharge;
    private Double weightCharge;
    private Double peakHourSurcharge;
    private Double discount;
    private Double totalAmount;
    private String estimatedTime;
}