package com.authguard.usersystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.authguard.usersystem.dto.CreateOrderRequestDto;
import com.authguard.usersystem.entity.InventoryMovement;
import com.authguard.usersystem.dto.OrderStatusLogDto;
import com.authguard.usersystem.entity.SAddress;
import com.authguard.usersystem.entity.SOrderItem;
import com.authguard.usersystem.entity.SOrderStatusLog;
import com.authguard.usersystem.entity.SOrders;
import com.authguard.usersystem.entity.SSizes;
import com.authguard.usersystem.entity.SUniform;
import com.authguard.usersystem.entity.ShoppingCartItem;
import com.authguard.usersystem.exception.BizException;
import com.authguard.usersystem.exception.ForbiddenException;
import com.authguard.usersystem.order.FulfillmentCommand;
import com.authguard.usersystem.mapper.ISAddressMapper;
import com.authguard.usersystem.mapper.RecommendationLogMapper;
import com.authguard.usersystem.mapper.SOrderItemMapper;
import com.authguard.usersystem.mapper.SOrdersMapper;
import com.authguard.usersystem.mapper.SOrderStatusLogMapper;
import com.authguard.usersystem.mapper.SSizesMapper;
import com.authguard.usersystem.mapper.SUniformMapper;
import com.authguard.usersystem.mapper.ShoppingCartItemMapper;
import com.authguard.usersystem.service.impl.SOrdersServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SOrdersServiceImplTest {

    @Mock
    private SOrdersMapper sOrdersMapper;

    @Mock
    private SOrderItemMapper sOrderItemMapper;

    @Mock
    private ShoppingCartItemMapper shoppingCartItemMapper;

    @Mock
    private SUniformMapper sUniformMapper;

    @Mock
    private SSizesMapper sSizesMapper;

    @Mock
    private ISAddressMapper sAddressMapper;

    @Mock
    private RecommendationLogMapper recommendationLogMapper;

    @Mock
    private SOrderStatusLogMapper sOrderStatusLogMapper;

    @Mock
    private InventoryService inventoryService;

    private SOrdersServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SOrdersServiceImpl();
        ReflectionTestUtils.setField(service, "sOrdersMapper", sOrdersMapper);
        ReflectionTestUtils.setField(service, "sOrderItemMapper", sOrderItemMapper);
        ReflectionTestUtils.setField(service, "shoppingCartItemMapper", shoppingCartItemMapper);
        ReflectionTestUtils.setField(service, "sUniformMapper", sUniformMapper);
        ReflectionTestUtils.setField(service, "sSizesMapper", sSizesMapper);
        ReflectionTestUtils.setField(service, "sAddressMapper", sAddressMapper);
        ReflectionTestUtils.setField(service, "recommendationLogMapper", recommendationLogMapper);
        ReflectionTestUtils.setField(service, "sOrderStatusLogMapper", sOrderStatusLogMapper);
        ReflectionTestUtils.setField(service, "inventoryService", inventoryService);
    }

    @Test
    void shouldBackfillRecommendationLogWhenCreatingOrderFromCart() {
        when(sAddressMapper.selectById(7L)).thenReturn(address(7L, 42L));
        when(shoppingCartItemMapper.selectByUserId(42L)).thenReturn(List.of(cartItem(100L, 5L, 2, 88L)));
        when(sUniformMapper.selectSUniformById(100L)).thenReturn(uniform(100L, "夏季短袖", "79.00"));
        when(sSizesMapper.selectSSizesById(5L)).thenReturn(size(5L, "165"));
        when(sOrdersMapper.insertOrder(any(SOrders.class))).thenAnswer(invocation -> {
            SOrders order = invocation.getArgument(0);
            order.setId(500L);
            return 1;
        });
        when(sOrderStatusLogMapper.insert(any())).thenReturn(1);
        when(sOrderItemMapper.insert(any(SOrderItem.class))).thenAnswer(invocation -> {
            SOrderItem item = invocation.getArgument(0);
            item.setOrderItemId(900L);
            return 1;
        });
        SOrders created = new SOrders();
        created.setId(500L);
        when(sOrdersMapper.selectById(500L)).thenReturn(created);

        CreateOrderRequestDto request = new CreateOrderRequestDto();
        request.setAddressId(7L);
        request.setRemark("请尽快发货");

        SOrders response = service.createOrderFromCart(request, 42L);

        assertEquals(500L, response.getId());
        verify(inventoryService).reserve(100L, 5L, 2L, 500L, 900L, 42L, "订单创建预留库存");
        verify(recommendationLogMapper).updateOrderItemLink(88L, 42L, 900L);
        verify(shoppingCartItemMapper).deleteByUserId(42L);
    }

    @Test
    void shouldRejectOrderCreationWhenInventoryReserveFails() {
        when(sAddressMapper.selectById(7L)).thenReturn(address(7L, 42L));
        when(shoppingCartItemMapper.selectByUserId(42L)).thenReturn(List.of(cartItem(100L, 5L, 2, null)));
        when(sUniformMapper.selectSUniformById(100L)).thenReturn(uniform(100L, "夏季短袖", "79.00"));
        when(sSizesMapper.selectSSizesById(5L)).thenReturn(size(5L, "165"));
        when(sOrdersMapper.insertOrder(any(SOrders.class))).thenAnswer(invocation -> {
            SOrders order = invocation.getArgument(0);
            order.setId(502L);
            return 1;
        });
        when(sOrderStatusLogMapper.insert(any())).thenReturn(1);
        when(sOrderItemMapper.insert(any(SOrderItem.class))).thenAnswer(invocation -> {
            SOrderItem item = invocation.getArgument(0);
            item.setOrderItemId(902L);
            return 1;
        });
        org.mockito.Mockito.doThrow(new BizException("商品库存不足，无法创建订单"))
                .when(inventoryService)
                .reserve(100L, 5L, 2L, 502L, 902L, 42L, "订单创建预留库存");

        CreateOrderRequestDto request = new CreateOrderRequestDto();
        request.setAddressId(7L);

        assertThrows(BizException.class, () -> service.createOrderFromCart(request, 42L));

        verify(shoppingCartItemMapper, never()).deleteByUserId(42L);
        verify(recommendationLogMapper, never()).updateOrderItemLink(any(), any(), any());
    }

    @Test
    void shouldSkipRecommendationBackfillWhenCartItemHasNoRecommendationLog() {
        when(sAddressMapper.selectById(7L)).thenReturn(address(7L, 42L));
        when(shoppingCartItemMapper.selectByUserId(42L)).thenReturn(List.of(cartItem(100L, 5L, 1, null)));
        when(sUniformMapper.selectSUniformById(100L)).thenReturn(uniform(100L, "夏季短袖", "79.00"));
        when(sSizesMapper.selectSSizesById(5L)).thenReturn(size(5L, "165"));
        when(sOrdersMapper.insertOrder(any(SOrders.class))).thenAnswer(invocation -> {
            SOrders order = invocation.getArgument(0);
            order.setId(501L);
            return 1;
        });
        when(sOrderStatusLogMapper.insert(any())).thenReturn(1);
        when(sOrderItemMapper.insert(any(SOrderItem.class))).thenAnswer(invocation -> {
            SOrderItem item = invocation.getArgument(0);
            item.setOrderItemId(901L);
            return 1;
        });
        SOrders created = new SOrders();
        created.setId(501L);
        when(sOrdersMapper.selectById(501L)).thenReturn(created);

        CreateOrderRequestDto request = new CreateOrderRequestDto();
        request.setAddressId(7L);

        service.createOrderFromCart(request, 42L);

        verify(recommendationLogMapper, never()).updateOrderItemLink(any(), any(), any());
    }

    @Test
    void shouldExecuteUserPaymentCommandAndWriteAudit() {
        SOrders current = order(600L, 42L, 0L, "PENDING", "NOT_SHIPPED");
        SOrders updated = order(600L, 42L, 1L, "PAID", "NOT_SHIPPED");
        when(sOrdersMapper.selectById(600L)).thenReturn(current, updated);
        when(sOrdersMapper.updateOrder(any(SOrders.class))).thenReturn(1);
        when(sOrderStatusLogMapper.insert(any(SOrderStatusLog.class))).thenReturn(1);

        SOrders response = service.executeUserFulfillmentCommand(
                600L,
                42L,
                FulfillmentCommand.SIMULATE_PAYMENT_SUCCESS,
                "支付成功"
        );

        assertEquals(1L, response.getStatus());
        assertEquals(List.of("REQUEST_REFUND"), response.getAllowedFulfillmentActions());
        verify(sOrdersMapper).updateOrder(any(SOrders.class));
        verify(sOrderStatusLogMapper).insert(any(SOrderStatusLog.class));
    }

    @Test
    void shouldReleaseReservedInventoryWhenCancellingUnpaidOrder() {
        SOrders current = order(605L, 42L, 0L, "PENDING", "NOT_SHIPPED");
        SOrders updated = order(605L, 42L, 4L, "CLOSED", "NOT_SHIPPED");
        InventoryMovement reserve = new InventoryMovement();
        reserve.setSkuId(10L);
        reserve.setQuantity(2L);
        reserve.setRelatedOrderItemId(900L);
        when(sOrdersMapper.selectById(605L)).thenReturn(current, updated);
        when(sOrdersMapper.updateOrder(any(SOrders.class))).thenReturn(1);
        when(inventoryService.hasOrderMovement(605L, InventoryService.RELEASE)).thenReturn(false);
        when(inventoryService.orderMovements(605L, InventoryService.RESERVE)).thenReturn(List.of(reserve));
        when(sOrderStatusLogMapper.insert(any(SOrderStatusLog.class))).thenReturn(1);

        SOrders response = service.executeUserFulfillmentCommand(605L, 42L, FulfillmentCommand.CANCEL_UNPAID, "取消订单");

        assertEquals(4L, response.getStatus());
        verify(inventoryService).release(10L, 2L, 605L, 900L, "USER", 42L, "未支付订单取消释放库存");
    }

    @Test
    void shouldSkipInventoryReleaseWhenCancellationAlreadyReleased() {
        SOrders current = order(606L, 42L, 0L, "PENDING", "NOT_SHIPPED");
        SOrders updated = order(606L, 42L, 4L, "CLOSED", "NOT_SHIPPED");
        when(sOrdersMapper.selectById(606L)).thenReturn(current, updated);
        when(sOrdersMapper.updateOrder(any(SOrders.class))).thenReturn(1);
        when(inventoryService.hasOrderMovement(606L, InventoryService.RELEASE)).thenReturn(true);
        when(sOrderStatusLogMapper.insert(any(SOrderStatusLog.class))).thenReturn(1);

        SOrders response = service.executeUserFulfillmentCommand(606L, 42L, FulfillmentCommand.CANCEL_UNPAID, "取消订单");

        assertEquals(4L, response.getStatus());
        verify(inventoryService, never()).orderMovements(606L, InventoryService.RESERVE);
        verify(inventoryService, never()).release(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void shouldRejectFulfillmentCommandForAnotherUser() {
        when(sOrdersMapper.selectById(601L)).thenReturn(order(601L, 99L, 0L, "PENDING", "NOT_SHIPPED"));

        assertThrows(ForbiddenException.class, () ->
                service.executeUserFulfillmentCommand(601L, 42L, FulfillmentCommand.CANCEL_UNPAID, null));

        verify(sOrdersMapper, never()).updateOrder(any());
        verify(sOrderStatusLogMapper, never()).insert(any());
    }

    @Test
    void shouldRejectIllegalFulfillmentCommandWithoutChangingOrder() {
        when(sOrdersMapper.selectById(602L)).thenReturn(order(602L, 42L, 0L, "PENDING", "NOT_SHIPPED"));

        assertThrows(IllegalArgumentException.class, () ->
                service.executeUserFulfillmentCommand(602L, 42L, FulfillmentCommand.CONFIRM_RECEIPT, null));

        verify(sOrdersMapper, never()).updateOrder(any());
        verify(sOrderStatusLogMapper, never()).insert(any());
    }

    @Test
    void shouldExposeOwnedStatusLogs() {
        when(sOrdersMapper.selectById(603L)).thenReturn(order(603L, 42L, 1L, "PAID", "NOT_SHIPPED"));
        SOrderStatusLog log = new SOrderStatusLog();
        log.setId(1L);
        log.setOrderId(603L);
        log.setEventType("SIMULATE_PAYMENT_SUCCESS");
        when(sOrderStatusLogMapper.selectByOrderId(603L)).thenReturn(List.of(log));

        List<OrderStatusLogDto> logs = service.getOrderStatusLogs(603L, 42L);

        assertEquals(1, logs.size());
        assertEquals("SIMULATE_PAYMENT_SUCCESS", logs.getFirst().getEventType());
    }

    @Test
    void shouldFailWhenAuditWriteFailsAfterOrderUpdate() {
        when(sOrdersMapper.selectById(604L)).thenReturn(order(604L, 42L, 0L, "PENDING", "NOT_SHIPPED"));
        when(sOrdersMapper.updateOrder(any(SOrders.class))).thenReturn(1);
        when(sOrderStatusLogMapper.insert(any(SOrderStatusLog.class))).thenReturn(0);

        assertThrows(BizException.class, () ->
                service.executeUserFulfillmentCommand(604L, 42L, FulfillmentCommand.SIMULATE_PAYMENT_SUCCESS, null));
    }

    private SAddress address(Long id, Long userId) {
        SAddress address = new SAddress();
        address.setId(id);
        address.setUserId(userId);
        return address;
    }

    private SOrders order(Long id, Long userId, Long status, String paymentStatus, String shippingStatus) {
        SOrders order = new SOrders();
        order.setId(id);
        order.setUserId(userId);
        order.setStatus(status);
        order.setPaymentStatusCode(paymentStatus);
        order.setShippingStatusCode(shippingStatus);
        return order;
    }

    private ShoppingCartItem cartItem(Long uniformId, Long sizeId, int quantity, Long recommendationLogId) {
        ShoppingCartItem item = new ShoppingCartItem();
        item.setUniformId(uniformId);
        item.setSizeId(sizeId);
        item.setQuantity(quantity);
        item.setRecommendationLogId(recommendationLogId);
        return item;
    }

    private SUniform uniform(Long id, String name, String price) {
        SUniform uniform = new SUniform();
        uniform.setId(id);
        uniform.setName(name);
        uniform.setPrice(new BigDecimal(price));
        uniform.setStatus(0L);
        uniform.setImage("uniform.jpg");
        return uniform;
    }

    private SSizes size(Long id, String name) {
        SSizes size = new SSizes();
        size.setId(id);
        size.setSizeName(name);
        return size;
    }
}
