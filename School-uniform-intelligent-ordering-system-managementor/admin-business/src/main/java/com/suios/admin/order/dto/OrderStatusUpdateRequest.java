package com.suios.admin.order.dto;

import lombok.Data;

@Data
public class OrderStatusUpdateRequest {

    private Long status;

    private String paymentStatus;

    private String shippingStatus;
}
