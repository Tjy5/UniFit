package com.suios.admin.analytics.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.suios.admin.analytics.dto.BehaviorFunnelResponse;
import com.suios.admin.analytics.dto.BehaviorFunnelStageDto;
import com.suios.admin.analytics.dto.RecommendationEntryComparisonResponse;
import com.suios.admin.analytics.dto.StalledCartRowDto;
import com.suios.admin.analytics.dto.StalledCartsResponse;
import com.suios.admin.analytics.service.BehaviorFunnelService;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class BehaviorFunnelControllerTest {

    @Mock
    private BehaviorFunnelService behaviorFunnelService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BehaviorFunnelController(behaviorFunnelService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldExposeBehaviorFunnelEndpoint() throws Exception {
        BehaviorFunnelStageDto stage = new BehaviorFunnelStageDto();
        stage.setStage("browse");
        stage.setLabel("浏览");
        stage.setSessionsReached(10L);
        stage.setConversionRate(BigDecimal.valueOf(0.5000));

        BehaviorFunnelResponse response = new BehaviorFunnelResponse();
        response.setStages(List.of(stage));
        response.setTotalSessions(10L);
        response.setSessionInactivityGapMinutes(30);
        when(behaviorFunnelService.getBehaviorFunnel(
                12L,
                3L,
                88L,
                "mall-home-dialog",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 18)
        )).thenReturn(response);

        mockMvc.perform(get("/analytics/behavior-funnel")
                        .param("schoolId", "12")
                        .param("gradeId", "3")
                        .param("uniformId", "88")
                        .param("source", "mall-home-dialog")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalSessions").value(10))
                .andExpect(jsonPath("$.data.stages[0].stage").value("browse"));

        verify(behaviorFunnelService).getBehaviorFunnel(
                12L,
                3L,
                88L,
                "mall-home-dialog",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 18)
        );
    }

    @Test
    void shouldExposeEmptyEntryComparisonResponse() throws Exception {
        RecommendationEntryComparisonResponse response = new RecommendationEntryComparisonResponse();
        response.setRows(List.of());
        when(behaviorFunnelService.getEntryComparison(
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 18)
        )).thenReturn(response);

        mockMvc.perform(get("/analytics/recommendation-entry-comparison")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.rows").isArray())
                .andExpect(jsonPath("$.data.rows.length()").value(0));
    }

    @Test
    void shouldExposeRecommendationAdoptionEndpointAndReturnBusinessErrorShape() throws Exception {
        when(behaviorFunnelService.getRecommendationAdoption(
                12L,
                null,
                null,
                null,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 18)
        )).thenThrow(new BizException("登录状态无效"));

        mockMvc.perform(get("/analytics/recommendation-adoption")
                        .param("schoolId", "12")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("登录状态无效"));
    }

    @Test
    void shouldExposeDropOffEndpointAndPropagateRangeRejection() throws Exception {
        when(behaviorFunnelService.getDropOffDistribution(
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 8, 1),
                20
        )).thenThrow(new BizException("行为分析日期范围不能超过90天"));

        mockMvc.perform(get("/analytics/behavior-drop-off")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-08-01")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("行为分析日期范围不能超过90天"));
    }

    @Test
    void shouldExposeStalledCartsEndpointWithPaginationParams() throws Exception {
        StalledCartRowDto row = new StalledCartRowDto();
        row.setUserId(42L);
        row.setUserAccount("lihua");
        row.setUniformId(88L);
        row.setUniformName("夏季短袖");
        row.setSizeId(5L);
        row.setSizeName("165");
        row.setQuantity(2);
        row.setAddedAt(LocalDateTime.of(2026, 5, 14, 8, 0));
        row.setHoursSinceAdd(72L);
        row.setRecommendationLogId(100L);
        row.setLastActivityAt(LocalDateTime.of(2026, 5, 15, 8, 0));

        StalledCartsResponse response = new StalledCartsResponse();
        response.setRecords(List.of(row));
        response.setTotal(1L);
        response.setLimit(50);
        response.setOffset(0);
        when(behaviorFunnelService.getStalledCarts(
                12L,
                3L,
                88L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 18),
                999,
                -10
        )).thenReturn(response);

        mockMvc.perform(get("/analytics/stalled-carts")
                        .param("schoolId", "12")
                        .param("gradeId", "3")
                        .param("uniformId", "88")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-18")
                        .param("limit", "999")
                        .param("offset", "-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.limit").value(50))
                .andExpect(jsonPath("$.data.records[0].userAccount").value("lihua"));

        verify(behaviorFunnelService).getStalledCarts(
                12L,
                3L,
                88L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 18),
                999,
                -10
        );
    }
}
