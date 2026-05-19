package com.suios.admin.analytics.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.suios.admin.analytics.dto.CalibrationImpactResponseDto;
import com.suios.admin.analytics.dto.CalibrationParamDetailDto;
import com.suios.admin.analytics.dto.CalibrationParamListItemDto;
import com.suios.admin.analytics.service.CalibrationParameterAdminService;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.exception.GlobalExceptionHandler;
import com.suios.admin.common.page.PageResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CalibrationParameterAdminControllerTest {

    @Mock
    private CalibrationParameterAdminService calibrationParameterAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CalibrationParameterAdminController(calibrationParameterAdminService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldExposeAuthorizedListEndpoint() throws Exception {
        CalibrationParamListItemDto row = new CalibrationParamListItemDto();
        row.setParamId(10L);
        row.setScopeType("PRODUCT");
        row.setScopeId("88");
        row.setVersion(3);
        when(calibrationParameterAdminService.list(
                1,
                20,
                "PRODUCT",
                "88",
                5L,
                true,
                "ACTIVE",
                "HIGH",
                3
        )).thenReturn(new PageResult<>(List.of(row), 1, 1, 20));

        mockMvc.perform(get("/calibration/params/list")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .param("scopeType", "PRODUCT")
                        .param("scopeId", "88")
                        .param("targetSizeId", "5")
                        .param("enabled", "true")
                        .param("status", "ACTIVE")
                        .param("confidenceLevel", "HIGH")
                        .param("version", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].paramId").value(10));

        verify(calibrationParameterAdminService).list(1, 20, "PRODUCT", "88", 5L, true, "ACTIVE", "HIGH", 3);
    }

    @Test
    void shouldReturnErrorShapeWhenServiceRejectsUnauthenticatedRequest() throws Exception {
        when(calibrationParameterAdminService.detail(10L)).thenThrow(new BizException("登录状态无效"));

        mockMvc.perform(get("/calibration/params/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("登录状态无效"));
    }

    @Test
    void shouldExposeLifecycleAndImpactEndpoints() throws Exception {
        CalibrationParamDetailDto detail = new CalibrationParamDetailDto();
        detail.setParamId(10L);
        detail.setVersion(3);
        when(calibrationParameterAdminService.enable(10L, "reviewed")).thenReturn(detail);
        when(calibrationParameterAdminService.impact(
                10L,
                3,
                12L,
                88L,
                5L,
                null,
                null,
                20,
                0
        )).thenReturn(new CalibrationImpactResponseDto());

        mockMvc.perform(post("/calibration/params/10/enable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"reviewed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.paramId").value(10));

        mockMvc.perform(get("/calibration/params/10/impact")
                        .param("version", "3")
                        .param("schoolId", "12")
                        .param("uniformId", "88")
                        .param("sizeId", "5")
                        .param("limit", "20")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.summary.appliedCount").value(0));
    }
}
