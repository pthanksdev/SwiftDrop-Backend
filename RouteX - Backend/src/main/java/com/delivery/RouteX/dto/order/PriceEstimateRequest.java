package com.delivery.RouteX.dto.order;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PriceEstimateRequest {
    @NotNull
    private Double pickupLatitude;
    @NotNull
    private Double pickupLongitude;
    @NotNull
    private Double deliveryLatitude;
    @NotNull
    private Double deliveryLongitude;
    @NotNull
    @DecimalMin("0.1")
    private Double packageWeight;
    private String promoCode;
}