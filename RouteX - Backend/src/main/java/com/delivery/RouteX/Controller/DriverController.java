package com.delivery.RouteX.Controller;

import com.delivery.RouteX.dto.driver.*;
import com.delivery.RouteX.model.User;
import com.delivery.RouteX.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class DriverController {

    private final DriverService driverService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<DriverResponse> getDriverById(@PathVariable Long id) {
        DriverResponse response = driverService.getDriverById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverResponse> getMyProfile(@AuthenticationPrincipal User user) {
        DriverResponse response = driverService.getDriverByUserId(user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<Page<DriverSummary>> getAllDrivers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DriverSummary> drivers = driverService.getAllDrivers(pageable);
        return ResponseEntity.ok(drivers);
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<List<DriverResponse>> getAvailableDrivers() {
        List<DriverResponse> drivers = driverService.getAvailableDrivers();
        return ResponseEntity.ok(drivers);
    }

    @GetMapping("/nearby")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<List<DriverResponse>> getNearbyDrivers(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radius
    ) {
        List<DriverResponse> drivers = driverService.findNearbyDrivers(
                latitude, longitude, radius
        );
        return ResponseEntity.ok(drivers);
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverResponse> updateMyProfile(
            @Valid @RequestBody UpdateDriverRequest request,
            @AuthenticationPrincipal User user
    ) {
        DriverResponse response = driverService.updateDriverProfile(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/location")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Map<String, String>> updateMyLocation(
            @Valid @RequestBody UpdateLocationRequest request,
            @AuthenticationPrincipal User user
    ) {
        driverService.updateDriverLocation(user.getId(), request);
        return ResponseEntity.ok(Map.of("message", "Location updated successfully"));
    }

    @PutMapping("/me/availability")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverResponse> updateMyAvailability(
            @Valid @RequestBody UpdateAvailabilityRequest request,
            @AuthenticationPrincipal User user
    ) {
        DriverResponse response = driverService.updateAvailability(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<DriverStatistics> getDriverStatistics(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        if (user.getRole() == User.Role.DRIVER) {
            Long driverId = user.getDriver().getId();
            if (!driverId.equals(id)) {
                return ResponseEntity.status(403).build();
            }
        }

        DriverStatistics stats = driverService.getDriverStatistics(id);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/me/statistics")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverStatistics> getMyStatistics(@AuthenticationPrincipal User user) {
        Long driverId = user.getDriver().getId();
        DriverStatistics stats = driverService.getDriverStatistics(driverId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/top")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<List<DriverResponse>> getTopDrivers(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<DriverResponse> drivers = driverService.getTopDrivers(limit);
        return ResponseEntity.ok(drivers);
    }
}