package com.delivery.RouteX.dto.driver;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateDriverRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String vehicleType;
    private String vehiclePlate;
    private String licenseNumber;
    private LocalDateTime licenseExpiry;
}