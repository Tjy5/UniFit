package com.suios.admin.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suios.admin.analytics.dto.CalibrationImpactLogRow;
import com.suios.admin.analytics.dto.CalibrationImpactResponseDto;
import com.suios.admin.analytics.dto.CalibrationParamDetailDto;
import com.suios.admin.analytics.dto.CalibrationParamListItemDto;
import com.suios.admin.analytics.dto.CalibrationRollbackRequest;
import com.suios.admin.analytics.entity.CalibrationAuditRecord;
import com.suios.admin.analytics.entity.CalibrationParamRecord;
import com.suios.admin.analytics.mapper.CalibrationParameterAdminMapper;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.page.PageResult;
import com.suios.admin.common.security.AuthContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CalibrationParameterAdminServiceTest {

    @Mock
    private CalibrationParameterAdminMapper calibrationMapper;

    @Mock
    private AuthContext authContext;

    private CalibrationParameterAdminService service;

    @BeforeEach
    void setUp() {
        service = new CalibrationParameterAdminService(calibrationMapper, authContext, new ObjectMapper());
        ReflectionTestUtils.setField(service, "maxPageSize", 200);
        when(authContext.getCurrentUserId()).thenReturn(1L);
        lenient().when(authContext.getCurrentUsername()).thenReturn("admin");
    }

    @Test
    void shouldListFilterAndNormalizeFeedbackDistribution() {
        CalibrationParamListItemDto row = new CalibrationParamListItemDto();
        row.setParamId(10L);
        row.setFeedbackDistribution("{\"FIT\":8,\"TOO_SMALL\":2}");
        Page<CalibrationParamListItemDto> page = new Page<>(1, 20);
        page.setRecords(List.of(row));
        page.setTotal(1L);
        when(calibrationMapper.selectParamPage(any(), eq("PRODUCT"), eq("88"), eq(5L), eq(true), eq("ACTIVE"), eq("HIGH"), eq(3)))
                .thenReturn(page);

        PageResult<CalibrationParamListItemDto> result = service.list(
                1,
                20,
                "PRODUCT",
                "88",
                5L,
                true,
                "ACTIVE",
                "HIGH",
                3
        );

        assertEquals(1L, result.total());
        assertEquals(10L, result.records().get(0).getParamId());
        assertInstanceOf(Map.class, result.records().get(0).getFeedbackDistribution());
        assertEquals(Boolean.FALSE, result.records().get(0).getLatestVersion());
        assertEquals(Boolean.FALSE, result.records().get(0).getEffectiveNow());
        assertEquals(Boolean.FALSE, result.records().get(0).getShadowedByHigherVersion());
    }

    @Test
    void shouldRejectUnauthenticatedAccess() {
        when(authContext.getCurrentUserId()).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () ->
                service.detail(10L)
        );

        assertEquals("登录状态无效", exception.getMessage());
    }

    @Test
    void shouldRejectEnableForObservingParam() {
        CalibrationParamRecord record = record(10L, 2, BigDecimal.valueOf(-2));
        record.setStatus("OBSERVING");
        when(calibrationMapper.selectParamRecord(10L)).thenReturn(record);

        BizException exception = assertThrows(BizException.class, () ->
                service.enable(10L, "manual review")
        );

        assertTrue(exception.getMessage().contains("观察状态"));
    }

    @Test
    void shouldDisableParamAndWriteAuditSnapshot() {
        CalibrationParamRecord record = record(10L, 2, BigDecimal.valueOf(-2));
        record.setEnabled(true);
        record.setStatus("ACTIVE");
        when(calibrationMapper.selectParamRecord(10L)).thenReturn(record);
        when(calibrationMapper.selectParamDetail(10L)).thenReturn(detail(10L, 2));

        service.disable(10L, "bad live result");

        ArgumentCaptor<CalibrationParamRecord> recordCaptor = ArgumentCaptor.forClass(CalibrationParamRecord.class);
        ArgumentCaptor<CalibrationAuditRecord> auditCaptor = ArgumentCaptor.forClass(CalibrationAuditRecord.class);
        verify(calibrationMapper).updateParamRecord(recordCaptor.capture());
        verify(calibrationMapper).insertAuditRecord(auditCaptor.capture());
        assertEquals(Boolean.FALSE, recordCaptor.getValue().getEnabled());
        assertEquals("DISABLED", recordCaptor.getValue().getStatus());
        assertEquals("DISABLE", auditCaptor.getValue().getAction());
        assertEquals("bad live result", auditCaptor.getValue().getReason());
        assertTrue(auditCaptor.getValue().getOldValue().contains("\"enabled\":true"));
        assertTrue(auditCaptor.getValue().getNewValue().contains("\"enabled\":false"));
    }

    @Test
    void shouldRollbackByCreatingNextVersionAndAuditingSource() {
        CalibrationParamRecord reference = record(10L, 3, BigDecimal.valueOf(-3));
        CalibrationParamRecord source = record(8L, 1, BigDecimal.valueOf(2));
        CalibrationParamRecord active = record(10L, 3, BigDecimal.valueOf(-3));
        when(calibrationMapper.selectParamRecord(10L)).thenReturn(reference);
        when(calibrationMapper.selectParamRecord(8L)).thenReturn(source);
        when(calibrationMapper.selectEffectiveRecordsByIdentity(eq("PRODUCT"), eq("88"), eq(5L), eq("SCORE_OFFSET"), any(Date.class)))
                .thenReturn(List.of(active));
        when(calibrationMapper.selectLatestVersionByIdentity("PRODUCT", "88", 5L, "SCORE_OFFSET")).thenReturn(3);
        when(calibrationMapper.insertParamRecord(any())).thenAnswer(invocation -> {
            CalibrationParamRecord inserted = invocation.getArgument(0);
            inserted.setParamId(11L);
            return 1;
        });
        when(calibrationMapper.selectParamDetail(11L)).thenReturn(detail(11L, 4));
        CalibrationRollbackRequest request = new CalibrationRollbackRequest();
        request.setSourceParamId(8L);
        request.setReason("restore stable version");

        CalibrationParamDetailDto result = service.rollback(10L, request);

        ArgumentCaptor<CalibrationParamRecord> insertedCaptor = ArgumentCaptor.forClass(CalibrationParamRecord.class);
        ArgumentCaptor<CalibrationAuditRecord> auditCaptor = ArgumentCaptor.forClass(CalibrationAuditRecord.class);
        verify(calibrationMapper).updateParamRecord(active);
        verify(calibrationMapper).insertParamRecord(insertedCaptor.capture());
        verify(calibrationMapper).insertAuditRecord(auditCaptor.capture());
        assertEquals(11L, result.getParamId());
        assertEquals(4, insertedCaptor.getValue().getVersion());
        assertEquals(BigDecimal.valueOf(2), insertedCaptor.getValue().getAdjustmentValue());
        assertEquals(Boolean.TRUE, insertedCaptor.getValue().getIsManual());
        assertEquals("ROLLBACK", auditCaptor.getValue().getAction());
        assertTrue(auditCaptor.getValue().getNewValue().contains("\"sourceParamId\":8"));
        assertTrue(auditCaptor.getValue().getNewValue().contains("\"sourceVersion\":1"));
    }

    @Test
    void shouldAggregateScoreImpactRows() {
        CalibrationImpactLogRow row = new CalibrationImpactLogRow();
        row.setLogId(501L);
        row.setSchoolId(12L);
        row.setSchoolName("一中");
        row.setUniformId(88L);
        row.setUniformName("夏季短袖");
        row.setRecommendedSizeId(2L);
        row.setRecommendedSizeName("165");
        row.setCalibrationDetails("""
                {
                  "baseBestSizeId": 1,
                  "baseBestSizeName": "160",
                  "baseBestScore": 80,
                  "calibratedBestSizeId": 2,
                  "calibratedBestSizeName": "165",
                  "calibratedBestScore": 86,
                  "finalBestSizeId": 2,
                  "finalBestSizeName": "165",
                  "finalBestScore": 86,
                  "detailsBySizeId": {
                    "2": {
                      "paramId": 10,
                      "version": 3,
                      "calibrationType": "SCORE_OFFSET",
                      "source": "PRODUCT:88:SIZE:2",
                      "offset": 6,
                      "effectiveOffset": 6,
                      "sampleSize": 30,
                      "uniqueUserCount": 12,
                      "confidenceLevel": "HIGH",
                      "status": "ACTIVE"
                    }
                  }
                }
                """);
        when(calibrationMapper.selectImpactLogs(eq(12L), eq(88L), any(), any(), eq(10L), eq(3), eq(20)))
                .thenReturn(List.of(row));

        CalibrationImpactResponseDto result = service.impact(
                10L,
                3,
                12L,
                88L,
                2L,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                20,
                0
        );

        assertEquals(1L, result.getSummary().getAppliedCount());
        assertEquals(1L, result.getSummary().getBestSizeChangedCount());
        assertEquals(BigDecimal.valueOf(6).setScale(4), result.getSummary().getAvgScoreDelta());
        assertEquals("TRACEABLE", result.getRecords().get(0).getAttributionStatus());
        assertEquals(10L, result.getRecords().get(0).getMatchedParams().get(0).getParamId());
        assertEquals(3, result.getRecords().get(0).getMatchedParams().get(0).getVersion());
    }

    private CalibrationParamRecord record(Long paramId, Integer version, BigDecimal adjustment) {
        CalibrationParamRecord record = new CalibrationParamRecord();
        record.setParamId(paramId);
        record.setScopeType("PRODUCT");
        record.setScopeId("88");
        record.setTargetSizeId(5L);
        record.setCalibrationType("SCORE_OFFSET");
        record.setAdjustmentValue(adjustment);
        record.setEnabled(true);
        record.setStatus("ACTIVE");
        record.setVersion(version);
        record.setEffectiveFrom(new Date(System.currentTimeMillis() - 1000));
        record.setCreateBy("auto");
        record.setCreateTime(new Date());
        record.setUpdateBy("auto");
        record.setUpdateTime(new Date());
        return record;
    }

    private CalibrationParamDetailDto detail(Long paramId, Integer version) {
        CalibrationParamDetailDto detail = new CalibrationParamDetailDto();
        detail.setParamId(paramId);
        detail.setScopeType("PRODUCT");
        detail.setScopeId("88");
        detail.setTargetSizeId(5L);
        detail.setCalibrationType("SCORE_OFFSET");
        detail.setVersion(version);
        detail.setAdjustmentValue(BigDecimal.ONE);
        detail.setEnabled(true);
        detail.setStatus("ACTIVE");
        return detail;
    }
}
