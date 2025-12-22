package com.delivery.RouteX.Controller;


import com.delivery.RouteX.dto.order.*;
import com.delivery.RouteX.model.User;
import com.delivery.RouteX.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal User user
    ) {
        Long customerId = user.getCustomer().getId();
        OrderResponse response = orderService.createOrder(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        OrderResponse response = orderService.getOrderById(id, user.getId(), user.getRole());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByNumber(@PathVariable String orderNumber) {
        OrderResponse response = orderService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<OrderSummary>> getMyOrders(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Long customerId = user.getCustomer().getId();
        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<OrderSummary> orders = orderService.getCustomerOrders(customerId, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/driver/me")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Page<OrderSummary>> getMyDriverOrders(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long driverId = user.getDriver().getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<OrderSummary> orders = orderService.getDriverOrders(driverId, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/driver/me/active")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<OrderResponse>> getMyActiveOrders(
            @AuthenticationPrincipal User user
    ) {
        Long driverId = user.getDriver().getId();
        List<OrderResponse> orders = orderService.getActiveDriverOrders(driverId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<Page<OrderSummary>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<OrderSummary> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<Page<OrderSummary>> searchOrders(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderSummary> orders = orderService.searchOrders(query, pageable);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<OrderResponse> assignDriver(
            @PathVariable Long id,
            @Valid @RequestBody AssignDriverRequest request,
            @AuthenticationPrincipal User user
    ) {
        OrderResponse response = orderService.assignDriver(id, request, user.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @AuthenticationPrincipal User user
    ) {
        OrderResponse response = orderService.updateOrderStatus(id, request, user.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> cancelOrder(
            @PathVariable Long id,
            @Valid @RequestBody CancelOrderRequest request,
            @AuthenticationPrincipal User user
    ) {
        orderService.cancelOrder(id, request, user.getId(), user.getRole());
        return ResponseEntity.ok(Map.of("message", "Order cancelled successfully"));
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<Map<String, String>> rateOrder(
            @PathVariable Long id,
            @Valid @RequestBody RateOrderRequest request,
            @AuthenticationPrincipal User user
    ) {
        orderService.rateOrder(id, request, user.getId(), user.getRole());
        return ResponseEntity.ok(Map.of("message", "Rating submitted successfully"));
    }

    @PostMapping("/estimate-price")
    public ResponseEntity<PriceEstimateResponse> estimatePrice(
            @Valid @RequestBody PriceEstimateRequest request
    ) {
        PriceEstimateResponse response = orderService.estimatePrice(request);
        return ResponseEntity.ok(response);
    }
}

