package com.suios.admin.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.security.AuthContext;
import com.suios.admin.inventory.entity.InventoryMovement;
import com.suios.admin.inventory.entity.InventorySku;
import com.suios.admin.inventory.mapper.InventoryMovementMapper;
import com.suios.admin.inventory.mapper.InventorySkuMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryMovementServiceTest {

    @Mock
    private InventorySkuMapper skuMapper;

    @Mock
    private InventoryMovementMapper movementMapper;

    @Mock
    private AuthContext authContext;

    private InventoryMovementService service;

    @BeforeEach
    void setUp() {
        service = new InventoryMovementService(skuMapper, movementMapper, authContext);
    }

    @Test
    void shouldReserveAvailableStockAndWriteMovement() {
        when(skuMapper.selectByIdForUpdate(10L)).thenReturn(sku(10L, 20L, 3L));
        when(skuMapper.updateById(org.mockito.ArgumentMatchers.<InventorySku>any())).thenReturn(1);
        when(movementMapper.insert(org.mockito.ArgumentMatchers.<InventoryMovement>any())).thenReturn(1);

        InventoryMovement movement = service.apply(10L, "reserve", 5L, 700L, 900L, "USER", 42L, "下单预留");

        assertEquals("RESERVE", movement.getMovementType());
        assertEquals(3L, movement.getBeforeReservedQuantity());
        assertEquals(8L, movement.getAfterReservedQuantity());
        ArgumentCaptor<InventorySku> skuCaptor = ArgumentCaptor.forClass(InventorySku.class);
        verify(skuMapper).updateById(skuCaptor.capture());
        assertEquals(20L, skuCaptor.getValue().getStockQuantity());
        assertEquals(8L, skuCaptor.getValue().getReservedQuantity());
    }

    @Test
    void shouldRejectReserveWhenAvailableStockIsInsufficient() {
        when(skuMapper.selectByIdForUpdate(10L)).thenReturn(sku(10L, 8L, 5L));

        assertThrows(BizException.class, () -> service.apply(10L, "RESERVE", 4L, 700L, 900L, "USER", 42L, "下单预留"));

        verify(skuMapper, never()).updateById(org.mockito.ArgumentMatchers.<InventorySku>any());
        verify(movementMapper, never()).insert(org.mockito.ArgumentMatchers.<InventoryMovement>any());
    }

    @Test
    void shouldReleaseReservedStock() {
        when(skuMapper.selectByIdForUpdate(10L)).thenReturn(sku(10L, 20L, 8L));
        when(skuMapper.updateById(org.mockito.ArgumentMatchers.<InventorySku>any())).thenReturn(1);
        when(movementMapper.insert(org.mockito.ArgumentMatchers.<InventoryMovement>any())).thenReturn(1);

        InventoryMovement movement = service.apply(10L, "RELEASE", 3L, 700L, 900L, "USER", 42L, "取消释放");

        assertEquals(8L, movement.getBeforeReservedQuantity());
        assertEquals(5L, movement.getAfterReservedQuantity());
    }

    @Test
    void shouldConsumeStockAndReservedQuantityOnOutbound() {
        when(skuMapper.selectByIdForUpdate(10L)).thenReturn(sku(10L, 20L, 8L));
        when(skuMapper.updateById(org.mockito.ArgumentMatchers.<InventorySku>any())).thenReturn(1);
        when(movementMapper.insert(org.mockito.ArgumentMatchers.<InventoryMovement>any())).thenReturn(1);

        InventoryMovement movement = service.apply(10L, "OUTBOUND", 6L, 700L, 900L, "ADMIN", 1L, "发货出库");

        assertEquals(20L, movement.getBeforeStockQuantity());
        assertEquals(14L, movement.getAfterStockQuantity());
        assertEquals(8L, movement.getBeforeReservedQuantity());
        assertEquals(2L, movement.getAfterReservedQuantity());
    }

    @Test
    void shouldRejectOutboundWhenReservedQuantityIsInsufficient() {
        when(skuMapper.selectByIdForUpdate(10L)).thenReturn(sku(10L, 20L, 2L));

        assertThrows(BizException.class, () -> service.apply(10L, "OUTBOUND", 3L, 700L, 900L, "ADMIN", 1L, "发货出库"));

        verify(skuMapper, never()).updateById(org.mockito.ArgumentMatchers.<InventorySku>any());
        verify(movementMapper, never()).insert(org.mockito.ArgumentMatchers.<InventoryMovement>any());
    }

    @Test
    void shouldAdjustStockWithoutChangingReservedQuantity() {
        when(skuMapper.selectByIdForUpdate(10L)).thenReturn(sku(10L, 20L, 8L));
        when(skuMapper.updateById(org.mockito.ArgumentMatchers.<InventorySku>any())).thenReturn(1);
        when(movementMapper.insert(org.mockito.ArgumentMatchers.<InventoryMovement>any())).thenReturn(1);

        InventoryMovement movement = service.apply(10L, "ADJUST", 12L, null, null, "ADMIN", 1L, "盘点调整");

        assertEquals(20L, movement.getBeforeStockQuantity());
        assertEquals(12L, movement.getAfterStockQuantity());
        assertEquals(8L, movement.getAfterReservedQuantity());
    }

    @Test
    void shouldRejectAdjustmentWithoutReason() {
        assertThrows(BizException.class, () -> service.apply(10L, "ADJUST", 12L, null, null, "ADMIN", 1L, " "));

        verify(skuMapper, never()).selectByIdForUpdate(10L);
        verify(skuMapper, never()).updateById(org.mockito.ArgumentMatchers.<InventorySku>any());
        verify(movementMapper, never()).insert(org.mockito.ArgumentMatchers.<InventoryMovement>any());
    }

    @Test
    void shouldReturnOrderMovementsForValidOrderAndType() {
        InventoryMovement reserve = new InventoryMovement();
        reserve.setMovementType("RESERVE");
        when(movementMapper.selectByOrderAndType(700L, "RESERVE")).thenReturn(List.of(reserve));

        List<InventoryMovement> rows = service.orderMovements(700L, "reserve");

        assertEquals(1, rows.size());
        verify(movementMapper).selectByOrderAndType(700L, "RESERVE");
    }

    private InventorySku sku(Long skuId, Long stockQuantity, Long reservedQuantity) {
        InventorySku sku = new InventorySku();
        sku.setSkuId(skuId);
        sku.setStockQuantity(stockQuantity);
        sku.setReservedQuantity(reservedQuantity);
        return sku;
    }
}
