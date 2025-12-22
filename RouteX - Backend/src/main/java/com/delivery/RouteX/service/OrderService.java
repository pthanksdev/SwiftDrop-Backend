package com.delivery.RouteX.service;

import com.delivery.RouteX.Repository.*;
import com.delivery.RouteX.dto.order.*;
import com.delivery.RouteX.exception.*;
import com.delivery.RouteX.model.*;
import com.delivery.RouteX.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final DriverRepository driverRepository;
    private final OrderTrackingRepository trackingRepository;
    private final NotificationService notificationService;
    private final DistanceCalculator distanceCalculator;
    private final PricingCalculator pricingCalculator;

    @Transactional
    public OrderResponse createOrder(Long customerId, CreateOrderRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        double distance = distanceCalculator.calculateDistance(
                request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDeliveryLatitude(), request.getDeliveryLongitude()
        );

        PriceEstimateResponse pricing = pricingCalculator.calculatePrice(
                distance, request.getPackageWeight(), request.getPromoCode()
        );

        Order order = Order.builder()
                .customer(customer)
                .status(Order.OrderStatus.PENDING)
                .pickupAddress(request.getPickupAddress())
                .pickupLatitude(request.getPickupLatitude())
                .pickupLongitude(request.getPickupLongitude())
                .pickupContactName(request.getPickupContactName())
                .pickupContactPhone(request.getPickupContactPhone())
                .deliveryAddress(request.getDeliveryAddress())
                .deliveryLatitude(request.getDeliveryLatitude())
                .deliveryLongitude(request.getDeliveryLongitude())
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .packageType(request.getPackageType())
                .packageWeight(request.getPackageWeight())
                .packageDimensions(request.getPackageDimensions())
                .specialInstructions(request.getSpecialInstructions())
                .scheduledPickupTime(request.getScheduledPickupTime())
                .scheduledDeliveryTime(request.getScheduledDeliveryTime())
                .distanceKm(pricing.getDistanceKm())
                .baseFare(pricing.getBaseFare())
                .distanceCharge(pricing.getDistanceCharge())
                .weightCharge(pricing.getWeightCharge())
                .peakHourSurcharge(pricing.getPeakHourSurcharge())
                .discount(pricing.getDiscount())
                .totalAmount(pricing.getTotalAmount())
                .promoCode(request.getPromoCode())
                .build();

        order = orderRepository.save(order);
        customer.incrementOrders();
        customerRepository.save(customer);
        notificationService.sendOrderCreatedNotification(order);

        log.info("Order created: {} for customer: {}",
                order.getOrderNumber(), customer.getUser().getEmail());

        return mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId, User.Role role) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        validateOrderAccess(order, userId, role);
        return mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummary> getCustomerOrders(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable)
                .map(this::mapToOrderSummary);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummary> getDriverOrders(Long driverId, Pageable pageable) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        return orderRepository.findByDriverId(driverId, pageable)
                .map(this::mapToOrderSummary);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getActiveDriverOrders(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        return orderRepository.findActiveOrdersByDriver(driverId)
                .stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<OrderSummary> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::mapToOrderSummary);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummary> searchOrders(String searchTerm, Pageable pageable) {
        return orderRepository.searchOrders(searchTerm, pageable)
                .map(this::mapToOrderSummary);
    }

    @Transactional
    public OrderResponse assignDriver(Long orderId, AssignDriverRequest request, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.canBeAssigned()) {
            throw new BadRequestException("Order cannot be assigned in current status");
        }

        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        if (!driver.isAvailable()) {
            throw new BadRequestException("Driver is not available");
        }

        order.assignDriver(driver);
        driver.setAvailabilityStatus(Driver.AvailabilityStatus.BUSY);

        orderRepository.save(order);
        driverRepository.save(driver);
        notificationService.sendDriverAssignedNotification(order);

        log.info("Driver {} assigned to order {}", driver.getId(), order.getOrderNumber());
        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Order.OrderStatus newStatus = request.getStatus();
        validateStatusTransition(order, newStatus);

        switch (newStatus) {
            case PICKED_UP:
                order.markPickedUp();
                break;
            case IN_TRANSIT:
                order.markInTransit();
                break;
            case DELIVERED:
                order.markDelivered(
                        request.getDeliverySignatureUrl(),
                        request.getDeliveryPhotoUrl(),
                        request.getDeliveryNotes()
                );
                if (order.getDriver() != null) {
                    Driver driver = order.getDriver();
                    driver.incrementDeliveries();
                    driver.setAvailabilityStatus(Driver.AvailabilityStatus.ONLINE);
                    driverRepository.save(driver);
                }
                break;
            case FAILED:
                order.markFailed(request.getNotes());
                if (order.getDriver() != null) {
                    order.getDriver().setAvailabilityStatus(Driver.AvailabilityStatus.ONLINE);
                }
                break;
            default:
                throw new BadRequestException("Invalid status transition");
        }

        orderRepository.save(order);
        notificationService.sendOrderStatusUpdateNotification(order);

        log.info("Order {} status updated to {}", order.getOrderNumber(), newStatus);
        return mapToOrderResponse(order);
    }

    @Transactional
    public void cancelOrder(Long orderId, CancelOrderRequest request, Long userId, User.Role role) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        validateOrderAccess(order, userId, role);

        if (!order.canBeCancelled()) {
            throw new BadRequestException("Order cannot be cancelled in current status");
        }

        order.cancel(request.getReason());
        orderRepository.save(order);

        if (order.getDriver() != null) {
            Driver driver = order.getDriver();
            driver.incrementCancellations();
            driver.setAvailabilityStatus(Driver.AvailabilityStatus.ONLINE);
            driverRepository.save(driver);
        }

        notificationService.sendOrderCancelledNotification(order);
        log.info("Order {} cancelled by user {}", order.getOrderNumber(), userId);
    }

    @Transactional
    public void rateOrder(Long orderId, RateOrderRequest request, Long userId, User.Role role) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != Order.OrderStatus.DELIVERED) {
            throw new BadRequestException("Can only rate delivered orders");
        }

        if (role == User.Role.CUSTOMER) {
            order.setCustomerRating(request.getRating());
            order.setCustomerFeedback(request.getFeedback());

            if (order.getDriver() != null) {
                Driver driver = order.getDriver();
                driver.updateRating(request.getRating());
                driverRepository.save(driver);
            }
        } else if (role == User.Role.DRIVER) {
            order.setDriverRating(request.getRating());
            order.setDriverFeedback(request.getFeedback());
        }

        orderRepository.save(order);
        log.info("Order {} rated by {} with rating {}",
                order.getOrderNumber(), role, request.getRating());
    }

    @Transactional(readOnly = true)
    public List<TrackingResponse> getOrderTracking(String orderNumber) {
        List<OrderTracking> trackingHistory =
                trackingRepository.findByOrderOrderNumberOrderByTimestampDesc(orderNumber);

        return trackingHistory.stream()
                .map(this::mapToTrackingResponse)
                .collect(Collectors.toList());
    }

    public PriceEstimateResponse estimatePrice(PriceEstimateRequest request) {
        double distance = distanceCalculator.calculateDistance(
                request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDeliveryLatitude(), request.getDeliveryLongitude()
        );

        return pricingCalculator.calculatePrice(
                distance, request.getPackageWeight(), request.getPromoCode()
        );
    }

    private void validateOrderAccess(Order order, Long userId, User.Role role) {
        if (role == User.Role.CUSTOMER && !order.getCustomer().getUser().getId().equals(userId)) {
            throw new BadRequestException("Access denied to this order");
        }
        if (role == User.Role.DRIVER &&
                (order.getDriver() == null || !order.getDriver().getUser().getId().equals(userId))) {
            throw new BadRequestException("Access denied to this order");
        }
    }

    private void validateStatusTransition(Order order, Order.OrderStatus newStatus) {
        Order.OrderStatus currentStatus = order.getStatus();

        boolean validTransition = switch (currentStatus) {
            case PENDING -> newStatus == Order.OrderStatus.ASSIGNED ||
                    newStatus == Order.OrderStatus.CANCELLED;
            case ASSIGNED -> newStatus == Order.OrderStatus.PICKED_UP ||
                    newStatus == Order.OrderStatus.CANCELLED;
            case PICKED_UP -> newStatus == Order.OrderStatus.IN_TRANSIT ||
                    newStatus == Order.OrderStatus.FAILED;
            case IN_TRANSIT -> newStatus == Order.OrderStatus.DELIVERED ||
                    newStatus == Order.OrderStatus.FAILED;
            default -> false;
        };

        if (!validTransition) {
            throw new BadRequestException(
                    "Cannot transition from " + currentStatus + " to " + newStatus
            );
        }
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse.OrderResponseBuilder builder = OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getUser().getFirstName() + " " +
                        order.getCustomer().getUser().getLastName())
                .customerPhone(order.getCustomer().getUser().getPhone())
                .pickupAddress(order.getPickupAddress())
                .pickupLatitude(order.getPickupLatitude())
                .pickupLongitude(order.getPickupLongitude())
                .pickupContactName(order.getPickupContactName())
                .pickupContactPhone(order.getPickupContactPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryLatitude(order.getDeliveryLatitude())
                .deliveryLongitude(order.getDeliveryLongitude())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .packageType(order.getPackageType())
                .packageWeight(order.getPackageWeight())
                .packageDimensions(order.getPackageDimensions())
                .specialInstructions(order.getSpecialInstructions())
                .scheduledPickupTime(order.getScheduledPickupTime())
                .actualPickupTime(order.getActualPickupTime())
                .scheduledDeliveryTime(order.getScheduledDeliveryTime())
                .actualDeliveryTime(order.getActualDeliveryTime())
                .createdAt(order.getCreatedAt())
                .distanceKm(order.getDistanceKm())
                .baseFare(order.getBaseFare())
                .distanceCharge(order.getDistanceCharge())
                .weightCharge(order.getWeightCharge())
                .peakHourSurcharge(order.getPeakHourSurcharge())
                .discount(order.getDiscount())
                .totalAmount(order.getTotalAmount())
                .promoCode(order.getPromoCode())
                .deliverySignatureUrl(order.getDeliverySignatureUrl())
                .deliveryPhotoUrl(order.getDeliveryPhotoUrl())
                .deliveryNotes(order.getDeliveryNotes())
                .customerRating(order.getCustomerRating())
                .customerFeedback(order.getCustomerFeedback());

        if (order.getDriver() != null) {
            Driver driver = order.getDriver();
            builder.driverId(driver.getId())
                    .driverName(driver.getUser().getFirstName() + " " +
                            driver.getUser().getLastName())
                    .driverPhone(driver.getUser().getPhone())
                    .driverRating(driver.getRating());
        }

        List<TrackingResponse> tracking = order.getTrackingHistory().stream()
                .map(this::mapToTrackingResponse)
                .collect(Collectors.toList());
        builder.trackingHistory(tracking);

        return builder.build();
    }

    private OrderSummary mapToOrderSummary(Order order) {
        return OrderSummary.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .pickupAddress(order.getPickupAddress())
                .deliveryAddress(order.getDeliveryAddress())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .driverName(order.getDriver() != null ?
                        order.getDriver().getUser().getFirstName() + " " +
                                order.getDriver().getUser().getLastName() : null)
                .build();
    }

    private TrackingResponse mapToTrackingResponse(OrderTracking tracking) {
        return TrackingResponse.builder()
                .id(tracking.getId())
                .status(tracking.getStatus())
                .latitude(tracking.getLatitude())
                .longitude(tracking.getLongitude())
                .notes(tracking.getNotes())
                .timestamp(tracking.getTimestamp())
                .build();
    }
}
