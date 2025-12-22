package com.delivery.RouteX.dto.driver;

import com.delivery.RouteX.model.Driver;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DriverSummary {
    private Long id;
    private String name;
    private String phone;
    private String vehicleType;
    private String vehiclePlate;
    private Driver.AvailabilityStatus availabilityStatus;
    private Double rating;
    private Integer totalDeliveries;
}

