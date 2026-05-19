package com.authguard.usersystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.authguard.usersystem.dto.CreateOrderRequestDto;
import com.authguard.usersystem.entity.RecommendationLog;
import com.authguard.usersystem.entity.SAddress;
import com.authguard.usersystem.entity.SOrderItem;
import com.authguard.usersystem.entity.SOrders;
import com.authguard.usersystem.entity.SSizes;
import com.authguard.usersystem.entity.SUniform;
import com.authguard.usersystem.entity.ShoppingCartItem;
import com.authguard.usersystem.mapper.ISAddressMapper;
import com.authguard.usersystem.mapper.RecommendationLogMapper;
import com.authguard.usersystem.mapper.SOrderItemMapper;
import com.authguard.usersystem.mapper.SOrderStatusLogMapper;
import com.authguard.usersystem.mapper.SOrdersMapper;
import com.authguard.usersystem.mapper.SSizesMapper;
import com.authguard.usersystem.mapper.SUniformMapper;
import com.authguard.usersystem.mapper.ShoppingCartItemMapper;
import com.authguard.usersystem.service.impl.SOrdersServiceImpl;
import com.authguard.usersystem.service.impl.ShoppingCartServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecommendationOrderLinkIntegrationTest {

    @Mock
    private ShoppingCartItemMapper shoppingCartItemMapper;

    @Mock
    private SUniformMapper uniformMapper;

    @Mock
    private RecommendationLogMapper recommendationLogMapper;

    @Mock
    private SOrdersMapper sOrdersMapper;

    @Mock
    private SOrderItemMapper sOrderItemMapper;

    @Mock
    private SOrderStatusLogMapper sOrderStatusLogMapper;

    @Mock
    private SSizesMapper sSizesMapper;

    @Mock
    private ISAddressMapper sAddressMapper;

    @Mock
    private InventoryService inventoryService;

    private ShoppingCartServiceImpl shoppingCartService;
    private SOrdersServiceImpl sOrdersService;

    @BeforeEach
    void setUp() {
        shoppingCartService = new ShoppingCartServiceImpl();
        ReflectionTestUtils.setField(shoppingCartService, "cartItemMapper", shoppingCartItemMapper);
        ReflectionTestUtils.setField(shoppingCartService, "uniformMapper", uniformMapper);
        ReflectionTestUtils.setField(shoppingCartService, "recommendationLogMapper", recommendationLogMapper);

        sOrdersService = new SOrdersServiceImpl();
        ReflectionTestUtils.setField(sOrdersService, "sOrdersMapper", sOrdersMapper);
        ReflectionTestUtils.setField(sOrdersService, "sOrderItemMapper", sOrderItemMapper);
        ReflectionTestUtils.setField(sOrdersService, "shoppingCartItemMapper", shoppingCartItemMapper);
        ReflectionTestUtils.setField(sOrdersService, "sUniformMapper", uniformMapper);
        ReflectionTestUtils.setField(sOrdersService, "sSizesMapper", sSizesMapper);
        ReflectionTestUtils.setField(sOrdersService, "sAddressMapper", sAddressMapper);
        ReflectionTestUtils.setField(sOrdersService, "recommendationLogMapper", recommendationLogMapper);
        ReflectionTestUtils.setField(sOrdersService, "sOrderStatusLogMapper", sOrderStatusLogMapper);
        ReflectionTestUtils.setField(sOrdersService, "inventoryService", inventoryService);
        when(sOrderStatusLogMapper.insert(any())).thenReturn(1);
    }

    @Test
    void shouldPropagateRecommendationLogIdThroughCartAndOrder() {
        Long userId = 42L;
        Long uniformId = 100L;
        Long sizeId = 5L;
        Long recommendationLogId = 99L;

        RecommendationLog recommendationLog = new RecommendationLog();
        recommendationLog.setLogId(recommendationLogId);
        recommendationLog.setExperimentKey("exp-size");
        recommendationLog.setExperimentVariant("B");
        recommendationLog.setStrategyVersion("v1");
        when(recommendationLogMapper.selectByLogIdAndUserId(recommendationLogId, userId)).thenReturn(recommendationLog);
        when(shoppingCartItemMapper.selectByUserIdAndUniformIdAndSizeId(userId, uniformId, sizeId)).thenReturn(null);
        when(uniformMapper.selectSUniformById(uniformId)).thenReturn(uniform(uniformId, "夏季短袖", "cover.png", 128, 0));

        ShoppingCartItem cartItem = shoppingCartService.addItemToCart(userId, uniformId, sizeId, 2, recommendationLogId);

        assertEquals(recommendationLogId, cartItem.getRecommendationLogId());
        verify(shoppingCartItemMapper).insert(any(ShoppingCartItem.class));

        CreateOrderRequestDto request = new CreateOrderRequestDto();
        request.setAddressId(7L);

        SAddress address = new SAddress();
        address.setId(7L);
        address.setUserId(userId);
        when(sAddressMapper.selectById(7L)).thenReturn(address);
        when(shoppingCartItemMapper.selectByUserId(userId)).thenReturn(List.of(cartItem));
        when(sSizesMapper.selectSSizesById(sizeId)).thenReturn(size(sizeId, "165"));
        when(recommendationLogMapper.updateOrderItemLink(recommendationLogId, userId, 700L)).thenReturn(1);
        doAnswer(invocation -> {
            SOrders order = invocation.getArgument(0);
            order.setId(500L);
            return 1;
        }).when(sOrdersMapper).insertOrder(any(SOrders.class));
        doAnswer(invocation -> {
            SOrderItem orderItem = invocation.getArgument(0);
            orderItem.setOrderItemId(700L);
            return 1;
        }).when(sOrderItemMapper).insert(any(SOrderItem.class));
        SOrders persistedOrder = new SOrders();
        persistedOrder.setId(500L);
        when(sOrdersMapper.selectById(500L)).thenReturn(persistedOrder);

        SOrders createdOrder = sOrdersService.createOrderFromCart(request, userId);

        ArgumentCaptor<ShoppingCartItem> cartCaptor = ArgumentCaptor.forClass(ShoppingCartItem.class);
        verify(shoppingCartItemMapper).insert(cartCaptor.capture());
        assertEquals(recommendationLogId, cartCaptor.getValue().getRecommendationLogId());
        verify(recommendationLogMapper).updateOrderItemLink(recommendationLogId, userId, 700L);
        verify(shoppingCartItemMapper).deleteByUserId(userId);
        assertNotNull(createdOrder);
        assertEquals(500L, createdOrder.getId());
    }

    @Test
    void shouldSkipOrderBackfillWhenRecommendationLogIdIsMissing() {
        Long userId = 42L;
        Long uniformId = 100L;
        Long sizeId = 5L;

        ShoppingCartItem cartItem = new ShoppingCartItem();
        cartItem.setUserId(userId);
        cartItem.setUniformId(uniformId);
        cartItem.setSizeId(sizeId);
        cartItem.setQuantity(1);

        SAddress address = new SAddress();
        address.setId(7L);
        address.setUserId(userId);
        when(sAddressMapper.selectById(7L)).thenReturn(address);
        when(shoppingCartItemMapper.selectByUserId(userId)).thenReturn(List.of(cartItem));
        when(uniformMapper.selectSUniformById(uniformId)).thenReturn(uniform(uniformId, "夏季短袖", "cover.png", 128, 0));
        when(sSizesMapper.selectSSizesById(sizeId)).thenReturn(size(sizeId, "165"));
        doAnswer(invocation -> {
            SOrders order = invocation.getArgument(0);
            order.setId(501L);
            return 1;
        }).when(sOrdersMapper).insertOrder(any(SOrders.class));
        doAnswer(invocation -> {
            SOrderItem orderItem = invocation.getArgument(0);
            orderItem.setOrderItemId(701L);
            return 1;
        }).when(sOrderItemMapper).insert(any(SOrderItem.class));
        SOrders persistedOrder = new SOrders();
        persistedOrder.setId(501L);
        when(sOrdersMapper.selectById(501L)).thenReturn(persistedOrder);

        CreateOrderRequestDto request = new CreateOrderRequestDto();
        request.setAddressId(7L);
        sOrdersService.createOrderFromCart(request, userId);

        verify(recommendationLogMapper, never()).updateOrderItemLink(any(), any(), any());
    }

    private SUniform uniform(Long id, String name, String image, long price, long status) {
        SUniform uniform = new SUniform();
        uniform.setId(id);
        uniform.setName(name);
        uniform.setImage(image);
        uniform.setPrice(BigDecimal.valueOf(price));
        uniform.setStatus(status);
        return uniform;
    }

    private SSizes size(Long id, String name) {
        SSizes size = new SSizes();
        size.setId(id);
        size.setSizeName(name);
        return size;
    }
}
