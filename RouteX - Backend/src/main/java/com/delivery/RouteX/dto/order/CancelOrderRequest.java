package com.delivery.RouteX.dto.order;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CancelOrderRequest {
    @NotBlank
    @Size(max = 500)
    private String reason;
}
