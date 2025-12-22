package com.delivery.RouteX.dto.order;

import com.delivery.RouteX.model.Order;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {
    @NotNull
    private Order.OrderStatus status;
    private String notes;
    private String deliverySignatureUrl;
    private String deliveryPhotoUrl;
    private String deliveryNotes;
}
