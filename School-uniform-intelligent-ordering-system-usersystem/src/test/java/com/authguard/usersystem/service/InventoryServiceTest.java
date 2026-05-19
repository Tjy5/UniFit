package com.authguard.usersystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.authguard.usersystem.entity.InventoryMovement;
import com.authguard.usersystem.entity.InventorySku;
import com.authguard.usersystem.exception.BizException;
import com.authguard.usersystem.mapper.InventoryMovementMapper;
import com.authguard.usersystem.mapper.InventorySkuMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventorySkuMapper skuMapper;

    @Mock
    private InventoryMovementMapper movementMapper;

    private InventoryService service;

    @BeforeEach
    void setUp() {
        service = new InventoryService(skuMapper, movementMapper);
    }

    @Test
    void shouldReserveAvailableStockAndWriteMovement() {
        when(skuMapper.selectActiveByUniformAndSizeForUpdate(1L, 2L)).thenReturn(sku(10L, 20L, 3L));
        when(skuMapper.updateQuantities(any(InventorySku.class))).thenReturn(1);
        when(movementMapper.insert(any(InventoryMovement.class))).thenReturn(1);

        service.reserve(1L, 2L, 5L, 700L, 900L, 42L, "订单创建预留库存");

        ArgumentCaptor<InventorySku> skuCaptor = ArgumentCaptor.forClass(InventorySku.class);
        verify(skuMapper).updateQuantities(skuCaptor.capture());
        assertEquals(20L, skuCaptor.getValue().getStockQuantity());
        assertEquals(8L, skuCaptor.getValue().getReservedQuantity());
        ArgumentCaptor<InventoryMovement> movementCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementMapper).insert(movementCaptor.capture());
        assertEquals("RESERVE", movementCaptor.getValue().getMovementType());
        assertEquals(700L, movementCaptor.getValue().getRelatedOrderId());
        assertEquals(900L, movementCaptor.getValue().getRelatedOrderItemId());
    }

    @Test
    void shouldRejectReserveWhenAvailableStockIsInsufficient() {
        when(skuMapper.selectActiveByUniformAndSizeForUpdate(1L, 2L)).thenReturn(sku(10L, 8L, 5L));

        assertThrows(BizException.class, () -> service.reserve(1L, 2L, 4L, 700L, 900L, 42L, "订单创建预留库存"));

        verify(skuMapper, never()).updateQuantities(any());
        verify(movementMapper, never()).insert(any());
    }

    @Test
    void shouldReleaseReservedInventory() {
        when(skuMapper.selectByIdForUpdate(10L)).thenReturn(sku(10L, 20L, 8L));
        when(skuMapper.updateQuantities(any(InventorySku.class))).thenReturn(1);
        when(movementMapper.insert(any(InventoryMovement.class))).thenReturn(1);

        service.release(10L, 3L, 700L, 900L, "USER", 42L, "未支付订单取消释放库存");

        ArgumentCaptor<InventorySku> skuCaptor = ArgumentCaptor.forClass(InventorySku.class);
        verify(skuMapper).updateQuantities(skuCaptor.capture());
        assertEquals(20L, skuCaptor.getValue().getStockQuantity());
        assertEquals(5L, skuCaptor.getValue().getReservedQuantity());
    }

    @Test
    void shouldOutboundReservedInventory() {
        when(skuMapper.selectByIdForUpdate(10L)).thenReturn(sku(10L, 20L, 8L));
        when(skuMapper.updateQuantities(any(InventorySku.class))).thenReturn(1);
        when(movementMapper.insert(any(InventoryMovement.class))).thenReturn(1);

        service.outbound(10L, 6L, 700L, 900L, "ADMIN", 1L, "订单发货出库");

        ArgumentCaptor<InventorySku> skuCaptor = ArgumentCaptor.forClass(InventorySku.class);
        verify(skuMapper).updateQuantities(skuCaptor.capture());
        assertEquals(14L, skuCaptor.getValue().getStockQuantity());
        assertEquals(2L, skuCaptor.getValue().getReservedQuantity());
    }

    @Test
    void shouldRejectOutboundWhenReservedQuantityIsInsufficient() {
        when(skuMapper.selectByIdForUpdate(10L)).thenReturn(sku(10L, 20L, 2L));

        assertThrows(BizException.class, () -> service.outbound(10L, 3L, 700L, 900L, "ADMIN", 1L, "订单发货出库"));

        verify(skuMapper, never()).updateQuantities(any());
        verify(movementMapper, never()).insert(any());
    }

    @Test
    void shouldReturnOrderMovementsAndIdempotencyFlag() {
        InventoryMovement reserve = new InventoryMovement();
        reserve.setMovementType("RESERVE");
        when(movementMapper.countByOrderAndType(700L, "OUTBOUND")).thenReturn(1L);
        when(movementMapper.selectByOrderAndType(700L, "RESERVE")).thenReturn(List.of(reserve));

        assertEquals(true, service.hasOrderMovement(700L, "OUTBOUND"));
        assertEquals(1, service.orderMovements(700L, "RESERVE").size());
    }

    private InventorySku sku(Long skuId, Long stockQuantity, Long reservedQuantity) {
        InventorySku sku = new InventorySku();
        sku.setSkuId(skuId);
        sku.setStockQuantity(stockQuantity);
        sku.setReservedQuantity(reservedQuantity);
        return sku;
    }
}
