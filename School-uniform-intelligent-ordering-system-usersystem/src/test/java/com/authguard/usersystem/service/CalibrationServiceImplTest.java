package com.authguard.usersystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.authguard.usersystem.config.CalibrationProperties;
import com.authguard.usersystem.dto.CalibrationBundle;
import com.authguard.usersystem.dto.FeedbackAggregation;
import com.authguard.usersystem.entity.CalibrationParams;
import com.authguard.usersystem.entity.SSizes;
import com.authguard.usersystem.mapper.CalibrationAuditMapper;
import com.authguard.usersystem.mapper.CalibrationParamsMapper;
import com.authguard.usersystem.mapper.SSizesMapper;
import com.authguard.usersystem.mapper.SizeFeedbackMapper;
import com.authguard.usersystem.service.impl.CalibrationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalibrationServiceImplTest {

    @Mock
    private CalibrationParamsMapper calibrationParamsMapper;

    @Mock
    private CalibrationAuditMapper calibrationAuditMapper;

    @Mock
    private SizeFeedbackMapper sizeFeedbackMapper;

    @Mock
    private SSizesMapper sSizesMapper;

    private CalibrationServiceImpl service;

    @BeforeEach
    void setUp() {
        CalibrationProperties properties = new CalibrationProperties();
        properties.setEnabled(true);
        properties.setCacheTtlMinutes(15);
        properties.setDecayDays(90);
        properties.setWindowDays(180);
        properties.getThresholds().setMinFeedbackCount(20);
        properties.getThresholds().setMinUniqueUsers(10);
        service = new CalibrationServiceImpl(
                calibrationParamsMapper,
                calibrationAuditMapper,
                sizeFeedbackMapper,
                sSizesMapper,
                properties,
                new ObjectMapper()
        );
    }

    @Test
    void shouldResolveCalibrationByPriority() {
        CalibrationParams productSize = param("PRODUCT", "88", 5L, BigDecimal.valueOf(-4));
        when(calibrationParamsMapper.selectActiveByScope(eq("PRODUCT"), eq("88"), eq(5L), any()))
                .thenReturn(List.of(productSize));
        when(calibrationParamsMapper.selectActiveByScope(eq("PRODUCT"), eq("88"), eq(6L), any()))
                .thenReturn(List.of());
        when(calibrationParamsMapper.selectActiveByScope(eq("PRODUCT"), eq("88"), eq(null), any()))
                .thenReturn(List.of(param("PRODUCT", "88", null, BigDecimal.valueOf(2))));
        when(calibrationParamsMapper.selectActiveByScope(eq("SCHOOL"), eq("12"), eq(6L), any()))
                .thenReturn(List.of(param("SCHOOL", "12", 6L, BigDecimal.valueOf(-1))));
        when(calibrationParamsMapper.selectActiveByScope(eq("SCHOOL"), eq("12"), eq(5L), any()))
                .thenReturn(List.of());
        when(calibrationParamsMapper.selectActiveByScope(eq("SCHOOL"), eq("12"), eq(null), any()))
                .thenReturn(List.of());
        when(calibrationParamsMapper.selectActiveByScope(eq("GLOBAL"), eq(null), eq(5L), any()))
                .thenReturn(List.of());
        when(calibrationParamsMapper.selectActiveByScope(eq("GLOBAL"), eq(null), eq(6L), any()))
                .thenReturn(List.of());
        when(calibrationParamsMapper.selectActiveByScope(eq("GLOBAL"), eq(null), eq(null), any()))
                .thenReturn(List.of());

        CalibrationBundle bundle = service.loadCalibrationSet(12L, 88L, List.of(5L, 6L));

        assertEquals(BigDecimal.valueOf(-4).setScale(4), bundle.getOffsetsBySizeId().get(5L));
        assertEquals("PRODUCT:88:SIZE:5", bundle.getProvenanceBySizeId().get(5L));
        assertEquals(BigDecimal.valueOf(2).setScale(4), bundle.getOffsetsBySizeId().get(6L));
        assertEquals("PRODUCT:88", bundle.getProvenanceBySizeId().get(6L));
    }

    @Test
    void shouldExposeParameterMetadataInCalibrationDetails() {
        CalibrationParams productSize = param("PRODUCT", "88", 5L, BigDecimal.valueOf(-4));
        productSize.setParamId(101L);
        productSize.setVersion(3);
        productSize.setSampleSize(36);
        productSize.setUniqueUserCount(14);
        productSize.setConfidenceLevel("MEDIUM");
        productSize.setStatus("ACTIVE");
        when(calibrationAuditMapper.selectLatestAuditRevision()).thenReturn(7L);
        when(calibrationParamsMapper.selectActiveByScope(eq("PRODUCT"), eq("88"), eq(5L), any()))
                .thenReturn(List.of(productSize));

        CalibrationBundle bundle = service.loadCalibrationSet(12L, 88L, List.of(5L));

        assertEquals(101L, bundle.getDetailsBySizeId().get(5L).get("paramId"));
        assertEquals(3, bundle.getDetailsBySizeId().get(5L).get("version"));
        assertEquals("SCORE_OFFSET", bundle.getDetailsBySizeId().get(5L).get("calibrationType"));
        assertEquals("PRODUCT:88:SIZE:5", bundle.getDetailsBySizeId().get(5L).get("source"));
        assertEquals(36, bundle.getDetailsBySizeId().get(5L).get("sampleSize"));
        assertEquals(14, bundle.getDetailsBySizeId().get(5L).get("uniqueUserCount"));
        assertEquals("MEDIUM", bundle.getDetailsBySizeId().get(5L).get("confidenceLevel"));
        assertEquals("ACTIVE", bundle.getDetailsBySizeId().get(5L).get("status"));
    }

    @Test
    void shouldInvalidateCachedBundleWhenAuditRevisionChanges() {
        CalibrationParams first = param("PRODUCT", "88", 5L, BigDecimal.valueOf(-1));
        first.setParamId(101L);
        first.setVersion(1);
        first.setStatus("ACTIVE");
        CalibrationParams second = param("PRODUCT", "88", 5L, BigDecimal.valueOf(-3));
        second.setParamId(102L);
        second.setVersion(2);
        second.setStatus("ACTIVE");
        when(calibrationAuditMapper.selectLatestAuditRevision()).thenReturn(1L).thenReturn(1L).thenReturn(2L);
        when(calibrationParamsMapper.selectActiveByScope(eq("PRODUCT"), eq("88"), eq(5L), any()))
                .thenReturn(List.of(first))
                .thenReturn(List.of(second));

        CalibrationBundle initial = service.loadCalibrationSet(12L, 88L, List.of(5L));
        CalibrationBundle cached = service.loadCalibrationSet(12L, 88L, List.of(5L));
        CalibrationBundle refreshed = service.loadCalibrationSet(12L, 88L, List.of(5L));

        assertEquals(BigDecimal.valueOf(-1).setScale(4), initial.getOffsetsBySizeId().get(5L));
        assertEquals(BigDecimal.valueOf(-1).setScale(4), cached.getOffsetsBySizeId().get(5L));
        assertEquals(BigDecimal.valueOf(-3).setScale(4), refreshed.getOffsetsBySizeId().get(5L));
        assertEquals(102L, refreshed.getDetailsBySizeId().get(5L).get("paramId"));
        verify(calibrationParamsMapper, times(2)).selectActiveByScope(eq("PRODUCT"), eq("88"), eq(5L), any());
    }

    @Test
    void shouldClampAdjustedScores() {
        CalibrationBundle bundle = CalibrationBundle.empty(List.of(1L, 2L));
        bundle.getOffsetsBySizeId().put(1L, BigDecimal.valueOf(15));
        bundle.getOffsetsBySizeId().put(2L, BigDecimal.valueOf(-30));
        LinkedHashMap<Long, BigDecimal> baseScores = new LinkedHashMap<>();
        baseScores.put(1L, BigDecimal.valueOf(92));
        baseScores.put(2L, BigDecimal.valueOf(18));

        var adjusted = service.applyScoreOffsets(baseScores, bundle);

        assertEquals(BigDecimal.valueOf(100).setScale(2), adjusted.get(1L));
        assertEquals(BigDecimal.ZERO.setScale(2), adjusted.get(2L));
    }

    @Test
    void shouldIgnorePositiveOffsetsForUnsafeCandidatesAndKeepNegativeOffsets() {
        CalibrationBundle bundle = CalibrationBundle.empty(List.of(1L, 2L));
        bundle.getOffsetsBySizeId().put(1L, BigDecimal.valueOf(12));
        bundle.getOffsetsBySizeId().put(2L, BigDecimal.valueOf(-8));
        LinkedHashMap<Long, BigDecimal> baseScores = new LinkedHashMap<>();
        baseScores.put(1L, BigDecimal.valueOf(60));
        baseScores.put(2L, BigDecimal.valueOf(60));
        LinkedHashMap<Long, Boolean> safety = new LinkedHashMap<>();
        safety.put(1L, false);
        safety.put(2L, false);

        var adjusted = service.applyScoreOffsets(baseScores, bundle, safety);

        assertEquals(BigDecimal.valueOf(60).setScale(2), adjusted.get(1L));
        assertEquals(BigDecimal.valueOf(52).setScale(2), adjusted.get(2L));
    }

    @Test
    void shouldGenerateObservingAndActiveParamsBasedOnThresholds() {
        FeedbackAggregation observing = new FeedbackAggregation();
        observing.setScopeType("GLOBAL");
        observing.setSampleSize(5);
        observing.setUniqueUserCount(3);
        observing.setWeightedFitCount(BigDecimal.ONE);
        observing.setWeightedTooLargeCount(BigDecimal.ONE);
        observing.setWeightedTooSmallCount(BigDecimal.ONE);
        observing.setTotalWeightedCount(BigDecimal.valueOf(3));
        observing.setFeedbackDistribution("{\"fit\":1}");

        FeedbackAggregation active = new FeedbackAggregation();
        active.setScopeType("PRODUCT");
        active.setScopeId("88");
        active.setTargetSizeId(5L);
        active.setSampleSize(25);
        active.setUniqueUserCount(12);
        active.setWeightedFitCount(BigDecimal.valueOf(2));
        active.setWeightedTooLargeCount(BigDecimal.valueOf(6));
        active.setWeightedTooSmallCount(BigDecimal.valueOf(2));
        active.setTotalWeightedCount(BigDecimal.TEN);
        active.setFeedbackDistribution("{\"fit\":2}");

        when(calibrationParamsMapper.selectLatestVersion(eq("GLOBAL"), eq(null), eq(null))).thenReturn(0);
        when(calibrationParamsMapper.selectLatestVersion(eq("PRODUCT"), eq("88"), eq(5L))).thenReturn(2);

        List<CalibrationParams> params = service.generateCalibrationParams(List.of(observing, active));

        assertEquals("OBSERVING", params.get(0).getStatus());
        assertEquals(Boolean.FALSE, params.get(0).getEnabled());
        assertEquals("ACTIVE", params.get(1).getStatus());
        assertEquals(Boolean.TRUE, params.get(1).getEnabled());
        assertTrue(params.get(1).getAdjustmentValue().compareTo(BigDecimal.valueOf(-5)) <= 0);
        assertEquals(3, params.get(1).getVersion());
    }

    @Test
    void shouldGenerateDirectionAwareAdjacentParamWhenTooSmallDominates() {
        FeedbackAggregation active = new FeedbackAggregation();
        active.setScopeType("PRODUCT");
        active.setScopeId("88");
        active.setTargetSizeId(5L);
        active.setSampleSize(25);
        active.setUniqueUserCount(12);
        active.setWeightedFitCount(BigDecimal.ONE);
        active.setWeightedTooSmallCount(BigDecimal.valueOf(8));
        active.setWeightedTooLargeCount(BigDecimal.ONE);
        active.setTotalWeightedCount(BigDecimal.TEN);

        when(sSizesMapper.selectAllSizes()).thenReturn(List.of(size(5L, 150, 40), size(6L, 160, 50)));
        when(calibrationParamsMapper.selectLatestVersion(eq("PRODUCT"), eq("88"), eq(5L))).thenReturn(0);
        when(calibrationParamsMapper.selectLatestVersion(eq("PRODUCT"), eq("88"), eq(6L))).thenReturn(0);

        List<CalibrationParams> params = service.generateCalibrationParams(List.of(active));

        assertEquals(2, params.size());
        assertEquals(5L, params.get(0).getTargetSizeId());
        assertTrue(params.get(0).getAdjustmentValue().compareTo(BigDecimal.ZERO) < 0);
        assertEquals(6L, params.get(1).getTargetSizeId());
        assertTrue(params.get(1).getAdjustmentValue().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(params.get(1).getAdjustmentValue().compareTo(BigDecimal.valueOf(5)) <= 0);
        assertTrue(params.get(1).getFeedbackDistribution().contains("TOO_SMALL"));
    }

    @Test
    void shouldOnlyPenalizeCurrentSizeWhenAdjacentCannotBeResolved() {
        FeedbackAggregation active = new FeedbackAggregation();
        active.setScopeType("PRODUCT");
        active.setScopeId("88");
        active.setTargetSizeId(5L);
        active.setSampleSize(25);
        active.setUniqueUserCount(12);
        active.setWeightedFitCount(BigDecimal.ONE);
        active.setWeightedTooSmallCount(BigDecimal.valueOf(8));
        active.setWeightedTooLargeCount(BigDecimal.ONE);
        active.setTotalWeightedCount(BigDecimal.TEN);

        when(sSizesMapper.selectAllSizes()).thenReturn(List.of(size(5L, 150, 40)));
        when(calibrationParamsMapper.selectLatestVersion(eq("PRODUCT"), eq("88"), eq(5L))).thenReturn(0);

        List<CalibrationParams> params = service.generateCalibrationParams(List.of(active));

        assertEquals(1, params.size());
        assertEquals(5L, params.get(0).getTargetSizeId());
        assertTrue(params.get(0).getAdjustmentValue().compareTo(BigDecimal.ZERO) < 0);
        assertTrue(params.get(0).getFeedbackDistribution().contains("TOO_SMALL"));
    }

    @Test
    void shouldPersistGeneratedParamsAndAudits() {
        CalibrationParams param = param("GLOBAL", null, null, BigDecimal.valueOf(-5));
        param.setStatus("ACTIVE");
        param.setEnabled(true);
        param.setSampleSize(30);
        param.setUniqueUserCount(12);
        when(calibrationParamsMapper.selectLatestVersion(eq("GLOBAL"), eq(null), eq(null))).thenReturn(0);
        when(calibrationParamsMapper.insert(any())).thenAnswer(invocation -> {
            CalibrationParams saved = invocation.getArgument(0);
            saved.setParamId(11L);
            return 1;
        });

        service.saveGeneratedParams(List.of(param), "auto-job", "test-run");

        verify(calibrationParamsMapper).insert(any(CalibrationParams.class));
        verify(calibrationAuditMapper).insert(any());
    }

    private CalibrationParams param(String scopeType, String scopeId, Long targetSizeId, BigDecimal adjustment) {
        CalibrationParams param = new CalibrationParams();
        param.setScopeType(scopeType);
        param.setScopeId(scopeId);
        param.setTargetSizeId(targetSizeId);
        param.setAdjustmentValue(adjustment.setScale(4));
        param.setCalibrationType("SCORE_OFFSET");
        return param;
    }

    private SSizes size(Long id, long minHeight, long minWeight) {
        SSizes size = new SSizes();
        size.setId(id);
        size.setMinHeight(minHeight);
        size.setMinWeight(BigDecimal.valueOf(minWeight));
        return size;
    }
}
