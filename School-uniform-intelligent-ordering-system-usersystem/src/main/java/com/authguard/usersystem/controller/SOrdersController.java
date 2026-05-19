package com.authguard.usersystem.controller;

import com.authguard.usersystem.dto.ApiResponse;
import com.authguard.usersystem.dto.CreateOrderRequestDto;
import com.authguard.usersystem.dto.OrderFulfillmentCommandDto;
import com.authguard.usersystem.dto.OrderStatusLogDto;
import com.authguard.usersystem.dto.UpdateOrderStatusDto;
import com.authguard.usersystem.entity.SOrders;
import com.authguard.usersystem.exception.BizException;
import com.authguard.usersystem.exception.ForbiddenException;
import com.authguard.usersystem.exception.NotFoundException;
import com.authguard.usersystem.security.CurrentUserResolver;
import com.authguard.usersystem.service.ISUserActivityLogService;
import com.authguard.usersystem.service.SOrdersService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/s-orders")
@RequiredArgsConstructor
public class SOrdersController {

    private static final Logger log = LoggerFactory.getLogger(SOrdersController.class);

    private final SOrdersService sOrdersService;
    private final CurrentUserResolver currentUserResolver;
    private final ISUserActivityLogService activityLogService;
    private final ObjectMapper objectMapper;

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<SOrders>>> getOrdersByCurrentUser(HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        List<SOrders> orders = sOrdersService.getOrdersByUserId(userId);
        activityLogService.recordActivityAsync(userId, "VIEW_MY_ORDERS", null, null, null, request);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<SOrders>> getOrderById(@PathVariable Long orderId, HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        SOrders order = sOrdersService.getOrderById(orderId);
        if (order == null) {
            throw new NotFoundException("订单未找到。");
        }
        if (!userId.equals(order.getUserId())) {
            throw new ForbiddenException("您无权查看此订单。");
        }
        activityLogService.recordActivityAsync(userId, "VIEW_ORDER_DETAIL", "ORDER", orderId.toString(), null, request);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<SOrders>> createOrder(@Valid @RequestBody CreateOrderRequestDto requestDto, HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        SOrders createdOrder = sOrdersService.createOrderFromCart(requestDto, userId);
        activityLogService.recordActivityAsync(
                userId,
                "PLACE_ORDER_SUCCESS",
                "ORDER",
                createdOrder.getId().toString(),
                createdOrder.getTotalPrice() == null ? null : "TotalPrice:" + createdOrder.getTotalPrice(),
                request
        );
        return ResponseEntity.status(201).body(ApiResponse.success(createdOrder));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(@PathVariable Long orderId,
                                                                @Valid @RequestBody UpdateOrderStatusDto statusDto,
                                                                HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        boolean result = sOrdersService.updateOrderStatus(
                orderId,
                statusDto.getStatus(),
                statusDto.getPaymentStatusCode(),
                statusDto.getShippingStatusCode(),
                String.valueOf(userId)
        );
        if (!result) {
            throw new BizException("订单状态修改失败或未找到订单。");
        }
        activityLogService.recordActivityAsync(userId, "UPDATE_ORDER_STATUS_SUCCESS", "ORDER", orderId.toString(), safeJson(statusDto), request);
        return ResponseEntity.ok(ApiResponse.success("订单状态修改成功", null));
    }

    @PostMapping("/{orderId}/fulfillment-commands")
    public ResponseEntity<ApiResponse<SOrders>> executeFulfillmentCommand(@PathVariable Long orderId,
                                                                          @Valid @RequestBody OrderFulfillmentCommandDto commandDto,
                                                                          HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        SOrders updatedOrder = sOrdersService.executeUserFulfillmentCommand(
                orderId,
                userId,
                commandDto.getCommand(),
                commandDto.getReason()
        );
        activityLogService.recordActivityAsync(userId, "ORDER_FULFILLMENT_COMMAND", "ORDER", orderId.toString(), safeJson(commandDto), request);
        return ResponseEntity.ok(ApiResponse.success("订单履约命令执行成功", updatedOrder));
    }

    @GetMapping("/{orderId}/status-logs")
    public ResponseEntity<ApiResponse<List<OrderStatusLogDto>>> getStatusLogs(@PathVariable Long orderId,
                                                                              HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        List<OrderStatusLogDto> logs = sOrdersService.getOrderStatusLogs(orderId, userId);
        activityLogService.recordActivityAsync(userId, "VIEW_ORDER_STATUS_LOGS", "ORDER", orderId.toString(), null, request);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    private String safeJson(Object value) {
        if (objectMapper == null || value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.debug("Failed to serialize order payload: {}", ex.getMessage());
            return null;
        }
    }
}
