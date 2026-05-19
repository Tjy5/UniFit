package com.authguard.usersystem.service.impl;

import com.authguard.usersystem.dto.CreateOrderRequestDto;
import com.authguard.usersystem.dto.OrderStatusLogDto;
import com.authguard.usersystem.entity.*; // 导入 SAddress
import com.authguard.usersystem.exception.BizException;
import com.authguard.usersystem.exception.ForbiddenException;
import com.authguard.usersystem.exception.NotFoundException;
import com.authguard.usersystem.mapper.*; // 导入 ISAddressMapper（占位符）
import com.authguard.usersystem.order.FulfillmentActorType;
import com.authguard.usersystem.order.FulfillmentCommand;
import com.authguard.usersystem.order.FulfillmentState;
import com.authguard.usersystem.order.OrderFulfillmentStateMachine;
import com.authguard.usersystem.order.OrderStatus;
import com.authguard.usersystem.order.PaymentStatus;
import com.authguard.usersystem.order.ShippingStatus;
import com.authguard.usersystem.service.InventoryService;
import com.authguard.usersystem.service.SOrdersService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
// 如果其他地方不需要，删除未使用的 Map 导入
// import java.util.Map;
// 如果其他地方不需要，删除未使用的 Collectors 导入
// import java.util.stream.Collectors;

@Service
public class SOrdersServiceImpl implements SOrdersService {

    private static final Logger log = LoggerFactory.getLogger(SOrdersServiceImpl.class);

