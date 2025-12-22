package com.delivery.RouteX.dto.driver;

import com.delivery.RouteX.model.Driver;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAvailabilityRequest {
    @NotNull
    private Driver.AvailabilityStatus status;
}
