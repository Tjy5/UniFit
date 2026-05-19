package com.suios.admin.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.security.AuthContext;
import com.suios.admin.inventory.entity.InventoryMovement;
import com.suios.admin.inventory.service.InventoryMovementService;
import com.suios.admin.order.dto.OrderAnomalySummary;
import com.suios.admin.order.dto.OrderFulfillmentCommandRequest;
import com.suios.admin.order.dto.OrderStatusLogDto;
import com.suios.admin.order.dto.OrderStatusUpdateRequest;
import com.suios.admin.order.entity.SOrderStatusLog;
import com.suios.admin.order.entity.SOrders;
import com.suios.admin.order.mapper.SOrderItemMapper;
import com.suios.admin.order.mapper.SOrderStatusLogMapper;
import com.suios.admin.order.mapper.SOrdersMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SOrdersServiceTest {

    @Mock
    private SOrdersMapper ordersMapper;

    @Mock
    private SOrderItemMapper orderItemMapper;

    @Mock
    private SOrderStatusLogMapper statusLogMapper;

    @Mock
    private AuthContext authContext;

    @Mock
    private OrderAnomalyService anomalyService;

    @Mock
    private InventoryMovementService inventoryMovementService;

    private SOrdersService service;

    @BeforeEach
    void setUp() {
        service = new SOrdersService(ordersMapper, orderItemMapper, statusLogMapper, authContext, anomalyService, inventoryMovementService);
    }

    @Test
    void shouldExecuteShipmentCommandAndWriteAudit() {
        SOrders current = order(700L, 1L, "PAID", "NOT_SHIPPED");
        SOrders updated = order(700L, 2L, "PAID", "SHIPPED");
        OrderFulfillmentCommandRequest request = new OrderFulfillmentCommandRequest();
        request.setCommand(com.suios.admin.common.order.FulfillmentCommand.SHIP_ORDER);
        request.setReason("发货");
        when(ordersMapper.selectDetailById(700L)).thenReturn(current, updated);
        when(orderItemMapper.selectByOrderId(700L)).thenReturn(List.of());
        when(anomalyService.summarize(org.mockito.ArgumentMatchers.<SOrders>any())).thenReturn(new OrderAnomalySummary());
        when(authContext.getCurrentUserId()).thenReturn(1L);
        when(ordersMapper.updateById(org.mockito.ArgumentMatchers.<SOrders>any())).thenReturn(1);
        when(statusLogMapper.insert(org.mockito.ArgumentMatchers.<SOrderStatusLog>any())).thenReturn(1);

        SOrders response = service.executeCommand(700L, request);

        assertEquals(2L, response.getStatus());
        verify(ordersMapper).updateById(org.mockito.ArgumentMatchers.<SOrders>any());
        verify(statusLogMapper).insert(org.mockito.ArgumentMatchers.<SOrderStatusLog>any());
    }

    @Test
    void shouldExecuteReceiptConfirmationCommand() {
        SOrders current = order(705L, 2L, "PAID", "SHIPPED");
        SOrders updated = order(705L, 3L, "PAID", "DELIVERED");
        OrderFulfillmentCommandRequest request = new OrderFulfillmentCommandRequest();
        request.setCommand(com.suios.admin.common.order.FulfillmentCommand.CONFIRM_RECEIPT);
        when(ordersMapper.selectDetailById(705L)).thenReturn(current, updated);
        when(orderItemMapper.selectByOrderId(705L)).thenReturn(List.of());
        when(anomalyService.summarize(org.mockito.ArgumentMatchers.<SOrders>any())).thenReturn(new OrderAnomalySummary());
        when(ordersMapper.updateById(org.mockito.ArgumentMatchers.<SOrders>any())).thenReturn(1);
        when(statusLogMapper.insert(org.mockito.ArgumentMatchers.<SOrderStatusLog>any())).thenReturn(1);

        SOrders response = service.executeCommand(705L, request);

        assertEquals(3L, response.getStatus());
        assertEquals("DELIVERED", response.getShippingStatus());
    }

    @Test
    void shouldApplyOutboundInventoryWhenShippingOrder() {
        SOrders current = order(708L, 1L, "PAID", "NOT_SHIPPED");
        SOrders updated = order(708L, 2L, "PAID", "SHIPPED");
        InventoryMovement reserve = new InventoryMovement();
        reserve.setSkuId(10L);
        reserve.setQuantity(2L);
        reserve.setRelatedOrderItemId(900L);
        OrderFulfillmentCommandRequest request = new OrderFulfillmentCommandRequest();
        request.setCommand(com.suios.admin.common.order.FulfillmentCommand.SHIP_ORDER);
        when(ordersMapper.selectDetailById(708L)).thenReturn(current, updated);
        when(orderItemMapper.selectByOrderId(708L)).thenReturn(List.of());
        when(anomalyService.summarize(org.mockito.ArgumentMatchers.<SOrders>any())).thenReturn(new OrderAnomalySummary());
        when(authContext.getCurrentUserId()).thenReturn(1L);
        when(ordersMapper.updateById(org.mockito.ArgumentMatchers.<SOrders>any())).thenReturn(1);
        when(statusLogMapper.insert(org.mockito.ArgumentMatchers.<SOrderStatusLog>any())).thenReturn(1);
        when(inventoryMovementService.hasOrderMovement(708L, "OUTBOUND")).thenReturn(false);
        when(inventoryMovementService.orderMovements(708L, "RESERVE")).thenReturn(List.of(reserve));

        service.executeCommand(708L, request);

        verify(inventoryMovementService).apply(10L, "OUTBOUND", 2L, 708L, 900L, "ADMIN", 1L, "订单发货出库");
    }

    @Test
    void shouldSkipOutboundInventoryWhenShipmentAlreadyConsumed() {
        SOrders current = order(710L, 1L, "PAID", "NOT_SHIPPED");
        SOrders updated = order(710L, 2L, "PAID", "SHIPPED");
        OrderFulfillmentCommandRequest request = new OrderFulfillmentCommandRequest();
        request.setCommand(com.suios.admin.common.order.FulfillmentCommand.SHIP_ORDER);
        when(ordersMapper.selectDetailById(710L)).thenReturn(current, updated);
        when(orderItemMapper.selectByOrderId(710L)).thenReturn(List.of());
        when(anomalyService.summarize(org.mockito.ArgumentMatchers.<SOrders>any())).thenReturn(new OrderAnomalySummary());
        when(ordersMapper.updateById(org.mockito.ArgumentMatchers.<SOrders>any())).thenReturn(1);
        when(statusLogMapper.insert(org.mockito.ArgumentMatchers.<SOrderStatusLog>any())).thenReturn(1);
        when(inventoryMovementService.hasOrderMovement(710L, "OUTBOUND")).thenReturn(true);

        service.executeCommand(710L, request);

        verify(inventoryMovementService, never()).orderMovements(710L, "RESERVE");
        verify(inventoryMovementService, never()).apply(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void shouldReleaseReservedInventoryWhenApprovingRefundBeforeShipment() {
        SOrders current = order(709L, 5L, "REFUNDING", "NOT_SHIPPED");
        SOrders updated = order(709L, 6L, "REFUNDED", "NOT_SHIPPED");
        InventoryMovement reserve = new InventoryMovement();
        reserve.setSkuId(10L);
        reserve.setQuantity(2L);
        reserve.setRelatedOrderItemId(900L);
        OrderFulfillmentCommandRequest request = new OrderFulfillmentCommandRequest();
        request.setCommand(com.suios.admin.common.order.FulfillmentCommand.APPROVE_REFUND);
        when(ordersMapper.selectDetailById(709L)).thenReturn(current, updated);
        when(orderItemMapper.selectByOrderId(709L)).thenReturn(List.of());
        when(anomalyService.summarize(org.mockito.ArgumentMatchers.<SOrders>any())).thenReturn(new OrderAnomalySummary());
        when(authContext.getCurrentUserId()).thenReturn(1L);
        when(ordersMapper.updateById(org.mockito.ArgumentMatchers.<SOrders>any())).thenReturn(1);
        when(statusLogMapper.insert(org.mockito.ArgumentMatchers.<SOrderStatusLog>any())).thenReturn(1);
        when(inventoryMovementService.hasOrderMovement(709L, "RELEASE")).thenReturn(false);
        when(inventoryMovementService.orderMovements(709L, "RESERVE")).thenReturn(List.of(reserve));

        service.executeCommand(709L, request);

        verify(inventoryMovementService).apply(10L, "RELEASE", 2L, 709L, 900L, "ADMIN", 1L, "未发货退款释放库存");
    }

    @Test
    void shouldSkipReleaseInventoryWhenRefundAlreadyReleased() {
        SOrders current = order(711L, 5L, "REFUNDING", "NOT_SHIPPED");
        SOrders updated = order(711L, 6L, "REFUNDED", "NOT_SHIPPED");
        OrderFulfillmentCommandRequest request = new OrderFulfillmentCommandRequest();
        request.setCommand(com.suios.admin.common.order.FulfillmentCommand.APPROVE_REFUND);
        when(ordersMapper.selectDetailById(711L)).thenReturn(current, updated);
        when(orderItemMapper.selectByOrderId(711L)).thenReturn(List.of());
        when(anomalyService.summarize(org.mockito.ArgumentMatchers.<SOrders>any())).thenReturn(new OrderAnomalySummary());
        when(ordersMapper.updateById(org.mockito.ArgumentMatchers.<SOrders>any())).thenReturn(1);
        when(statusLogMapper.insert(org.mockito.ArgumentMatchers.<SOrderStatusLog>any())).thenReturn(1);
        when(inventoryMovementService.hasOrderMovement(711L, "RELEASE")).thenReturn(true);

        service.executeCommand(711L, request);

        verify(inventoryMovementService, never()).orderMovements(711L, "RESERVE");
        verify(inventoryMovementService, never()).apply(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void shouldExecuteRefundApprovalCommand() {
        SOrders current = order(706L, 5L, "REFUNDING", "RETURN_REQUESTED");
        SOrders updated = order(706L, 6L, "REFUNDED", "RETURNED");
        OrderFulfillmentCommandRequest request = new OrderFulfillmentCommandRequest();
        request.setCommand(com.suios.admin.common.order.FulfillmentCommand.APPROVE_REFUND);
        when(ordersMapper.selectDetailById(706L)).thenReturn(current, updated);
        when(orderItemMapper.selectByOrderId(706L)).thenReturn(List.of());
        when(anomalyService.summarize(org.mockito.ArgumentMatchers.<SOrders>any())).thenReturn(new OrderAnomalySummary());
        when(ordersMapper.updateById(org.mockito.ArgumentMatchers.<SOrders>any())).thenReturn(1);
        when(statusLogMapper.insert(org.mockito.ArgumentMatchers.<SOrderStatusLog>any())).thenReturn(1);

        SOrders response = service.executeCommand(706L, request);

        assertEquals(6L, response.getStatus());
        assertEquals("REFUNDED", response.getPaymentStatus());
    }

    @Test
    void shouldExecuteRefundRejectionCommandUsingAuditBaseline() {
        SOrders current = order(707L, 5L, "REFUNDING", "RETURN_REQUESTED");
        SOrders updated = order(707L, 2L, "PAID", "SHIPPED");
        SOrderStatusLog refundRequestLog = new SOrderStatusLog();
        refundRequestLog.setFromOrderStatus(2L);
        refundRequestLog.setFromPaymentStatus("PAID");
        refundRequestLog.setFromShippingStatus("SHIPPED");
        OrderFulfillmentCommandRequest request = new OrderFulfillmentCommandRequest();
        request.setCommand(com.suios.admin.common.order.FulfillmentCommand.REJECT_REFUND);
        when(ordersMapper.selectDetailById(707L)).thenReturn(current, updated);
        when(orderItemMapper.selectByOrderId(707L)).thenReturn(List.of());
        when(anomalyService.summarize(org.mockito.ArgumentMatchers.<SOrders>any())).thenReturn(new OrderAnomalySummary());
        when(statusLogMapper.selectLatestByOrderIdAndEventType(707L, "REQUEST_REFUND")).thenReturn(refundRequestLog);
        when(ordersMapper.updateById(org.mockito.ArgumentMatchers.<SOrders>any())).thenReturn(1);
        when(statusLogMapper.insert(org.mockito.ArgumentMatchers.<SOrderStatusLog>any())).thenReturn(1);

        SOrders response = service.executeCommand(707L, request);

        assertEquals(2L, response.getStatus());
        assertEquals("SHIPPED", response.getShippingStatus());
    }

    @Test
    void shouldRejectIllegalAdminCommandWithoutUpdate() {
        SOrders current = order(701L, 0L, "PENDING", "NOT_SHIPPED");
        OrderFulfillmentCommandRequest request = new OrderFulfillmentCommandRequest();
        request.setCommand(com.suios.admin.common.order.FulfillmentCommand.SHIP_ORDER);
        when(ordersMapper.selectDetailById(701L)).thenReturn(current);
        when(orderItemMapper.selectByOrderId(701L)).thenReturn(List.of());
        when(anomalyService.summarize(current)).thenReturn(new OrderAnomalySummary());

        assertThrows(BizException.class, () -> service.executeCommand(701L, request));

        verify(ordersMapper, never()).updateById(org.mockito.ArgumentMatchers.<SOrders>any());
        verify(statusLogMapper, never()).insert(org.mockito.ArgumentMatchers.<SOrderStatusLog>any());
    }

    @Test
    void shouldMapLegacyStatusPatchToCommand() {
        SOrders current = order(702L, 1L, "PAID", "NOT_SHIPPED");
        when(ordersMapper.selectDetailById(702L)).thenReturn(current);
        when(orderItemMapper.selectByOrderId(702L)).thenReturn(List.of());
        when(anomalyService.summarize(current)).thenReturn(new OrderAnomalySummary());
        when(authContext.getCurrentUserId()).thenReturn(1L);
        when(ordersMapper.updateById(org.mockito.ArgumentMatchers.<SOrders>any())).thenReturn(1);
        when(statusLogMapper.insert(org.mockito.ArgumentMatchers.<SOrderStatusLog>any())).thenReturn(1);
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest();
        request.setStatus(2L);
        request.setShippingStatus("SHIPPED");

        service.updateStatus(702L, request);

        verify(ordersMapper).updateById(org.mockito.ArgumentMatchers.<SOrders>any());
        verify(statusLogMapper).insert(org.mockito.ArgumentMatchers.<SOrderStatusLog>any());
    }

    @Test
    void shouldExposeStatusLogs() {
        SOrderStatusLog log = new SOrderStatusLog();
        log.setId(1L);
        log.setOrderId(703L);
        log.setEventType("SHIP_ORDER");
        log.setCreateTime(LocalDateTime.now());
        when(ordersMapper.selectById(703L)).thenReturn(order(703L, 2L, "PAID", "SHIPPED"));
        when(statusLogMapper.selectByOrderId(703L)).thenReturn(List.of(log));

        List<OrderStatusLogDto> logs = service.statusLogs(703L);

        assertEquals(1, logs.size());
        assertEquals("SHIP_ORDER", logs.getFirst().getEventType());
    }

    @Test
    void shouldFailWhenAuditWriteFailsAfterOrderUpdate() {
        SOrders current = order(704L, 1L, "PAID", "NOT_SHIPPED");
        OrderFulfillmentCommandRequest request = new OrderFulfillmentCommandRequest();
        request.setCommand(com.suios.admin.common.order.FulfillmentCommand.SHIP_ORDER);
        when(ordersMapper.selectDetailById(704L)).thenReturn(current);
        when(orderItemMapper.selectByOrderId(704L)).thenReturn(List.of());
        when(anomalyService.summarize(current)).thenReturn(new OrderAnomalySummary());
        when(ordersMapper.updateById(org.mockito.ArgumentMatchers.<SOrders>any())).thenReturn(1);
        when(statusLogMapper.insert(org.mockito.ArgumentMatchers.<SOrderStatusLog>any())).thenReturn(0);

        assertThrows(BizException.class, () -> service.executeCommand(704L, request));
    }

    private SOrders order(Long id, Long status, String paymentStatus, String shippingStatus) {
        SOrders order = new SOrders();
        order.setId(id);
        order.setStatus(status);
        order.setPaymentStatus(paymentStatus);
        order.setShippingStatus(shippingStatus);
        return order;
    }
}
