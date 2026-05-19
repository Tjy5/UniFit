package com.suios.admin.common.order;

public record FulfillmentState(Long orderStatus, String paymentStatus, String shippingStatus) {
}
