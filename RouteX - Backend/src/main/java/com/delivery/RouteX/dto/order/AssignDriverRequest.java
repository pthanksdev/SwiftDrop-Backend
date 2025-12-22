package com.delivery.RouteX.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignDriverRequest {
    @NotNull
    private Long driverId;
}