    @Autowired
    private SOrdersMapper sOrdersMapper;
    @Autowired
    private SOrderItemMapper sOrderItemMapper;
    @Autowired
    private ShoppingCartItemMapper shoppingCartItemMapper;
    @Autowired
    private SUniformMapper sUniformMapper;
    @Autowired
    private SSizesMapper sSizesMapper;
    @Autowired
    private ISAddressMapper sAddressMapper;
    @Autowired
    private RecommendationLogMapper recommendationLogMapper;
    @Autowired
    private SOrderStatusLogMapper sOrderStatusLogMapper;
    @Autowired
    private InventoryService inventoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional // 确保原子性
    public SOrders createOrderFromCart(CreateOrderRequestDto requestDto, Long userId) {
        log.info("Creating order from cart for user {}", userId);

        // --- 1. 验证输入 DTO 和地址 ID ---
        if (requestDto == null) {
            log.warn("Create order request body is null for user {}", userId);
            throw new IllegalArgumentException("订单请求数据不能为空。");
        }
        if (requestDto.getAddressId() == null) {
            log.warn("Create order request missing addressId for user {}", userId);
            throw new IllegalArgumentException("需要配送地址 ID。");
        }
        log.debug("Create order request for user {} uses address {}", userId, requestDto.getAddressId());

        // --- 2. 使用 SAddressMapper 验证地址（关键步骤） ---
        // 使用占位符映射器获取地址详细信息
        SAddress selectedAddress = sAddressMapper.selectById(requestDto.getAddressId()); // 假设 selectById 存在于 ISAddressMapper 中

        if (selectedAddress == null) {
            log.warn("Address {} not found while creating order for user {}", requestDto.getAddressId(), userId);
            throw new IllegalArgumentException("选定的配送地址未找到。");
        }
        if (!selectedAddress.getUserId().equals(userId)) {
            log.warn("Address {} does not belong to user {}", requestDto.getAddressId(), userId);
            // 如果这被视为授权失败，SecurityException 可能更合适
            throw new IllegalArgumentException("选定的配送地址不属于当前用户。");
        }
        log.debug("Address {} verified for order creation by user {}", selectedAddress.getId(), userId);
        // 现在我们有了验证过的 'selectedAddress' 对象

        // --- 3. 获取购物车项目 ---
        List<ShoppingCartItem> cartItems = shoppingCartItemMapper.selectByUserId(userId);
        log.debug("Found {} cart items for user {}", cartItems == null ? 0 : cartItems.size(), userId);
        if (CollectionUtils.isEmpty(cartItems)) {
            throw new IllegalStateException("购物车为空。");
        }

        // --- 4. 创建订单头对象 ---
        SOrders newOrder = new SOrders();
        newOrder.setUserId(userId);
        // newOrder.setAddress(requestDto.getAddress()); // 已移除 - 现在使用 addressId
        newOrder.setAddressId(requestDto.getAddressId()); // 已添加 - 设置验证过的地址 ID
        newOrder.setRemark(requestDto.getRemark());
        newOrder.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        newOrder.setPaymentStatusCode(PaymentStatus.PENDING.getCode());
        newOrder.setShippingStatusCode(ShippingStatus.NOT_SHIPPED.getCode());
        newOrder.setOrderDate(new Date()); // 设置订单创建时间

        // 如果数据库或基础实体没有自动处理，设置创建者/时间戳
        // 假设您的数据库或基础实体没有自动处理这些：
        newOrder.setCreateBy(String.valueOf(userId)); // 设置创建者
        newOrder.setCreateTime(newOrder.getOrderDate()); // 通常与订单日期相同
        newOrder.setUpdateBy(String.valueOf(userId)); // 初始 updateBy
        newOrder.setUpdateTime(newOrder.getOrderDate()); // 初始 updateTime

        // --- 5. 处理购物车项目，计算总价，创建订单项目 ---
        List<SOrderItem> orderItemsForDb = new ArrayList<>();
        BigDecimal totalOrderPrice = BigDecimal.ZERO;

        for (ShoppingCartItem cartItem : cartItems) {
            SUniform uniform = sUniformMapper.selectSUniformById(cartItem.getUniformId());
            SSizes size = sSizesMapper.selectSSizesById(cartItem.getSizeId());

            // 对制服和尺寸进行健壮性验证
            if (uniform == null) {
                log.warn("Uniform {} not found while creating order for user {}", cartItem.getUniformId(), userId);
                throw new IllegalStateException("未找到 ID 为的制服：" + cartItem.getUniformId() + "。订单创建中止。");
            }
            if (size == null) {
                log.warn("Size {} not found while creating order for user {}", cartItem.getSizeId(), userId);
                throw new IllegalStateException("未找到 ID 为的尺寸：" + cartItem.getSizeId() + "。订单创建中止。");
            }
            if (uniform.getStatus() != 0) { // 假设状态 0 表示可用
                throw new IllegalStateException("制服 '" + uniform.getName() + "' 当前不可用。");
            }
            if (uniform.getPrice() == null) {
                log.warn("Uniform {} has no price while creating order for user {}", uniform.getId(), userId);
                throw new IllegalStateException("未找到制服 '" + uniform.getName() + "' 的价格。订单创建中止。");
            }

            SOrderItem orderItem = new SOrderItem();
            orderItem.setUniformId(uniform.getId());
            orderItem.setSizeId(size.getId());
            orderItem.setUniformNameSnapshot(uniform.getName());

            // 如果是逗号分隔的，取第一张图片，处理空值
            orderItem.setImageSnapshot(uniform.getImage() != null && !uniform.getImage().isEmpty() ? uniform.getImage().split(",")[0].trim() : null);
            orderItem.setSizeNameSnapshot(size.getSizeName());
            orderItem.setQuantity(Long.valueOf(cartItem.getQuantity()));
            orderItem.setUnitPriceSnapshot(uniform.getPrice()); // 下单时的价格
            orderItem.setItemTotalPrice(uniform.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));

            orderItemsForDb.add(orderItem);
            totalOrderPrice = totalOrderPrice.add(orderItem.getItemTotalPrice());
        }

        newOrder.setTotalPrice(totalOrderPrice);
        log.debug("Calculated order total {} for user {}", newOrder.getTotalPrice(), userId);

        // --- 6. 插入订单头（确保映射器返回生成的 ID） ---
        int orderInsertedCount = sOrdersMapper.insertOrder(newOrder);
        log.debug("Order header insert affected {} rows for user {}", orderInsertedCount, userId);

