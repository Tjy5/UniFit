package com.suios.admin.analytics.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.suios.admin.analytics.dto.FeedbackDistributionDto;
import com.suios.admin.analytics.dto.FeedbackDistributionResponse;
import com.suios.admin.analytics.dto.LowConfidenceHotspotDto;
import com.suios.admin.analytics.dto.LowConfidenceHotspotsResponse;
import com.suios.admin.analytics.dto.RecommendationStatsDto;
import com.suios.admin.analytics.service.RecommendationAnalyticsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RecommendationAnalyticsControllerTest {

    @Mock
    private RecommendationAnalyticsService recommendationAnalyticsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RecommendationAnalyticsController(recommendationAnalyticsService))
                .build();
    }

    @Test
    void shouldExposeRecommendationStatsEndpoint() throws Exception {
        RecommendationStatsDto stats = new RecommendationStatsDto();
        stats.setTotalRecommendations(10L);
        stats.setLinkedOrderCoverage(BigDecimal.valueOf(0.4));
        when(recommendationAnalyticsService.getRecommendationStats(
                12L,
                88L,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        )).thenReturn(stats);

        mockMvc.perform(get("/analytics/recommendation-stats")
                        .param("schoolId", "12")
                        .param("uniformId", "88")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalRecommendations").value(10))
                .andExpect(jsonPath("$.data.linkedOrderCoverage").value(0.4));

        verify(recommendationAnalyticsService).getRecommendationStats(
                12L,
                88L,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        );
    }

    @Test
    void shouldExposeFeedbackDistributionEndpointWithPagingParams() throws Exception {
        FeedbackDistributionDto group = new FeedbackDistributionDto();
        group.setGroupKey("88");
        group.setGroupName("夏季短袖");

        FeedbackDistributionResponse response = new FeedbackDistributionResponse();
        response.setGroups(List.of(group));
        response.setTotal(1L);
        response.setLimit(20L);
        response.setOffset(20L);
        when(recommendationAnalyticsService.getFeedbackDistribution(
                "product",
                12L,
                88L,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                20,
                20,
                "totalFeedback",
                "desc"
        )).thenReturn(response);

        mockMvc.perform(get("/analytics/feedback-distribution")
                        .param("groupBy", "product")
                        .param("schoolId", "12")
                        .param("uniformId", "88")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31")
                        .param("limit", "20")
                        .param("offset", "20")
                        .param("sortBy", "totalFeedback")
                        .param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.groups[0].groupName").value("夏季短袖"));
    }

    @Test
    void shouldExposeLowConfidenceHotspotsEndpoint() throws Exception {
        LowConfidenceHotspotDto hotspot = new LowConfidenceHotspotDto();
        hotspot.setUniformId(88L);
        hotspot.setUniformName("夏季短袖");

        LowConfidenceHotspotsResponse response = new LowConfidenceHotspotsResponse();
        response.setHotspots(List.of(hotspot));
        when(recommendationAnalyticsService.getLowConfidenceHotspots(
                12L,
                88L,
                60,
                10,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        )).thenReturn(response);

        mockMvc.perform(get("/analytics/low-confidence-hotspots")
                        .param("schoolId", "12")
                        .param("uniformId", "88")
                        .param("threshold", "60")
                        .param("limit", "10")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.hotspots[0].uniformId").value(88))
                .andExpect(jsonPath("$.data.hotspots[0].uniformName").value("夏季短袖"));
    }
}
