package com.suios.admin.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suios.admin.common.exception.BizException;
import com.suios.admin.inventory.dto.LowStockAlertDto;
import com.suios.admin.inventory.entity.InventorySku;
import com.suios.admin.inventory.mapper.InventorySkuMapper;
import com.suios.admin.size.entity.SSizes;
import com.suios.admin.size.mapper.SSizesMapper;
import com.suios.admin.uniform.entity.SUniform;
import com.suios.admin.uniform.mapper.SUniformMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventorySkuServiceTest {

    @Mock
    private InventorySkuMapper skuMapper;

    @Mock
    private SUniformMapper uniformMapper;

    @Mock
    private SSizesMapper sizesMapper;

    private InventorySkuService service;

    @BeforeEach
    void setUp() {
        service = new InventorySkuService(skuMapper, uniformMapper, sizesMapper);
    }

    @Test
    void shouldCalculateAvailableQuantity() {
        InventorySku sku = sku(10L, 1L, 2L, 20L, 6L, 5L, 10L);

        long available = service.availableQuantity(sku);

        assertEquals(14L, available);
    }

    @Test
    void shouldSortLowStockAlertsBySeverityAndShortage() {
        InventorySku critical = sku(10L, 1L, 2L, 6L, 4L, 5L, 8L);
        critical.setUniformName("夏季短袖");
        critical.setSizeName("165");
        InventorySku warning = sku(11L, 1L, 3L, 12L, 5L, 3L, 10L);
        warning.setUniformName("夏季短袖");
        warning.setSizeName("170");
        when(skuMapper.selectListWithContext(null, null, null, null, null, "ACTIVE")).thenReturn(List.of(warning, critical));

        List<LowStockAlertDto> alerts = service.lowStockAlerts(null, null, null, null, null, null);

        assertEquals(2, alerts.size());
        assertEquals(10L, alerts.get(0).getSkuId());
        assertEquals("CRITICAL", alerts.get(0).getSeverity());
        assertEquals(6L, alerts.get(0).getShortageQuantity());
        assertEquals(11L, alerts.get(1).getSkuId());
        assertEquals("WARNING", alerts.get(1).getSeverity());
        assertEquals(3L, alerts.get(1).getShortageQuantity());
    }

    @Test
    void shouldFilterLowStockAlertsBySeverity() {
        InventorySku critical = sku(10L, 1L, 2L, 6L, 4L, 5L, 8L);
        InventorySku warning = sku(11L, 1L, 3L, 12L, 5L, 3L, 10L);
        when(skuMapper.selectListWithContext(null, null, null, null, null, "ACTIVE")).thenReturn(List.of(critical, warning));

        List<LowStockAlertDto> alerts = service.lowStockAlerts(null, null, null, null, null, "warning");

        assertEquals(1, alerts.size());
        assertEquals(11L, alerts.get(0).getSkuId());
    }

    @Test
    void shouldIncludeLowStockAlertAtReorderPointBoundary() {
        InventorySku boundary = sku(12L, 1L, 4L, 10L, 2L, 3L, 8L);
        when(skuMapper.selectListWithContext(null, null, null, null, null, "ACTIVE")).thenReturn(List.of(boundary));

        List<LowStockAlertDto> alerts = service.lowStockAlerts(null, null, null, null, null, null);

        assertEquals(1, alerts.size());
        assertEquals(12L, alerts.get(0).getSkuId());
        assertEquals(8L, alerts.get(0).getAvailableQuantity());
        assertEquals(0L, alerts.get(0).getShortageQuantity());
        assertEquals("WARNING", alerts.get(0).getSeverity());
    }

    @Test
    void shouldCreateActiveSkuWhenMasterDataAndQuantitiesAreValid() {
        InventorySku sku = sku(null, 1L, 2L, 20L, 0L, 5L, 10L);
        InventorySku saved = sku(20L, 1L, 2L, 20L, 0L, 5L, 10L);
        when(uniformMapper.selectById(1L)).thenReturn(new SUniform());
        when(sizesMapper.selectById(2L)).thenReturn(new SSizes());
        when(skuMapper.selectActiveByUniformAndSize(1L, 2L)).thenReturn(null);
        when(skuMapper.insert(org.mockito.ArgumentMatchers.<InventorySku>any())).thenAnswer(invocation -> {
            InventorySku value = invocation.getArgument(0);
            value.setSkuId(20L);
            return 1;
        });
        when(skuMapper.selectByIdWithContext(20L)).thenReturn(saved);

        InventorySku created = service.create(sku);

        assertEquals(20L, created.getSkuId());
        assertEquals(20L, created.getStockQuantity());
        assertEquals(20L, created.getAvailableQuantity());
    }

    @Test
    void shouldRejectNegativeQuantitiesWhenCreatingSku() {
        InventorySku sku = sku(null, 1L, 2L, -1L, 0L, 0L, 0L);
        when(uniformMapper.selectById(1L)).thenReturn(new SUniform());
        when(sizesMapper.selectById(2L)).thenReturn(new SSizes());

        assertThrows(BizException.class, () -> service.create(sku));

        verify(skuMapper, never()).insert(org.mockito.ArgumentMatchers.<InventorySku>any());
        verify(skuMapper, never()).selectActiveByUniformAndSize(eq(1L), eq(2L));
    }

    private InventorySku sku(Long skuId, Long uniformId, Long sizeId, Long stockQuantity, Long reservedQuantity, Long safetyStock, Long reorderPoint) {
        InventorySku sku = new InventorySku();
        sku.setSkuId(skuId);
        sku.setUniformId(uniformId);
        sku.setSizeId(sizeId);
        sku.setStockQuantity(stockQuantity);
        sku.setReservedQuantity(reservedQuantity);
        sku.setSafetyStock(safetyStock);
        sku.setReorderPoint(reorderPoint);
        sku.setLeadTimeDays(0);
        sku.setStatus("ACTIVE");
        return sku;
    }
}
