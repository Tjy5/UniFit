package com.authguard.usersystem.service;

import com.authguard.usersystem.dto.CreateOrderRequestDto; // NEW DTO
import com.authguard.usersystem.dto.OrderStatusLogDto;
import com.authguard.usersystem.entity.SOrders;
import com.authguard.usersystem.order.FulfillmentCommand;
import java.util.List;

public interface SOrdersService {
    List<SOrders> getOrdersByUserId(Long userId);
    SOrders getOrderById(Long orderId); // NEW
    // boolean createOrder(SOrders order); // OLD METHOD SIGNATURE
    SOrders createOrderFromCart(CreateOrderRequestDto requestDto, Long userId); // NEW
    boolean updateOrderStatus(Long orderId, Long status, String paymentStatusCode, String shippingStatusCode, String updateBy); // Modified for more specific updates
    SOrders executeUserFulfillmentCommand(Long orderId, Long userId, FulfillmentCommand command, String reason);
    List<OrderStatusLogDto> getOrderStatusLogs(Long orderId, Long userId);
}
