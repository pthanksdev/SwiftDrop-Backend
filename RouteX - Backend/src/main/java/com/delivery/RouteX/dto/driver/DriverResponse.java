package com.delivery.RouteX.dto.driver;

import com.delivery.RouteX.model.Driver;
import lombok.*;
import java.time.LocalDateTime;

// DriverResponse
@Data
@Builder
public class DriverResponse {
    private Long id;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String vehicleType;
    private String vehiclePlate;
    private String licenseNumber;
    private LocalDateTime licenseExpiry;
    private Double currentLatitude;
    private Double currentLongitude;
    private Driver.AvailabilityStatus availabilityStatus;
    private Double rating;
    private Integer totalDeliveries;
    private Integer completedDeliveries;
    private Double totalEarnings;
    private LocalDateTime lastLocationUpdate;
    private LocalDateTime createdAt;
}
