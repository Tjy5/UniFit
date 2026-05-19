package com.authguard.usersystem.order;

import com.authguard.usersystem.entity.SOrders;

public record FulfillmentState(Long orderStatus, String paymentStatus, String shippingStatus) {

    public static FulfillmentState fromOrder(SOrders order) {
        return new FulfillmentState(order.getStatus(), order.getPaymentStatusCode(), order.getShippingStatusCode());
    }

    public SOrders toOrderUpdate(Long orderId, String updateBy) {
        SOrders order = new SOrders();
        order.setId(orderId);
        order.setStatus(orderStatus);
        order.setPaymentStatusCode(paymentStatus);
        order.setShippingStatusCode(shippingStatus);
        order.setUpdateBy(updateBy);
        order.setUpdateTime(new java.util.Date());
        return order;
    }
}
