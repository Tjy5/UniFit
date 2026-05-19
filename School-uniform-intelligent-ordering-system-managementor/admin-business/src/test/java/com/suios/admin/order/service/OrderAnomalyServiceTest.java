package com.suios.admin.order.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.suios.admin.order.dto.OrderAnomalySummary;
import com.suios.admin.order.entity.SOrderItem;
import com.suios.admin.order.entity.SOrders;
import com.suios.admin.order.mapper.SOrderItemMapper;
import com.suios.admin.order.mapper.SOrderStatusLogMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderAnomalyServiceTest {

    @Mock
    private SOrderItemMapper orderItemMapper;

    @Mock
    private SOrderStatusLogMapper statusLogMapper;

    @Test
    void shouldReturnNoFlagsForNormalOrder() {
        OrderAnomalyService service = new OrderAnomalyService(orderItemMapper, statusLogMapper);
        SOrders order = order(10L, 1L, "PAID", "NOT_SHIPPED", "张三", "100.00", LocalDateTime.now());
        order.setOrderItems(List.of(item("100.00")));
        when(statusLogMapper.countByOrderId(10L)).thenReturn(1);

        OrderAnomalySummary summary = service.summarize(order);

        assertFalse(summary.isAbnormal());
    }

    @Test
    void shouldDetectInvalidCombinationAndIntegrityIssues() {
        OrderAnomalyService service = new OrderAnomalyService(orderItemMapper, statusLogMapper);
        SOrders order = order(11L, 3L, "PAID", "SHIPPED", null, "120.00", LocalDateTime.now().minusDays(1));
        when(orderItemMapper.selectByOrderId(11L)).thenReturn(List.of(item("100.00")));
        when(statusLogMapper.countByOrderId(11L)).thenReturn(0);

        OrderAnomalySummary summary = service.summarize(order);

        assertTrue(summary.isAbnormal());
        assertTrue(summary.getReasons().contains("状态组合不符合订单履约状态机"));
        assertTrue(summary.getReasons().contains("订单金额与订单项小计不一致"));
        assertTrue(summary.getReasons().contains("订单缺少有效配送地址"));
        assertTrue(summary.getReasons().contains("缺少订单状态审计基线"));
    }

    @Test
    void shouldDetectStaleStates() {
        OrderAnomalyService service = new OrderAnomalyService(orderItemMapper, statusLogMapper);
        SOrders order = order(12L, 2L, "PAID", "SHIPPED", "李四", "50.00", LocalDateTime.now().minusDays(20));
        order.setOrderItems(List.of(item("50.00")));
        when(statusLogMapper.countByOrderId(12L)).thenReturn(1);

        OrderAnomalySummary summary = service.summarize(order);

        assertTrue(summary.isAbnormal());
        assertTrue(summary.getReasons().contains("已发货待签收超时"));
    }

    @Test
    void shouldDetectMissingOrderItemsAndWaitShipTimeout() {
        OrderAnomalyService service = new OrderAnomalyService(orderItemMapper, statusLogMapper);
        SOrders order = order(13L, 1L, "PAID", "NOT_SHIPPED", "王五", "50.00", LocalDateTime.now().minusDays(9));
        when(orderItemMapper.selectByOrderId(13L)).thenReturn(List.of());
        when(statusLogMapper.countByOrderId(13L)).thenReturn(1);

        OrderAnomalySummary summary = service.summarize(order);

        assertTrue(summary.isAbnormal());
        assertTrue(summary.getReasons().contains("待发货超时"));
        assertTrue(summary.getReasons().contains("订单缺少订单项"));
    }

    @Test
    void shouldDetectRefundingTimeout() {
        OrderAnomalyService service = new OrderAnomalyService(orderItemMapper, statusLogMapper);
        SOrders order = order(14L, 5L, "REFUNDING", "RETURN_REQUESTED", "赵六", "50.00", LocalDateTime.now().minusDays(8));
        order.setOrderItems(List.of(item("50.00")));
        when(statusLogMapper.countByOrderId(14L)).thenReturn(1);

        OrderAnomalySummary summary = service.summarize(order);

        assertTrue(summary.isAbnormal());
        assertTrue(summary.getReasons().contains("退款处理中超时"));
    }

    private SOrders order(Long id,
                          Long status,
                          String paymentStatus,
                          String shippingStatus,
                          String recipientName,
                          String totalPrice,
                          LocalDateTime updateTime) {
        SOrders order = new SOrders();
        order.setId(id);
        order.setStatus(status);
        order.setPaymentStatus(paymentStatus);
        order.setShippingStatus(shippingStatus);
        order.setAddressId(1L);
        order.setRecipientName(recipientName);
        order.setTotalPrice(new BigDecimal(totalPrice));
        order.setUpdateTime(updateTime);
        return order;
    }

    private SOrderItem item(String totalPrice) {
        SOrderItem item = new SOrderItem();
        item.setItemTotalPrice(new BigDecimal(totalPrice));
        return item;
    }
}