        if (orderInsertedCount == 0 || newOrder.getId() == null) {
            log.error("Failed to insert order header for user {}", userId);
            throw new IllegalStateException("创建订单失败。请重试。");
        }
        log.info("Order {} created for user {}", newOrder.getId(), userId);
        writeStatusLog(
                newOrder.getId(),
                FulfillmentCommand.ORDER_CREATED,
                FulfillmentActorType.SYSTEM,
                userId,
                new FulfillmentState(null, null, null),
                FulfillmentState.fromOrder(newOrder),
                "订单创建",
                null
        );

        // --- 7. 插入订单项目并回填推荐日志 ---
        log.debug("Inserting {} order items for order {}", orderItemsForDb.size(), newOrder.getId());
        for (int i = 0; i < orderItemsForDb.size(); i++) {
            SOrderItem dbItem = orderItemsForDb.get(i);
            ShoppingCartItem sourceCartItem = cartItems.get(i);
            dbItem.setOrderId(newOrder.getId());
            int itemInsertedCount = sOrderItemMapper.insert(dbItem);
            if (itemInsertedCount == 0 || dbItem.getOrderItemId() == null) {
                throw new IllegalStateException("创建订单项失败。请重试。");
            }
            inventoryService.reserve(
                    dbItem.getUniformId(),
                    dbItem.getSizeId(),
                    dbItem.getQuantity(),
                    newOrder.getId(),
                    dbItem.getOrderItemId(),
                    userId,
                    "订单创建预留库存"
            );
            backfillRecommendationOrderLink(userId, sourceCartItem.getRecommendationLogId(), dbItem.getOrderItemId());
        }
        log.debug("Order items inserted for order {}", newOrder.getId());

        // --- 8. 清空购物车 ---
        shoppingCartItemMapper.deleteByUserId(userId);
        log.debug("Cart cleared after order {} creation for user {}", newOrder.getId(), userId);

