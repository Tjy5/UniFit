package com.suios.admin.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.analytics.dto.FeedbackDistributionDto;
import com.suios.admin.analytics.dto.FeedbackDistributionResponse;
import com.suios.admin.analytics.dto.LowConfidenceHotspotDto;
import com.suios.admin.analytics.dto.LowConfidenceHotspotsResponse;
import com.suios.admin.analytics.dto.RecommendationExperimentVariantMetricsDto;
import com.suios.admin.analytics.dto.RecommendationExperimentMetricsResponse;
import com.suios.admin.analytics.dto.RecommendationStatsDto;
import com.suios.admin.analytics.dto.RecommendationTrendPointDto;
import com.suios.admin.analytics.mapper.RecommendationAnalyticsMapper;
import com.suios.admin.common.security.AuthContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecommendationAnalyticsServiceTest {

    @Mock
    private RecommendationAnalyticsMapper recommendationAnalyticsMapper;

    @Mock
    private AuthContext authContext;

    private RecommendationAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationAnalyticsService(recommendationAnalyticsMapper, authContext);
        ReflectionTestUtils.setField(service, "maxPageSize", 200);
        when(authContext.getCurrentUserId()).thenReturn(1L);
    }

    @Test
    void shouldAggregateRecommendationStatsAndFillTrendGaps() {
        RecommendationStatsDto stats = new RecommendationStatsDto();
        stats.setTotalRecommendations(10L);
        stats.setLinkedOrderCount(4L);
        stats.setTotalFeedbacks(3L);
        stats.setFitCount(2L);
        stats.setTooLargeCount(1L);
        stats.setTooSmallCount(0L);
        stats.setLowConfidenceCount(2L);
        stats.setAvgConfidenceScore(78);

        RecommendationTrendPointDto dayOne = new RecommendationTrendPointDto();
        dayOne.setDay("2026-03-01");
        dayOne.setTotalRecommendations(10L);
        dayOne.setLinkedOrderCount(4L);
        dayOne.setTotalFeedbacks(3L);
        dayOne.setFitCount(2L);
        dayOne.setTooLargeCount(1L);
        dayOne.setTooSmallCount(0L);
        dayOne.setFitRate(BigDecimal.valueOf(0.6667));
        dayOne.setAvgConfidenceScore(78);

        when(recommendationAnalyticsMapper.selectRecommendationStats(
                eq(12L),
                eq(88L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(stats);
        when(recommendationAnalyticsMapper.selectRecommendationTrend(
                eq(12L),
                eq(88L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(dayOne));

        RecommendationStatsDto response = service.getRecommendationStats(
                12L,
                88L,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 2)
        );

        assertEquals(BigDecimal.valueOf(0.4000).setScale(4), response.getLinkedOrderCoverage());
        assertEquals(BigDecimal.valueOf(0.7500).setScale(4), response.getFeedbackCoverage());
        assertEquals(BigDecimal.valueOf(0.6667).setScale(4), response.getFitRate());
        assertEquals(BigDecimal.valueOf(0.2000).setScale(4), response.getLowConfidenceRate());
        assertEquals(2, response.getTrend().size());
        assertEquals("2026-03-02", response.getTrend().get(1).getDay());
        assertEquals(0L, response.getTrend().get(1).getTotalRecommendations());
        assertEquals(2L, response.getSatisfactionDistribution().get("FIT"));
    }

    @Test
    void shouldReturnZeroedRecommendationStatsWhenNoDataExists() {
        when(recommendationAnalyticsMapper.selectRecommendationStats(any(), any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(null);
        when(recommendationAnalyticsMapper.selectRecommendationTrend(any(), any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(null);

        RecommendationStatsDto response = service.getRecommendationStats(
                null,
                null,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 1)
        );

        assertEquals(0L, response.getTotalRecommendations());
        assertEquals(0L, response.getLinkedOrderCount());
        assertEquals(0L, response.getTotalFeedbacks());
        assertEquals(BigDecimal.ZERO.setScale(4), response.getLinkedOrderCoverage());
        assertEquals(1, response.getTrend().size());
        assertEquals(0L, response.getTrend().get(0).getTotalRecommendations());
    }

    @Test
    void shouldSupportFeedbackDistributionSortingAndPagination() {
        FeedbackDistributionDto group = new FeedbackDistributionDto();
        group.setGroupKey("88");
        group.setGroupName("夏季短袖");
        group.setTotalFeedback(11L);
        group.setFitRate(BigDecimal.valueOf(0.2727));

        Page<FeedbackDistributionDto> page = new Page<>(2, 20);
        page.setRecords(List.of(group));
        page.setTotal(41L);
        when(recommendationAnalyticsMapper.selectFeedbackDistribution(
                ArgumentMatchers.<Page<FeedbackDistributionDto>>any(),
                eq("product"),
                eq(12L),
                eq(88L),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq("totalFeedback"),
                eq("desc")
        )).thenReturn(page);

        FeedbackDistributionResponse response = service.getFeedbackDistribution(
                "product",
                12L,
                88L,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                20,
                20,
                "totalFeedback",
                "desc"
        );

        ArgumentCaptor<Page<FeedbackDistributionDto>> pageCaptor = ArgumentCaptor.forClass(feedbackDistributionPageClass());
        verify(recommendationAnalyticsMapper).selectFeedbackDistribution(
                pageCaptor.capture(),
                eq("product"),
                eq(12L),
                eq(88L),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq("totalFeedback"),
                eq("desc")
        );

        assertEquals(2L, pageCaptor.getValue().getCurrent());
        assertEquals(20L, pageCaptor.getValue().getSize());
        assertEquals(41L, response.getTotal());
        assertEquals(20L, response.getLimit());
        assertEquals(20L, response.getOffset());
        assertEquals(1, response.getGroups().size());
        assertEquals("夏季短袖", response.getGroups().get(0).getGroupName());
    }

    @Test
    void shouldReturnEmptyFeedbackDistributionPageWhenMapperHasNoRows() {
        Page<FeedbackDistributionDto> emptyPage = new Page<>(1, 200);
        emptyPage.setRecords(null);
        emptyPage.setTotal(0L);
        when(recommendationAnalyticsMapper.selectFeedbackDistribution(
                ArgumentMatchers.<Page<FeedbackDistributionDto>>any(),
                eq("size"),
                eq(null),
                eq(null),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq("fitRate"),
                eq("asc")
        )).thenReturn(emptyPage);

        FeedbackDistributionResponse response = service.getFeedbackDistribution(
                "size",
                null,
                null,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                999,
                0,
                null,
                null
        );

        assertTrue(response.getGroups().isEmpty());
        assertEquals(0L, response.getTotal());
        assertEquals(200L, response.getLimit());
        assertEquals(0L, response.getOffset());
    }

    @Test
    void shouldFilterLowConfidenceHotspotsAndClampRequestValues() {
        LowConfidenceHotspotDto hotspot = new LowConfidenceHotspotDto();
        hotspot.setUniformId(88L);
        hotspot.setUniformName("夏季短袖");
        hotspot.setSizeId(5L);
        hotspot.setSizeName("165");
        hotspot.setLowConfidenceCount(9L);
        hotspot.setTotalRecommendations(20L);
        hotspot.setLowConfidenceRate(BigDecimal.valueOf(0.45));
        hotspot.setAvgConfidence(52);

        when(recommendationAnalyticsMapper.selectLowConfidenceHotspots(
                eq(12L),
                eq(88L),
                eq(100),
                eq(200),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(hotspot));

        LowConfidenceHotspotsResponse response = service.getLowConfidenceHotspots(
                12L,
                88L,
                120,
                999,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        );

        assertNotNull(response);
        assertEquals(1, response.getHotspots().size());
        assertEquals("夏季短袖", response.getHotspots().get(0).getUniformName());
    }

    @Test
    void shouldReturnEmptyHotspotsWhenMapperReturnsNull() {
        when(recommendationAnalyticsMapper.selectLowConfidenceHotspots(
                eq(null),
                eq(null),
                eq(60),
                eq(20),
                eq(null),
                eq(null)
        )).thenReturn(null);

        LowConfidenceHotspotsResponse response = service.getLowConfidenceHotspots(null, null, null, null, null, null);

        assertNotNull(response);
        assertTrue(response.getHotspots().isEmpty());
    }

    @Test
    void shouldReturnExperimentMetricsForBothVariants() {
        RecommendationExperimentVariantMetricsDto a = new RecommendationExperimentVariantMetricsDto();
        a.setExperimentKey("exp-size");
        a.setExperimentVariant("A");
        a.setTotalRecommendations(10L);
        a.setLinkedOrderCount(4L);
        a.setTotalFeedbacks(2L);
        a.setFitCount(1L);
        a.setTooLargeCount(1L);
        a.setLowConfidenceCount(3L);
        a.setAvgConfidenceScore(70);

        RecommendationExperimentVariantMetricsDto b = new RecommendationExperimentVariantMetricsDto();
        b.setExperimentKey("exp-size");
        b.setExperimentVariant("B");
        b.setTotalRecommendations(20L);
        b.setLinkedOrderCount(10L);
        b.setTotalFeedbacks(5L);
        b.setFitCount(4L);
        b.setTooSmallCount(1L);
        b.setLowConfidenceCount(2L);
        b.setAvgConfidenceScore(82);
        b.setCalibrationHitCount(8L);
        b.setCalibrationAppliedCount(6L);
        b.setRecommendationChangedCount(3L);
        b.setSafetyGateSkipCount(2L);

        when(recommendationAnalyticsMapper.selectExperimentMetrics(
                eq(12L),
                eq(88L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(a, b));

        RecommendationExperimentMetricsResponse response = service.getExperimentMetrics(
                12L,
                88L,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        );

        assertEquals(2, response.getVariants().size());
        assertEquals("A", response.getVariants().get(0).getExperimentVariant());
        assertEquals(BigDecimal.valueOf(0.5000).setScale(4), response.getVariants().get(0).getFitRate());
        assertEquals(BigDecimal.ZERO.setScale(4), response.getVariants().get(0).getCalibrationHitRate());
        assertEquals("B", response.getVariants().get(1).getExperimentVariant());
        assertEquals(BigDecimal.valueOf(0.8000).setScale(4), response.getVariants().get(1).getFitRate());
        assertEquals(BigDecimal.valueOf(0.4000).setScale(4), response.getVariants().get(1).getCalibrationHitRate());
        assertEquals(BigDecimal.valueOf(0.3000).setScale(4), response.getVariants().get(1).getCalibrationApplicationRate());
        assertEquals(BigDecimal.valueOf(0.1500).setScale(4), response.getVariants().get(1).getRecommendationChangeRate());
        assertEquals(2L, response.getVariants().get(1).getSafetyGateSkipCount());
    }

    @Test
    void shouldReturnStableEmptyExperimentMetricsWhenNoRowsExist() {
        when(recommendationAnalyticsMapper.selectExperimentMetrics(
                eq(null),
                eq(null),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(null);

        RecommendationExperimentMetricsResponse response = service.getExperimentMetrics(
                null,
                null,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 1)
        );

        assertEquals(2, response.getVariants().size());
        assertEquals("A", response.getVariants().get(0).getExperimentVariant());
        assertEquals("B", response.getVariants().get(1).getExperimentVariant());
        assertEquals(0L, response.getVariants().get(0).getTotalRecommendations());
        assertEquals(BigDecimal.ZERO.setScale(4), response.getVariants().get(1).getRecommendationChangeRate());
    }

    @SuppressWarnings("unchecked")
    private static Class<Page<FeedbackDistributionDto>> feedbackDistributionPageClass() {
        return (Class<Page<FeedbackDistributionDto>>) (Class<?>) Page.class;
    }
}
