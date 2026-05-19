package com.suios.admin.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.suios.admin.analytics.dto.ReplenishmentSuggestionDto;
import com.suios.admin.analytics.dto.SupplyForecastDto;
import com.suios.admin.analytics.dto.SupplySalesAnalyticsDto;
import com.suios.admin.analytics.dto.SupplyTrendPointDto;
import com.suios.admin.analytics.mapper.SupplyDemandAnalyticsMapper;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.security.AuthContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupplyDemandAnalyticsServiceTest {

    @Mock
    private SupplyDemandAnalyticsMapper mapper;

    @Mock
    private AuthContext authContext;

    private SupplyDemandAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new SupplyDemandAnalyticsService(mapper, authContext);
    }

    @Test
    void shouldFillMissingDailyTrendBucketsWithZeroValues() {
        authenticateAdmin();
        SupplyTrendPointDto dayOne = new SupplyTrendPointDto();
        dayOne.setBucket("2026-05-18");
        dayOne.setTotalQuantity(5L);
        dayOne.setTotalAmount(new BigDecimal("120.00"));
        dayOne.setOrderCount(2L);
        when(mapper.selectTrend(eq("day"), eq(1L), eq(2L), eq(3L), eq(4L), eq("summer"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(dayOne));

        List<SupplyTrendPointDto> rows = service.trend(
                "day",
                1L,
                2L,
                3L,
                4L,
                "summer",
                LocalDate.of(2026, 5, 18),
                LocalDate.of(2026, 5, 20)
        );

        assertEquals(3, rows.size());
        assertEquals("2026-05-18", rows.get(0).getBucket());
        assertEquals(5L, rows.get(0).getTotalQuantity());
        assertEquals("2026-05-19", rows.get(1).getBucket());
        assertEquals(0L, rows.get(1).getTotalQuantity());
        assertEquals(BigDecimal.ZERO, rows.get(1).getTotalAmount());
        assertEquals(0L, rows.get(1).getOrderCount());
    }

    @Test
    void shouldPassSalesAnalyticsFiltersToMapper() {
        authenticateAdmin();
        SupplySalesAnalyticsDto row = new SupplySalesAnalyticsDto();
        row.setGroupKey("summer");
        row.setGroupName("夏季");
        row.setTotalQuantity(12L);
        row.setTotalAmount(new BigDecimal("360.00"));
        row.setOrderCount(5L);
        when(mapper.selectSalesAnalytics(eq("category"), eq(1L), eq(2L), eq(3L), eq(4L), eq("summer"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(row));

        List<SupplySalesAnalyticsDto> rows = service.salesAnalytics(
                "category",
                1L,
                2L,
                3L,
                4L,
                "summer",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );

        assertEquals(1, rows.size());
        assertEquals("summer", rows.get(0).getGroupKey());
        assertEquals(12L, rows.get(0).getTotalQuantity());
    }

    @Test
    void shouldNormalizeForecastUsingHistoricalAverageAndReasonCodes() {
        authenticateAdmin();
        SupplyForecastDto base = forecastBase(10L, 15L, 3L, 4L, 5L, 8L, 2);
        when(mapper.selectForecastBase(eq(null), eq(null), eq(null), eq(null), eq(null), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(base));

        List<SupplyForecastDto> rows = service.forecasts(null, null, null, null, null, 10, 7);

        assertEquals(1, rows.size());
        assertEquals(10, rows.get(0).getLookbackDays());
        assertEquals(7, rows.get(0).getHorizonDays());
        assertEquals(new BigDecimal("1.5000"), rows.get(0).getDailyAverage());
        assertEquals(11L, rows.get(0).getForecastDemand());
        assertEquals(new BigDecimal("0.3000"), rows.get(0).getDataCompleteness());
        assertEquals(List.of("HISTORICAL_AVERAGE"), rows.get(0).getReasonCodes());
    }

    @Test
    void shouldCalculateReplenishmentSuggestionAndFilterCoveredRows() {
        authenticateAdmin();
        SupplyForecastDto shortage = forecastBase(10L, 15L, 3L, 4L, 5L, 8L, 2);
        SupplyForecastDto covered = forecastBase(11L, 0L, 0L, 100L, 5L, 8L, 2);
        when(mapper.selectForecastBase(eq(null), eq(null), eq(null), eq(null), eq(null), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(shortage, covered));

        List<ReplenishmentSuggestionDto> rows = service.replenishmentSuggestions(null, null, null, null, null, 10, 7, false);

        assertEquals(1, rows.size());
        assertEquals(10L, rows.get(0).getSkuId());
        assertEquals(11L, rows.get(0).getForecastDemand());
        assertEquals(15L, rows.get(0).getSuggestedQuantity());
    }

    @Test
    void shouldRejectInvalidDateRange() {
        authenticateAdmin();
        assertThrows(BizException.class, () -> service.salesAnalytics(
                "uniform",
                null,
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 5, 20),
                LocalDate.of(2026, 5, 18)
        ));
    }

    @Test
    void lowStockQueryShouldIncludeReorderPointBoundary() throws NoSuchMethodException {
        String sql = mapperSql(
                "selectLowStockAlerts",
                Long.class,
                Long.class,
                Long.class,
                Long.class,
                String.class
        );

        assertTrue(sql.contains("sku.reorder_point - (sku.stock_quantity - sku.reserved_quantity)"));
        assertTrue(sql.contains("(sku.stock_quantity - sku.reserved_quantity) &lt;= sku.reorder_point"));
    }

    @Test
    void sizeGroupingShouldUseStableUnknownBucketForMissingSizeId() throws NoSuchMethodException {
        String sql = mapperSql(
                "selectSalesAnalytics",
                String.class,
                Long.class,
                Long.class,
                Long.class,
                Long.class,
                String.class,
                LocalDateTime.class,
                LocalDateTime.class
        );

        assertTrue(sql.contains("CASE WHEN oi.size_id IS NULL THEN 'unknown' ELSE CAST(oi.size_id AS CHAR) END AS group_key"));
        assertTrue(sql.contains("CASE WHEN oi.size_id IS NULL THEN '未记录尺码' ELSE COALESCE(sz.size_name, CONCAT('尺码#', oi.size_id)) END AS group_name"));
    }

    private SupplyForecastDto forecastBase(Long skuId,
                                            Long historicalQuantity,
                                           Long salesDayCount,
                                           Long availableQuantity,
                                           Long safetyStock,
                                           Long reorderPoint,
                                           Integer leadTimeDays) {
        SupplyForecastDto row = new SupplyForecastDto();
        row.setSkuId(skuId);
        row.setUniformId(1L);
        row.setSizeId(2L);
        row.setUniformName("夏季短袖");
        row.setSizeName("165");
        row.setHistoricalQuantity(historicalQuantity);
        row.setSalesDayCount(salesDayCount);
        row.setAvailableQuantity(availableQuantity);
        row.setSafetyStock(safetyStock);
        row.setReorderPoint(reorderPoint);
        row.setLeadTimeDays(leadTimeDays);
        return row;
    }

    private void authenticateAdmin() {
        when(authContext.getCurrentUserId()).thenReturn(1L);
    }

    private String mapperSql(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Select select = SupplyDemandAnalyticsMapper.class
                .getMethod(methodName, parameterTypes)
                .getAnnotation(Select.class);
        return String.join("\n", select.value());
    }
}
