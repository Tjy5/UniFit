package com.suios.admin.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.common.enums.OrderStatus;
import com.suios.admin.common.enums.PaymentStatus;
import com.suios.admin.common.enums.ShippingStatus;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.order.FulfillmentActorType;
import com.suios.admin.common.order.FulfillmentCommand;
import com.suios.admin.common.order.FulfillmentState;
import com.suios.admin.common.order.OrderFulfillmentStateMachine;
import com.suios.admin.common.page.PageResult;
import com.suios.admin.common.security.AuthContext;
import com.suios.admin.inventory.entity.InventoryMovement;
import com.suios.admin.inventory.service.InventoryMovementService;
import com.suios.admin.order.dto.OrderAnomalySummary;
import com.suios.admin.order.dto.OrderFulfillmentCommandRequest;
import com.suios.admin.order.dto.OrderStatusLogDto;
import com.suios.admin.order.dto.OrderStatusUpdateRequest;
import com.suios.admin.order.entity.SOrderItem;
import com.suios.admin.order.entity.SOrderStatusLog;
import com.suios.admin.order.entity.SOrders;
import com.suios.admin.order.mapper.SOrderItemMapper;
import com.suios.admin.order.mapper.SOrderStatusLogMapper;
import com.suios.admin.order.mapper.SOrdersMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SOrdersService {

    private final SOrdersMapper ordersMapper;
    private final SOrderItemMapper orderItemMapper;
    private final SOrderStatusLogMapper statusLogMapper;
    private final AuthContext authContext;
    private final OrderAnomalyService anomalyService;
    private final InventoryMovementService inventoryMovementService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SOrdersService(SOrdersMapper ordersMapper,
                          SOrderItemMapper orderItemMapper,
                          SOrderStatusLogMapper statusLogMapper,
                          AuthContext authContext,
                          OrderAnomalyService anomalyService,
                          InventoryMovementService inventoryMovementService) {
        this.ordersMapper = ordersMapper;
        this.orderItemMapper = orderItemMapper;
        this.statusLogMapper = statusLogMapper;
        this.authContext = authContext;
        this.anomalyService = anomalyService;
        this.inventoryMovementService = inventoryMovementService;
    }

    public PageResult<SOrders> list(long pageNum,
                                    long pageSize,
                                    Long userId,
                                    Long status,
                                    String paymentStatus,
                                    String shippingStatus) {
        IPage<SOrders> page = ordersMapper.selectPageWithDetails(
                new Page<>(pageNum, pageSize),
                userId,
                status,
                paymentStatus,
                shippingStatus
        );
        page.getRecords().forEach(this::decorateAdminOrder);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<SOrders> listAll(Long userId, Long status, String paymentStatus, String shippingStatus) {
        List<SOrders> orders = ordersMapper.selectListWithDetails(userId, status, paymentStatus, shippingStatus);
        orders.forEach(this::decorateAdminOrder);
        return orders;
    }

    public SOrders getById(Long id) {
        SOrders order = ordersMapper.selectDetailById(id);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        List<SOrderItem> items = orderItemMapper.selectByOrderId(id);
        order.setOrderItems(items);
        decorateAdminOrder(order);
        return order;
    }

    @Transactional
    public void updateStatus(Long id, OrderStatusUpdateRequest request) {
        if (request.getStatus() == null
                && !StringUtils.hasText(request.getPaymentStatus())
                && !StringUtils.hasText(request.getShippingStatus())) {
            throw new BizException("至少需要更新一个状态字段");
        }

        if (request.getStatus() != null && !OrderStatus.supports(request.getStatus())) {
            throw new BizException("订单状态不合法");
        }
        if (StringUtils.hasText(request.getPaymentStatus()) && !PaymentStatus.supports(request.getPaymentStatus())) {
            throw new BizException("支付状态不合法");
        }
        if (StringUtils.hasText(request.getShippingStatus()) && !ShippingStatus.supports(request.getShippingStatus())) {
            throw new BizException("物流状态不合法");
        }

        SOrders order = getById(id);
        FulfillmentState current = stateFrom(order);
        FulfillmentState target = new FulfillmentState(
                request.getStatus() == null ? order.getStatus() : request.getStatus(),
                StringUtils.hasText(request.getPaymentStatus()) ? request.getPaymentStatus() : order.getPaymentStatus(),
                StringUtils.hasText(request.getShippingStatus()) ? request.getShippingStatus() : order.getShippingStatus()
        );
        FulfillmentState previousBeforeRefund = previousStateBeforeRefund(id);
        Optional<FulfillmentCommand> command = OrderFulfillmentStateMachine.findMatchingCommand(
                current,
                FulfillmentActorType.ADMIN,
                target,
                previousBeforeRefund
        );
        if (command.isEmpty()) {
            throw new BizException("历史状态接口只能执行可映射的管理端履约命令");
        }
        executeCommand(order, command.get(), "legacy status update");
    }

    @Transactional
    public SOrders executeCommand(Long id, OrderFulfillmentCommandRequest request) {
        SOrders order = getById(id);
        executeCommand(order, request.getCommand(), request.getReason());
        return getById(id);
    }

    public List<OrderStatusLogDto> statusLogs(Long id) {
        if (ordersMapper.selectById(id) == null) {
            throw new BizException("订单不存在");
        }
        return statusLogMapper.selectByOrderId(id).stream().map(this::toLogDto).toList();
    }

    public PageResult<SOrders> anomalies(long pageNum, long pageSize) {
        IPage<SOrders> page = ordersMapper.selectPageWithDetails(
                new Page<>(pageNum, pageSize),
                null,
                null,
                null,
                null
        );
        List<SOrders> abnormalOrders = page.getRecords()
                .stream()
                .peek(this::decorateAdminOrder)
                .filter(order -> order.getAnomalySummary() != null && order.getAnomalySummary().isAbnormal())
                .toList();
        return new PageResult<>(abnormalOrders, abnormalOrders.size(), pageNum, pageSize);
    }

    private void executeCommand(SOrders order, FulfillmentCommand command, String reason) {
        FulfillmentState current = stateFrom(order);
        FulfillmentState next;
        try {
            next = OrderFulfillmentStateMachine.transition(
                    current,
                    command,
                    FulfillmentActorType.ADMIN,
                    previousStateBeforeRefund(order.getId())
            );
        } catch (IllegalArgumentException ex) {
            throw new BizException(ex.getMessage());
        }
        boolean wasNotShipped = ShippingStatus.NOT_SHIPPED.getCode().equals(order.getShippingStatus());
        order.setStatus(next.orderStatus());
        order.setPaymentStatus(next.paymentStatus());
        order.setShippingStatus(next.shippingStatus());
        ordersMapper.updateById(order);
        applyInventorySideEffects(order, command, wasNotShipped);
        writeStatusLog(order.getId(), command, current, next, reason, contextFor(command, current));
    }

    private void applyInventorySideEffects(SOrders order, FulfillmentCommand command, boolean wasNotShipped) {
        if (command == FulfillmentCommand.SHIP_ORDER) {
            applyOutbound(order);
            return;
        }
        if (command == FulfillmentCommand.APPROVE_REFUND && wasNotShipped) {
            applyRelease(order);
        }
    }

    private void applyOutbound(SOrders order) {
        if (inventoryMovementService.hasOrderMovement(order.getId(), "OUTBOUND")) {
            return;
        }
        List<InventoryMovement> reserveMovements = inventoryMovementService.orderMovements(order.getId(), "RESERVE");
        if (reserveMovements == null) {
            return;
        }
        for (InventoryMovement reserveMovement : reserveMovements) {
            inventoryMovementService.apply(
                    reserveMovement.getSkuId(),
                    "OUTBOUND",
                    reserveMovement.getQuantity(),
                    order.getId(),
                    reserveMovement.getRelatedOrderItemId(),
                    "ADMIN",
                    authContext.getCurrentUserId(),
                    "订单发货出库"
            );
        }
    }

    private void applyRelease(SOrders order) {
        if (inventoryMovementService.hasOrderMovement(order.getId(), "RELEASE")) {
            return;
        }
        List<InventoryMovement> reserveMovements = inventoryMovementService.orderMovements(order.getId(), "RESERVE");
        if (reserveMovements == null) {
            return;
        }
        for (InventoryMovement reserveMovement : reserveMovements) {
            inventoryMovementService.apply(
                    reserveMovement.getSkuId(),
                    "RELEASE",
                    reserveMovement.getQuantity(),
                    order.getId(),
                    reserveMovement.getRelatedOrderItemId(),
                    "ADMIN",
                    authContext.getCurrentUserId(),
                    "未发货退款释放库存"
            );
        }
    }

    private void decorateAdminOrder(SOrders order) {
        try {
            order.setAllowedFulfillmentActions(
                    OrderFulfillmentStateMachine.allowedCommands(stateFrom(order), FulfillmentActorType.ADMIN)
                            .stream()
                            .map(Enum::name)
                            .toList()
            );
        } catch (RuntimeException ex) {
            order.setAllowedFulfillmentActions(List.of());
        }
        OrderAnomalySummary anomalySummary = anomalyService.summarize(order);
        order.setAnomalySummary(anomalySummary);
    }

    private FulfillmentState stateFrom(SOrders order) {
        return new FulfillmentState(order.getStatus(), order.getPaymentStatus(), order.getShippingStatus());
    }

    private FulfillmentState previousStateBeforeRefund(Long orderId) {
        SOrderStatusLog refundRequestLog = statusLogMapper.selectLatestByOrderIdAndEventType(
                orderId,
                FulfillmentCommand.REQUEST_REFUND.name()
        );
        if (refundRequestLog == null) {
            return null;
        }
        return new FulfillmentState(
                refundRequestLog.getFromOrderStatus(),
                refundRequestLog.getFromPaymentStatus(),
                refundRequestLog.getFromShippingStatus()
        );
    }

    private Map<String, Object> contextFor(FulfillmentCommand command, FulfillmentState current) {
        if (command != FulfillmentCommand.REQUEST_REFUND) {
            return null;
        }
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("previousOrderStatus", current.orderStatus());
        context.put("previousPaymentStatus", current.paymentStatus());
        context.put("previousShippingStatus", current.shippingStatus());
        return context;
    }

    private void writeStatusLog(Long orderId,
                                FulfillmentCommand command,
                                FulfillmentState from,
                                FulfillmentState to,
                                String reason,
                                Map<String, Object> context) {
        SOrderStatusLog log = new SOrderStatusLog();
        log.setOrderId(orderId);
        log.setEventType(command.name());
        log.setActorType(FulfillmentActorType.ADMIN.name());
        log.setActorId(authContext.getCurrentUserId());
        log.setFromOrderStatus(from.orderStatus());
        log.setToOrderStatus(to.orderStatus());
        log.setFromPaymentStatus(from.paymentStatus());
        log.setToPaymentStatus(to.paymentStatus());
        log.setFromShippingStatus(from.shippingStatus());
        log.setToShippingStatus(to.shippingStatus());
        log.setReason(reason);
        log.setContextJson(toJson(context));
        log.setCreateTime(LocalDateTime.now());
        int inserted = statusLogMapper.insert(log);
        if (inserted == 0) {
            throw new BizException("订单状态审计写入失败");
        }
    }

    private String toJson(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException ex) {
            throw new BizException("订单状态审计上下文序列化失败");
        }
    }

    private OrderStatusLogDto toLogDto(SOrderStatusLog log) {
        OrderStatusLogDto dto = new OrderStatusLogDto();
        dto.setId(log.getId());
        dto.setOrderId(log.getOrderId());
        dto.setEventType(log.getEventType());
        dto.setActorType(log.getActorType());
        dto.setActorId(log.getActorId());
        dto.setFromOrderStatus(log.getFromOrderStatus());
        dto.setToOrderStatus(log.getToOrderStatus());
        dto.setFromPaymentStatus(log.getFromPaymentStatus());
        dto.setToPaymentStatus(log.getToPaymentStatus());
        dto.setFromShippingStatus(log.getFromShippingStatus());
        dto.setToShippingStatus(log.getToShippingStatus());
        dto.setReason(log.getReason());
        dto.setContextJson(log.getContextJson());
        dto.setCreateTime(log.getCreateTime());
        return dto;
    }
}
