package com.suios.admin.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.analytics.dto.BehaviorFunnelResponse;
import com.suios.admin.analytics.dto.BehaviorFunnelStageCountDto;
import com.suios.admin.analytics.dto.DropOffDistributionResponse;
import com.suios.admin.analytics.dto.DropOffDistributionRowDto;
import com.suios.admin.analytics.dto.RecommendationAdoptionStatsDto;
import com.suios.admin.analytics.dto.RecommendationEntryComparisonResponse;
import com.suios.admin.analytics.dto.RecommendationEntryComparisonRowDto;
import com.suios.admin.analytics.dto.StalledCartRowDto;
import com.suios.admin.analytics.dto.StalledCartsResponse;
import com.suios.admin.analytics.mapper.BehaviorFunnelMapper;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.security.AuthContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BehaviorFunnelServiceTest {

    @Mock
    private BehaviorFunnelMapper behaviorFunnelMapper;

    @Mock
    private AuthContext authContext;

    private BehaviorFunnelService service;

    @BeforeEach
    void setUp() {
        service = new BehaviorFunnelService(behaviorFunnelMapper, authContext);
        ReflectionTestUtils.setField(service, "maxPageSize", 50);
        ReflectionTestUtils.setField(service, "maxFunnelRangeDays", 90);
        ReflectionTestUtils.setField(service, "sessionInactivityGapMinutes", 30);
        ReflectionTestUtils.setField(service, "stalledCartThresholdHours", 48);
        when(authContext.getCurrentUserId()).thenReturn(1L);
    }

    @Test
    void shouldBuildStableFunnelResponseWithZeroDivisionHandling() {
        when(behaviorFunnelMapper.selectFunnelStageCounts(
                eq(12L),
                eq(3L),
                eq(88L),
                eq("mall-home-dialog"),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(30)
        )).thenReturn(List.of(stageCount("browse", 8L), stageCount("detail", 4L), stageCount("cart", 2L)));

        BehaviorFunnelResponse response = service.getBehaviorFunnel(
                12L,
                3L,
                88L,
                "MALL_HOME_DIALOG",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 18)
        );

        assertEquals(8L, response.getTotalSessions());
        assertEquals(30, response.getSessionInactivityGapMinutes());
        assertEquals(5, response.getStages().size());
        assertEquals("browse", response.getStages().get(0).getStage());
        assertEquals("详情", response.getStages().get(1).getLabel());
        assertEquals(BigDecimal.valueOf(0.5000).setScale(4), response.getStages().get(0).getConversionRate());
        assertEquals(BigDecimal.ZERO.setScale(4), response.getStages().get(1).getConversionRate());
        assertEquals(BigDecimal.ZERO.setScale(4), response.getStages().get(2).getConversionRate());
        assertEquals(BigDecimal.ZERO.setScale(4), response.getStages().get(3).getConversionRate());
        assertEquals(BigDecimal.ZERO.setScale(4), response.getStages().get(4).getConversionRate());

        verify(behaviorFunnelMapper).selectFunnelStageCounts(
                eq(12L),
                eq(3L),
                eq(88L),
                eq("mall-home-dialog"),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(30)
        );
    }

    @Test
    void shouldNormalizeEntryComparisonAndInjectUnknownBucket() {
        when(behaviorFunnelMapper.selectEntryComparison(
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(row("WISHLIST", 4L, 2L, 1L, 1L)));

        RecommendationEntryComparisonResponse response = service.getEntryComparison(
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 18)
        );

        assertEquals(2, response.getRows().size());
        assertEquals("wishlist", response.getRows().get(0).getRequestSource());
        assertEquals(BigDecimal.valueOf(0.5000).setScale(4), response.getRows().get(0).getCartConversionRate());
        assertEquals(BigDecimal.valueOf(0.2500).setScale(4), response.getRows().get(0).getOrderConversionRate());
        assertEquals(BigDecimal.valueOf(1.0000).setScale(4), response.getRows().get(0).getAdoptionRate());
        assertEquals("unknown", response.getRows().get(1).getRequestSource());
        assertEquals(BigDecimal.ZERO.setScale(4), response.getRows().get(1).getOrderConversionRate());
    }

    @Test
    void shouldKeepProductDetailEntryComparisonSource() {
        when(behaviorFunnelMapper.selectEntryComparison(
                eq(null),
                eq(null),
                eq(null),
                eq("product-detail"),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(row("PRODUCT_DETAIL", 3L, 2L, 1L, 1L)));

        RecommendationEntryComparisonResponse response = service.getEntryComparison(
                null,
                null,
                null,
                "PRODUCT_DETAIL",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 18)
        );

        assertEquals(1, response.getRows().size());
        assertEquals("product-detail", response.getRows().get(0).getRequestSource());
        assertEquals(BigDecimal.valueOf(0.6667).setScale(4), response.getRows().get(0).getCartConversionRate());
        assertEquals(BigDecimal.valueOf(0.3333).setScale(4), response.getRows().get(0).getOrderConversionRate());
        assertEquals(BigDecimal.valueOf(1.0000).setScale(4), response.getRows().get(0).getAdoptionRate());
    }

    @Test
    void shouldNormalizeAdoptionStatsAndHandleEmptyPayloads() {
        RecommendationAdoptionStatsDto stats = new RecommendationAdoptionStatsDto();
        stats.setLinkedOrderCount(4L);
        stats.setAdoptedCount(3L);
        stats.setWithFeedbackCount(1L);
        when(behaviorFunnelMapper.selectRecommendationAdoption(
                eq(12L),
                eq(3L),
                eq(88L),
                eq("wishlist"),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(stats);

        RecommendationAdoptionStatsDto response = service.getRecommendationAdoption(
                12L,
                3L,
                88L,
                "wishlist",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 18)
        );

        assertEquals(4L, response.getLinkedOrderCount());
        assertEquals(3L, response.getAdoptedCount());
        assertEquals(1L, response.getSizeChangeCount());
        assertEquals(3L, response.getWithoutFeedbackCount());
        assertEquals(BigDecimal.valueOf(0.7500).setScale(4), response.getAdoptionRate());
        assertEquals(BigDecimal.valueOf(0.2500).setScale(4), response.getSizeChangeRate());
    }

    @Test
    void shouldMergeDropOffOtherBucketAndComputeShares() {
        when(behaviorFunnelMapper.selectDropOffDistribution(
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(30)
        )).thenReturn(List.of(
                dropOff("VIEW_ACTIVE_UNIFORMS", 7L),
                dropOff("VIEW_UNIFORM_DETAIL", 5L),
                dropOff("ADD_TO_CART_SUCCESS", 3L),
                dropOff(null, 2L)
        ));

        DropOffDistributionResponse response = service.getDropOffDistribution(
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 18),
                2
        );

        assertEquals(3, response.getRows().size());
        assertEquals("VIEW_ACTIVE_UNIFORMS", response.getRows().get(0).getLastActionType());
        assertEquals("VIEW_UNIFORM_DETAIL", response.getRows().get(1).getLastActionType());
        assertEquals("OTHER", response.getRows().get(2).getLastActionType());
        assertEquals(5L, response.getRows().get(2).getSessionCount());
        assertEquals(BigDecimal.valueOf(0.4118).setScale(4), response.getRows().get(0).getShare());
        assertEquals(BigDecimal.valueOf(0.2941).setScale(4), response.getRows().get(2).getShare());
        assertEquals(17L, response.getTotalSessions());
        assertEquals(2, response.getLimit());
    }

    @Test
    void shouldClampPaginationAndHandleNullStalledCartPage() {
        ArgumentCaptor<Page<StalledCartRowDto>> pageCaptor = ArgumentCaptor.forClass(pageClass());
        when(behaviorFunnelMapper.selectStalledCarts(
                pageCaptor.capture(),
                eq(12L),
                eq(3L),
                eq(88L),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(48)
        )).thenReturn(null);

        StalledCartsResponse response = service.getStalledCarts(
                12L,
                3L,
                88L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 18),
                999,
                -5
        );

        assertEquals(50, response.getLimit());
        assertEquals(0, response.getOffset());
        assertEquals(0L, response.getTotal());
        assertTrue(response.getRecords().isEmpty());
        assertEquals(1L, pageCaptor.getValue().getCurrent());
        assertEquals(50L, pageCaptor.getValue().getSize());
    }

    @Test
    void shouldRejectUnauthorizedAndOversizedDateRanges() {
        when(authContext.getCurrentUserId()).thenReturn(null);
        assertThrows(BizException.class, () -> service.getBehaviorFunnel(
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 18)
        ));
        verifyNoInteractions(behaviorFunnelMapper);

        when(authContext.getCurrentUserId()).thenReturn(1L);
        ReflectionTestUtils.setField(service, "maxFunnelRangeDays", 3);
        BizException exception = assertThrows(BizException.class, () -> service.getBehaviorFunnel(
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 7)
        ));
        assertTrue(exception.getMessage().contains("行为分析日期范围不能超过3天"));
    }

    private static BehaviorFunnelStageCountDto stageCount(String stage, Long sessionsReached) {
        BehaviorFunnelStageCountDto dto = new BehaviorFunnelStageCountDto();
        dto.setStage(stage);
        dto.setSessionsReached(sessionsReached);
        return dto;
    }

    private static RecommendationEntryComparisonRowDto row(String source,
                                                           Long exposureCount,
                                                           Long linkedCartCount,
                                                           Long linkedOrderCount,
                                                           Long adoptedCount) {
        RecommendationEntryComparisonRowDto dto = new RecommendationEntryComparisonRowDto();
        dto.setRequestSource(source);
        dto.setExposureCount(exposureCount);
        dto.setLinkedCartCount(linkedCartCount);
        dto.setLinkedOrderCount(linkedOrderCount);
        dto.setAdoptedCount(adoptedCount);
        return dto;
    }

    private static DropOffDistributionRowDto dropOff(String lastActionType, Long sessionCount) {
        DropOffDistributionRowDto dto = new DropOffDistributionRowDto();
        dto.setLastActionType(lastActionType);
        dto.setSessionCount(sessionCount);
        return dto;
    }

    @SuppressWarnings("unchecked")
    private static Class<Page<StalledCartRowDto>> pageClass() {
        return (Class<Page<StalledCartRowDto>>) (Class<?>) Page.class;
    }
}
