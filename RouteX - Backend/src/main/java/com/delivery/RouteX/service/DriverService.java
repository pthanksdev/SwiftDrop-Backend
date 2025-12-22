package com.delivery.RouteX.service;

import com.delivery.RouteX.Repository.*;
import com.delivery.RouteX.dto.driver.*;
import com.delivery.RouteX.exception.ResourceNotFoundException;
import com.delivery.RouteX.model.*;
import com.delivery.RouteX.util.DistanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final DistanceCalculator distanceCalculator;

    @Transactional(readOnly = true)
    public DriverResponse getDriverById(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        return mapToDriverResponse(driver);
    }

    @Transactional(readOnly = true)
    public DriverResponse getDriverByUserId(Long userId) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
        return mapToDriverResponse(driver);
    }

    @Transactional(readOnly = true)
    public Page<DriverSummary> getAllDrivers(Pageable pageable) {
        return driverRepository.findAll(pageable).map(this::mapToDriverSummary);
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> getAvailableDrivers() {
        return driverRepository.findByAvailabilityStatus(Driver.AvailabilityStatus.ONLINE)
                .stream()
                .map(this::mapToDriverResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> findNearbyDrivers(Double latitude, Double longitude, Double radiusKm) {
        double latDiff = radiusKm / 111.0;
        double lonDiff = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude)));

        List<Driver> driversInArea = driverRepository.findAvailableDriversInArea(
                latitude - latDiff, latitude + latDiff,
                longitude - lonDiff, longitude + lonDiff
        );

        return driversInArea.stream()
                .filter(driver -> {
                    double distance = distanceCalculator.calculateDistance(
                            latitude, longitude,
                            driver.getCurrentLatitude(), driver.getCurrentLongitude()
                    );
                    return distance <= radiusKm;
                })
                .map(this::mapToDriverResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DriverResponse updateDriverProfile(Long userId, UpdateDriverRequest request) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        User user = driver.getUser();

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getVehicleType() != null) driver.setVehicleType(request.getVehicleType());
        if (request.getVehiclePlate() != null) driver.setVehiclePlate(request.getVehiclePlate());
        if (request.getLicenseNumber() != null) driver.setLicenseNumber(request.getLicenseNumber());
        if (request.getLicenseExpiry() != null) driver.setLicenseExpiry(request.getLicenseExpiry());

        userRepository.save(user);
        driver = driverRepository.save(driver);

        log.info("Driver profile updated: {}", driver.getId());
        return mapToDriverResponse(driver);
    }

    @Transactional
    public void updateDriverLocation(Long userId, UpdateLocationRequest request) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        driver.updateLocation(request.getLatitude(), request.getLongitude());
        driverRepository.save(driver);

        log.debug("Driver {} location updated: {}, {}",
                driver.getId(), request.getLatitude(), request.getLongitude());
    }

    @Transactional
    public DriverResponse updateAvailability(Long userId, UpdateAvailabilityRequest request) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        driver.setAvailabilityStatus(request.getStatus());
        driver = driverRepository.save(driver);

        log.info("Driver {} availability updated to {}", driver.getId(), request.getStatus());
        return mapToDriverResponse(driver);
    }

    @Transactional(readOnly = true)
    public DriverStatistics getDriverStatistics(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        return DriverStatistics.builder()
                .totalDeliveries(driver.getTotalDeliveries())
                .completedDeliveries(driver.getCompletedDeliveries())
                .cancelledDeliveries(driver.getCancelledDeliveries())
                .rating(driver.getRating())
                .totalEarnings(driver.getTotalEarnings())
                .successRate(calculateSuccessRate(driver))
                .build();
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> getTopDrivers(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return driverRepository.findTopDrivers(pageable)
                .stream()
                .map(this::mapToDriverResponse)
                .collect(Collectors.toList());
    }

    private double calculateSuccessRate(Driver driver) {
        if (driver.getTotalDeliveries() == 0) return 0.0;
        return (driver.getCompletedDeliveries() * 100.0) / driver.getTotalDeliveries();
    }

    private DriverResponse mapToDriverResponse(Driver driver) {
        User user = driver.getUser();

        return DriverResponse.builder()
                .id(driver.getId())
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .vehicleType(driver.getVehicleType())
                .vehiclePlate(driver.getVehiclePlate())
                .licenseNumber(driver.getLicenseNumber())
                .licenseExpiry(driver.getLicenseExpiry())
                .currentLatitude(driver.getCurrentLatitude())
                .currentLongitude(driver.getCurrentLongitude())
                .availabilityStatus(driver.getAvailabilityStatus())
                .rating(driver.getRating())
                .totalDeliveries(driver.getTotalDeliveries())
                .completedDeliveries(driver.getCompletedDeliveries())
                .totalEarnings(driver.getTotalEarnings())
                .lastLocationUpdate(driver.getLastLocationUpdate())
                .createdAt(driver.getCreatedAt())
                .build();
    }

    private DriverSummary mapToDriverSummary(Driver driver) {
        User user = driver.getUser();

        return DriverSummary.builder()
                .id(driver.getId())
                .name(user.getFirstName() + " " + user.getLastName())
                .phone(user.getPhone())
                .vehicleType(driver.getVehicleType())
                .vehiclePlate(driver.getVehiclePlate())
                .availabilityStatus(driver.getAvailabilityStatus())
                .rating(driver.getRating())
                .totalDeliveries(driver.getTotalDeliveries())
                .build();
    }
}