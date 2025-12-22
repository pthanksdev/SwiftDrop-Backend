package com.delivery.RouteX.dto.order;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RateOrderRequest {
    @NotNull
    @DecimalMin("1.0") @DecimalMax("5.0") private Double rating;
    @Size(max = 1000) private String feedback;
}
