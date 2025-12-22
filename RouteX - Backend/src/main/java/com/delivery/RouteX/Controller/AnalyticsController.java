package com.delivery.RouteX.Controller;

import com.delivery.RouteX.dto.analytics.DashboardMetrics;
import com.delivery.RouteX.dto.analytics.OrderStatistics;
import com.delivery.RouteX.dto.analytics.RevenueReport;
import com.delivery.RouteX.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardMetrics> getDashboardMetrics() {
        DashboardMetrics metrics = analyticsService.getDashboardMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/revenue")
    public ResponseEntity<RevenueReport> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        RevenueReport report = analyticsService.getRevenueReport(startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/orders")
    public ResponseEntity<OrderStatistics> getOrderStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        OrderStatistics stats = analyticsService.getOrderStatistics(startDate, endDate);
        return ResponseEntity.ok(stats);
    }
}
