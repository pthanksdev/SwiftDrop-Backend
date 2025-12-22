package com.delivery.RouteX.service;

import com.delivery.RouteX.Repository.*;
import com.delivery.RouteX.dto.analytics.*;
import com.delivery.RouteX.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final DriverRepository driverRepository;

    @Transactional(readOnly = true)
    public DashboardMetrics getDashboardMetrics() {
        Long activeDeliveries = orderRepository.countByStatus(Order.OrderStatus.IN_TRANSIT) +
                orderRepository.countByStatus(Order.OrderStatus.PICKED_UP) +
                orderRepository.countByStatus(Order.OrderStatus.ASSIGNED);

        Long pendingOrders = orderRepository.countByStatus(Order.OrderStatus.PENDING);

        LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        Long ordersToday = orderRepository.countOrdersInDateRange(startOfDay, endOfDay);

        Double revenueToday = orderRepository.getTotalRevenueInDateRange(startOfDay, endOfDay);
        if (revenueToday == null) revenueToday = 0.0;

        Long availableDrivers = (long) driverRepository
                .findByAvailabilityStatus(com.delivery.model.Driver.AvailabilityStatus.ONLINE)
                .size();

        Double avgDeliveryTime = 45.0;
        Double successRate = calculateSuccessRate();

        return DashboardMetrics.builder()
                .activeDeliveries(activeDeliveries)
                .pendingOrders(pendingOrders)
                .ordersToday(ordersToday)
                .revenueToday(revenueToday)
                .availableDrivers(availableDrivers)
                .averageDeliveryTime(avgDeliveryTime)
                .successRate(successRate)
                .build();
    }

    @Transactional(readOnly = true)
    public RevenueReport getRevenueReport(LocalDateTime startDate, LocalDateTime endDate) {
        Double totalRevenue = orderRepository.getTotalRevenueInDateRange(startDate, endDate);
        if (totalRevenue == null) totalRevenue = 0.0;

        Long totalOrders = orderRepository.countOrdersInDateRange(startDate, endDate);
        Double averageOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0.0;

        Map<String, Double> dailyRevenue = new HashMap<>();
        LocalDateTime current = startDate;
        while (current.isBefore(endDate)) {
            LocalDateTime nextDay = current.plusDays(1);
            Double dayRevenue = orderRepository.getTotalRevenueInDateRange(current, nextDay);
            dailyRevenue.put(current.toLocalDate().toString(), dayRevenue != null ? dayRevenue : 0.0);
            current = nextDay;
        }

        return RevenueReport.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .averageOrderValue(averageOrderValue)
                .startDate(startDate)
                .endDate(endDate)
                .dailyBreakdown(dailyRevenue)
                .build();
    }

    @Transactional(readOnly = true)
    public OrderStatistics getOrderStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        Long totalOrders = orderRepository.countOrdersInDateRange(startDate, endDate);

        Long completed = orderRepository.findByStatusAndDateRange(
                Order.OrderStatus.DELIVERED, startDate, endDate
        ).size();

        Long cancelled = orderRepository.findByStatusAndDateRange(
                Order.OrderStatus.CANCELLED, startDate, endDate
        ).size();

        Long failed = orderRepository.findByStatusAndDateRange(
                Order.OrderStatus.FAILED, startDate, endDate
        ).size();

        Double completionRate = totalOrders > 0 ? (completed * 100.0) / totalOrders : 0.0;
        Double cancellationRate = totalOrders > 0 ? (cancelled * 100.0) / totalOrders : 0.0;

        return OrderStatistics.builder()
                .totalOrders(totalOrders)
                .completedOrders(completed)
                .cancelledOrders(cancelled)
                .failedOrders(failed)
                .completionRate(completionRate)
                .cancellationRate(cancellationRate)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private Double calculateSuccessRate() {
        Long total = orderRepository.count();
        if (total == 0) return 100.0;

        Long delivered = orderRepository.countByStatus(Order.OrderStatus.DELIVERED);
        return (delivered * 100.0) / total;
    }
}
