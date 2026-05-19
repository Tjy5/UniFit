package com.suios.admin.order.service;

import com.suios.admin.common.order.FulfillmentState;
import com.suios.admin.common.order.OrderFulfillmentStateMachine;
import com.suios.admin.order.dto.OrderAnomalySummary;
import com.suios.admin.order.entity.SOrderItem;
import com.suios.admin.order.entity.SOrders;
import com.suios.admin.order.mapper.SOrderItemMapper;
import com.suios.admin.order.mapper.SOrderStatusLogMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderAnomalyService {

    private static final Duration WAIT_SHIP_TIMEOUT = Duration.ofDays(7);
    private static final Duration SHIPPED_TIMEOUT = Duration.ofDays(14);
    private static final Duration REFUNDING_TIMEOUT = Duration.ofDays(7);

    private final SOrderItemMapper orderItemMapper;
    private final SOrderStatusLogMapper statusLogMapper;

    public OrderAnomalyService(SOrderItemMapper orderItemMapper, SOrderStatusLogMapper statusLogMapper) {
        this.orderItemMapper = orderItemMapper;
        this.statusLogMapper = statusLogMapper;
    }

    public OrderAnomalySummary summarize(SOrders order) {
        List<SOrderItem> items = order.getOrderItems();
        if (items == null) {
            items = orderItemMapper.selectByOrderId(order.getId());
            order.setOrderItems(items);
        }

        OrderAnomalySummary summary = new OrderAnomalySummary();
        FulfillmentState state = new FulfillmentState(order.getStatus(), order.getPaymentStatus(), order.getShippingStatus());
        if (!OrderFulfillmentStateMachine.isCanonicalState(state)) {
            summary.getReasons().add("状态组合不符合订单履约状态机");
        }
        if (isStale(order, 1L, WAIT_SHIP_TIMEOUT)) {
            summary.getReasons().add("待发货超时");
        }
        if (isStale(order, 2L, SHIPPED_TIMEOUT)) {
            summary.getReasons().add("已发货待签收超时");
        }
        if (isStale(order, 5L, REFUNDING_TIMEOUT)) {
            summary.getReasons().add("退款处理中超时");
        }
        if (items == null || items.isEmpty()) {
            summary.getReasons().add("订单缺少订单项");
        } else {
            BigDecimal itemTotal = items.stream()
                    .map(SOrderItem::getItemTotalPrice)
                    .filter(value -> value != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (order.getTotalPrice() != null && order.getTotalPrice().compareTo(itemTotal) != 0) {
                summary.getReasons().add("订单金额与订单项小计不一致");
            }
        }
        if (order.getAddressId() == null || order.getRecipientName() == null) {
            summary.getReasons().add("订单缺少有效配送地址");
        }
        if (order.getUpdateTime() != null && statusLogMapper.countByOrderId(order.getId()) == 0) {
            summary.getReasons().add("缺少订单状态审计基线");
        }
        summary.setAbnormal(!summary.getReasons().isEmpty());
        return summary;
    }

    private boolean isStale(SOrders order, Long status, Duration timeout) {
        if (!status.equals(order.getStatus()) || order.getUpdateTime() == null) {
            return false;
        }
        return order.getUpdateTime().isBefore(LocalDateTime.now().minus(timeout));
    }
}