        // --- 9. 返回创建的订单（重新获取以确保包含完整详细信息） ---
        // 重新获取确保我们通过 MyBatis 的 resultMap 获取关联的地址
        SOrders createdOrder = sOrdersMapper.selectById(newOrder.getId());
        if (createdOrder == null) {
            // 如果插入成功，这不应该发生，但最好检查
            log.warn("Created order {} could not be reloaded; returning assembled order", newOrder.getId());
            // 如果重新获取失败，可以选择手动设置验证过的地址和项目
            newOrder.setShippingAddress(selectedAddress);
            newOrder.setOrderItems(orderItemsForDb);
            fillUserAllowedActions(newOrder);
            return newOrder; // 作为后备返回手动组装的对象
        }
        log.debug("Reloaded created order {}", createdOrder.getId());
        fillUserAllowedActions(createdOrder);
        return createdOrder; // 返回包含地址和项目的订单
    }

    @Override
    public List<SOrders> getOrdersByUserId(Long userId) {
        // 此方法现在受益于更新的 SOrdersMapper.xml resultMap，
        // 它自动关联地址。
        List<SOrders> orders = sOrdersMapper.selectByUserId(userId);
        if (orders != null) {
            orders.forEach(this::fillUserAllowedActions);
        }
        log.debug("Found {} orders for user {}", orders == null ? 0 : orders.size(), userId);
        return orders;
    }

    @Override
    public SOrders getOrderById(Long orderId) {
        // 此方法也受益于更新的 SOrdersMapper.xml resultMap。
        SOrders order = sOrdersMapper.selectById(orderId);
        if (order != null) {
            fillUserAllowedActions(order);
            log.debug("Order {} found with address {}", orderId, order.getAddressId());
            if (order.getShippingAddress() != null) {
                log.debug("Order {} has linked shipping address", orderId);
            } else {
                log.debug("Order {} has no linked shipping address", orderId);
            }
        } else {
            log.debug("Order {} not found", orderId);
        }
        return order;
    }

    @Override
    @Transactional // 使状态更新原子化
    public boolean updateOrderStatus(Long orderId, Long status, String paymentStatusCode, String shippingStatusCode, String updateBy) {
        Long userId = parseActorId(updateBy);
        log.debug("Updating order {} status fields via compatibility wrapper", orderId);
        SOrders existing = sOrdersMapper.selectById(orderId);
        if (existing == null) {
            log.warn("Order {} not found; status update skipped", orderId);
            return false;
        }
        ensureOrderOwner(existing, userId);
        FulfillmentState current = FulfillmentState.fromOrder(existing);
        FulfillmentState target = new FulfillmentState(
                status == null ? existing.getStatus() : status,
                paymentStatusCode == null || paymentStatusCode.isEmpty() ? existing.getPaymentStatusCode() : paymentStatusCode,
                shippingStatusCode == null || shippingStatusCode.isEmpty() ? existing.getShippingStatusCode() : shippingStatusCode
        );
        if (current.equals(target)) {
            return false;
        }
        Optional<FulfillmentCommand> command = OrderFulfillmentStateMachine.findMatchingCommand(
                current,
                FulfillmentActorType.USER,
                target,
                previousStateBeforeRefund(orderId)
        );
        if (command.isEmpty()) {
            throw new BizException("历史状态接口只能执行可映射的用户履约命令。");
        }
        executeFulfillmentCommand(existing, userId, command.get(), FulfillmentActorType.USER, "legacy status update");
        return true;
    }

    @Override
    @Transactional
    public SOrders executeUserFulfillmentCommand(Long orderId, Long userId, FulfillmentCommand command, String reason) {
        SOrders order = sOrdersMapper.selectById(orderId);
        if (order == null) {
            throw new NotFoundException("订单未找到。");
        }
        ensureOrderOwner(order, userId);
        executeFulfillmentCommand(order, userId, command, FulfillmentActorType.USER, reason);
        SOrders updated = sOrdersMapper.selectById(orderId);
        fillUserAllowedActions(updated);
        return updated;
    }

    @Override
    public List<OrderStatusLogDto> getOrderStatusLogs(Long orderId, Long userId) {
        SOrders order = sOrdersMapper.selectById(orderId);
        if (order == null) {
            throw new NotFoundException("订单未找到。");
        }
        ensureOrderOwner(order, userId);
        return sOrderStatusLogMapper.selectByOrderId(orderId)
                .stream()
                .map(this::toLogDto)
                .toList();
    }

    private void executeFulfillmentCommand(SOrders order,
                                           Long actorId,
                                           FulfillmentCommand command,
                                           FulfillmentActorType actorType,
                                           String reason) {
        FulfillmentState current = FulfillmentState.fromOrder(order);
        FulfillmentState previousBeforeRefund = previousStateBeforeRefund(order.getId());
        FulfillmentState next = OrderFulfillmentStateMachine.transition(current, command, actorType, previousBeforeRefund);
        SOrders updatePayload = next.toOrderUpdate(order.getId(), actorId == null ? "system" : String.valueOf(actorId));
        int rowsAffected = sOrdersMapper.updateOrder(updatePayload);
        if (rowsAffected == 0) {
            throw new BizException("订单状态更新失败");
        }
        applyInventorySideEffects(order, command, actorType, actorId);
        writeStatusLog(order.getId(), command, actorType, actorId, current, next, reason, contextFor(command, current));
    }

    private void applyInventorySideEffects(SOrders order,
                                           FulfillmentCommand command,
                                           FulfillmentActorType actorType,
                                           Long actorId) {
        if (command != FulfillmentCommand.CANCEL_UNPAID) {
            return;
        }
        if (inventoryService.hasOrderMovement(order.getId(), InventoryService.RELEASE)) {
            return;
        }
        List<InventoryMovement> reserveMovements = inventoryService.orderMovements(order.getId(), InventoryService.RESERVE);
        for (InventoryMovement movement : reserveMovements) {
            inventoryService.release(
                    movement.getSkuId(),
                    movement.getQuantity(),
                    order.getId(),
                    movement.getRelatedOrderItemId(),
                    actorType.name(),
                    actorId,
                    "未支付订单取消释放库存"
            );
        }
    }

    private void backfillRecommendationOrderLink(Long userId, Long recommendationLogId, Long orderItemId) {
        if (recommendationLogId == null || orderItemId == null) {
            return;
        }
        int updated = recommendationLogMapper.updateOrderItemLink(recommendationLogId, userId, orderItemId);
        if (updated == 0) {
            log.debug("No recommendation log link updated for log {} and orderItem {}", recommendationLogId, orderItemId);
        }
    }

    private void fillUserAllowedActions(SOrders order) {
        if (order == null) {
            return;
        }
        try {
            List<String> actions = OrderFulfillmentStateMachine.allowedCommands(
                            FulfillmentState.fromOrder(order),
                            FulfillmentActorType.USER
                    )
                    .stream()
                    .map(Enum::name)
                    .toList();
            order.setAllowedFulfillmentActions(actions);
        } catch (RuntimeException ex) {
            order.setAllowedFulfillmentActions(Collections.emptyList());
        }
    }

    private void ensureOrderOwner(SOrders order, Long userId) {
        if (userId == null || !userId.equals(order.getUserId())) {
            throw new ForbiddenException("您无权操作此订单。");
        }
    }

    private Long parseActorId(String updateBy) {
        if (updateBy == null || updateBy.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(updateBy);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private FulfillmentState previousStateBeforeRefund(Long orderId) {
        SOrderStatusLog refundRequestLog = sOrderStatusLogMapper.selectLatestByOrderIdAndEventType(
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
                                FulfillmentActorType actorType,
                                Long actorId,
                                FulfillmentState from,
                                FulfillmentState to,
                                String reason,
                                Map<String, Object> context) {
        SOrderStatusLog logRecord = new SOrderStatusLog();
        logRecord.setOrderId(orderId);
        logRecord.setEventType(command.name());
        logRecord.setActorType(actorType.name());
        logRecord.setActorId(actorId);
        logRecord.setFromOrderStatus(from == null ? null : from.orderStatus());
        logRecord.setToOrderStatus(to == null ? null : to.orderStatus());
        logRecord.setFromPaymentStatus(from == null ? null : from.paymentStatus());
        logRecord.setToPaymentStatus(to == null ? null : to.paymentStatus());
        logRecord.setFromShippingStatus(from == null ? null : from.shippingStatus());
        logRecord.setToShippingStatus(to == null ? null : to.shippingStatus());
        logRecord.setReason(reason);
        logRecord.setContextJson(toJson(context));
        logRecord.setCreateTime(new Date());
        int inserted = sOrderStatusLogMapper.insert(logRecord);
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

    private OrderStatusLogDto toLogDto(SOrderStatusLog logRecord) {
        OrderStatusLogDto dto = new OrderStatusLogDto();
        dto.setId(logRecord.getId());
        dto.setOrderId(logRecord.getOrderId());
        dto.setEventType(logRecord.getEventType());
        dto.setActorType(logRecord.getActorType());
        dto.setActorId(logRecord.getActorId());
        dto.setFromOrderStatus(logRecord.getFromOrderStatus());
        dto.setToOrderStatus(logRecord.getToOrderStatus());
        dto.setFromPaymentStatus(logRecord.getFromPaymentStatus());
        dto.setToPaymentStatus(logRecord.getToPaymentStatus());
        dto.setFromShippingStatus(logRecord.getFromShippingStatus());
        dto.setToShippingStatus(logRecord.getToShippingStatus());
        dto.setReason(logRecord.getReason());
        dto.setContextJson(logRecord.getContextJson());
        dto.setCreateTime(logRecord.getCreateTime());
        return dto;
    }
}
