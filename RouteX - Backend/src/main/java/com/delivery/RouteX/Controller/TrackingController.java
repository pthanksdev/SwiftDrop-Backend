package com.delivery.RouteX.Controller;

import com.delivery.RouteX.dto.order.TrackingResponse;
import com.delivery.RouteX.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class TrackingController {

    private final OrderService orderService;

    @GetMapping("/{orderNumber}")
    public ResponseEntity<List<TrackingResponse>> trackOrder(@PathVariable String orderNumber) {
        List<TrackingResponse> tracking = orderService.getOrderTracking(orderNumber);
        return ResponseEntity.ok(tracking);
    }
}
