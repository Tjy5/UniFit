package com.authguard.usersystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.authguard.usersystem.dto.CalibrationBundle;
import com.authguard.usersystem.dto.SizeRecommendationExperimentAssignment;
import com.authguard.usersystem.dto.SizePreferenceAdjustmentResult;
import com.authguard.usersystem.dto.SizeRecommendationRequest;
import com.authguard.usersystem.dto.SizeRecommendationResponse;
import com.authguard.usersystem.entity.RecommendationLog;
import com.authguard.usersystem.entity.SSizes;
import com.authguard.usersystem.entity.UserMessage;
import com.authguard.usersystem.mapper.RecommendationLogMapper;
import com.authguard.usersystem.mapper.SSizesMapper;
import com.authguard.usersystem.mapper.UserMessageMapper;
import com.authguard.usersystem.service.impl.SizeRecommendationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SizeRecommendationServiceImplTest {

    @Mock
    private UserMessageMapper userMessageMapper;

    @Mock
    private SSizesMapper sSizesMapper;

    @Mock
    private RecommendationLogMapper recommendationLogMapper;

    @Mock
    private CalibrationService calibrationService;

    @Mock
    private SizePreferenceService sizePreferenceService;

    @Mock
    private SizeRecommendationExperimentAssigner experimentAssigner;

    private SizeRecommendationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SizeRecommendationServiceImpl(
                userMessageMapper,
                sSizesMapper,
                new ConfidenceCalculator(),
                recommendationLogMapper,
                calibrationService,
                sizePreferenceService,
                experimentAssigner,
                new ObjectMapper()
        );
        lenient().when(calibrationService.loadCalibrationSet(any(), any(), any()))
                .thenReturn(CalibrationBundle.empty(List.of()));
        lenient().when(calibrationService.applyScoreOffsets(anyMap(), any()))
                .thenAnswer(invocation -> new LinkedHashMap<>(invocation.getArgument(0)));
        lenient().when(calibrationService.applyScoreOffsets(anyMap(), any(), anyMap()))
                .thenAnswer(invocation -> new LinkedHashMap<>(invocation.getArgument(0)));
        lenient().when(sizePreferenceService.resolveCategoryKey(any())).thenReturn("GENERAL");
        lenient().when(sizePreferenceService.buildAdjustment(any(), any(), any()))
                .thenAnswer(invocation -> {
                    SizePreferenceAdjustmentResult result = new SizePreferenceAdjustmentResult();
                    result.setCategoryKey(invocation.getArgument(1));
                    return result;
                });
        lenient().when(experimentAssigner.assign(any())).thenReturn(SizeRecommendationExperimentAssignment.disabled());
    }

    @Test
    void shouldRecommendForLegacyUserWithOnlyHeightAndWeight() {
        UserMessage legacyProfile = new UserMessage();
        legacyProfile.setHeight(BigDecimal.valueOf(165));
        legacyProfile.setWeight(BigDecimal.valueOf(52));
        when(userMessageMapper.selectByMessageUserId(7L)).thenReturn(legacyProfile);
        when(sSizesMapper.selectAllSizes()).thenReturn(List.of(size(1L, "160", 155, 165, 45, 55), size(2L, "165", 160, 170, 50, 60)));

        SizeRecommendationResponse response = service.recommend(7L, new SizeRecommendationRequest());

        assertTrue(response.isAvailable());
        assertEquals("165", response.getRecommended().getSizeName());
        assertEquals(List.of("身高", "体重"), response.getUsedDimensions());
        assertTrue(response.getMessage().contains("身高、体重"));
        assertTrue(response.getReasons().stream().anyMatch(reason -> reason.contains("身高和体重")));
        assertFalse(response.getReasons().stream().anyMatch(reason -> reason.contains("胸围") || reason.contains("腰围") || reason.contains("臀围") || reason.contains("肩宽")));
        verify(recommendationLogMapper).insert(any());
    }

    @Test
    void shouldRecommendWhenOptionalMeasurementsAreEmpty() {
        UserMessage savedProfile = new UserMessage();
        savedProfile.setHeight(BigDecimal.valueOf(165));
        savedProfile.setWeight(BigDecimal.valueOf(52));
        when(userMessageMapper.selectByMessageUserId(12L)).thenReturn(savedProfile);
        when(sSizesMapper.selectAllSizes()).thenReturn(List.of(
                sizeWithAllOptional(1L, "160", 155, 165, 45, 55, 78, 84, 58, 66, 82, 90, 35, 39),
                sizeWithAllOptional(2L, "165", 160, 170, 50, 60, 84, 90, 64, 72, 88, 96, 38, 42)
        ));

        SizeRecommendationResponse response = service.recommend(12L, new SizeRecommendationRequest());

        assertTrue(response.isAvailable());
        assertEquals("165", response.getRecommended().getSizeName());
        assertEquals(List.of("身高", "体重"), response.getUsedDimensions());
        assertFalse(response.getMessage().contains("胸围"));
        assertFalse(response.getReasons().stream().anyMatch(reason -> reason.contains("补全胸围") || reason.contains("补齐腰围")));
    }

    @Test
    void shouldUseSavedOptionalMeasurementsWhenRequestOmitsThem() {
        UserMessage savedProfile = new UserMessage();
        savedProfile.setHeight(BigDecimal.valueOf(165));
        savedProfile.setWeight(BigDecimal.valueOf(55));
        savedProfile.setChest(BigDecimal.valueOf(86));
        when(userMessageMapper.selectByMessageUserId(8L)).thenReturn(savedProfile);
        when(sSizesMapper.selectAllSizes()).thenReturn(List.of(
                sizeWithChest(1L, "160", 155, 165, 45, 55, 78, 84),
                sizeWithChest(2L, "165", 160, 170, 50, 60, 84, 90)
        ));

        SizeRecommendationRequest request = new SizeRecommendationRequest();
        request.setHeight(BigDecimal.valueOf(165));
        request.setWeight(BigDecimal.valueOf(55));
        SizeRecommendationResponse response = service.recommend(8L, request);

        assertTrue(response.isAvailable());
        assertEquals("165", response.getRecommended().getSizeName());
        assertTrue(response.getUsedDimensions().contains("胸围"));
        assertTrue(response.getReasons().stream().anyMatch(reason -> reason.contains("已结合胸围")));
        assertTrue(response.getReasons().stream().anyMatch(reason -> reason.contains("已自动补全")));
    }

    @Test
    void shouldRejectRecommendationWhenHeightAndWeightAreMissing() {
        when(userMessageMapper.selectByMessageUserId(9L)).thenReturn(new UserMessage());

        SizeRecommendationResponse response = service.recommend(9L, new SizeRecommendationRequest());

        assertFalse(response.isAvailable());
        assertNotNull(response.getMessage());
        assertTrue(response.getMessage().contains("身高和体重"));
    }

    @Test
    void shouldPersistRecommendationContextAndReturnLogId() {
        UserMessage savedProfile = new UserMessage();
        savedProfile.setHeight(BigDecimal.valueOf(165));
        savedProfile.setWeight(BigDecimal.valueOf(52));
        savedProfile.setSchoolId(12L);
        when(userMessageMapper.selectByMessageUserId(10L)).thenReturn(savedProfile);
        when(sSizesMapper.selectAllSizes()).thenReturn(List.of(
                size(1L, "160", 155, 165, 45, 55),
                size(2L, "165", 160, 170, 50, 60)
        ));
        doAnswer(invocation -> {
            RecommendationLog log = invocation.getArgument(0);
            log.setLogId(321L);
            return 1;
        }).when(recommendationLogMapper).insert(any(RecommendationLog.class));
        when(calibrationService.loadCalibrationSet(eq(12L), eq(88L), any()))
                .thenReturn(CalibrationBundle.empty(List.of(1L, 2L)));

        SizeRecommendationRequest request = new SizeRecommendationRequest();
        request.setUniformId(88L);
        request.setSource("product-detail");
        SizeRecommendationResponse response = service.recommend(10L, request);

        ArgumentCaptor<RecommendationLog> logCaptor = ArgumentCaptor.forClass(RecommendationLog.class);
        verify(recommendationLogMapper).insert(logCaptor.capture());
        RecommendationLog savedLog = logCaptor.getValue();

        assertTrue(response.isAvailable());
        assertEquals(321L, response.getRecommendationLogId());
        assertFalse(response.isCalibrationApplied());
        assertEquals(Long.valueOf(12L), savedLog.getSchoolId());
        assertEquals(Long.valueOf(88L), savedLog.getUniformId());
        assertEquals("product-detail", savedLog.getRequestSource());
        assertEquals("165", savedLog.getRecommendedSizeName());
        assertEquals(Boolean.FALSE, savedLog.getCalibrationApplied());
        assertNotNull(savedLog.getCalibrationDetails());
        assertEquals("GENERAL", savedLog.getCategoryKey());
        assertEquals(Boolean.FALSE, savedLog.getPersonalizationApplied());
    }

    @Test
    void shouldPersistExperimentAttributionForUnavailableRecommendation() {
        when(userMessageMapper.selectByMessageUserId(30L)).thenReturn(new UserMessage());
        when(experimentAssigner.assign(30L)).thenReturn(new SizeRecommendationExperimentAssignment(true, "exp-size", "B", "v2"));

        SizeRecommendationResponse response = service.recommend(30L, new SizeRecommendationRequest());

        ArgumentCaptor<RecommendationLog> logCaptor = ArgumentCaptor.forClass(RecommendationLog.class);
        verify(recommendationLogMapper).insert(logCaptor.capture());
        RecommendationLog savedLog = logCaptor.getValue();

        assertFalse(response.isAvailable());
        assertEquals("exp-size", savedLog.getExperimentKey());
        assertEquals("B", savedLog.getExperimentVariant());
        assertEquals("v2", savedLog.getStrategyVersion());
    }

    @Test
    void shouldBypassGroupCalibrationForVariantAWhileKeepingPersonalization() {
        UserMessage savedProfile = new UserMessage();
        savedProfile.setHeight(BigDecimal.valueOf(162));
        savedProfile.setWeight(BigDecimal.valueOf(53));
        savedProfile.setSchoolId(12L);
        when(userMessageMapper.selectByMessageUserId(31L)).thenReturn(savedProfile);
        when(sSizesMapper.selectAllSizes()).thenReturn(List.of(
                size(1L, "160", 160, 170, 50, 60),
                size(2L, "165", 160, 170, 50, 60)
        ));
        when(experimentAssigner.assign(31L)).thenReturn(new SizeRecommendationExperimentAssignment(true, "exp-size", "A", "v1"));
        when(sizePreferenceService.resolveCategoryKey(88L)).thenReturn("JACKET");
        SizePreferenceAdjustmentResult adjustment = new SizePreferenceAdjustmentResult();
        adjustment.setApplied(true);
        adjustment.setCategoryKey("JACKET");
        adjustment.setPreference("LOOSE");
        adjustment.setSummary("已参考个人偏宽松尺码偏好");
        adjustment.getOffsetsBySizeId().put(1L, BigDecimal.ZERO);
        adjustment.getOffsetsBySizeId().put(2L, BigDecimal.valueOf(8));
        when(sizePreferenceService.buildAdjustment(eq(31L), eq("JACKET"), any())).thenReturn(adjustment);

        SizeRecommendationRequest request = new SizeRecommendationRequest();
        request.setUniformId(88L);
        SizeRecommendationResponse response = service.recommend(31L, request);

        ArgumentCaptor<RecommendationLog> logCaptor = ArgumentCaptor.forClass(RecommendationLog.class);
        verify(recommendationLogMapper).insert(logCaptor.capture());
        RecommendationLog savedLog = logCaptor.getValue();

        verify(calibrationService, org.mockito.Mockito.never()).loadCalibrationSet(any(), any(), any());
        assertFalse(response.isCalibrationApplied());
        assertTrue(response.isPersonalizationApplied());
        assertEquals("165", response.getRecommended().getSizeName());
        assertEquals("A", savedLog.getExperimentVariant());
        assertFalse(response.getReasons().stream().anyMatch(reason -> reason.contains("实验")));
    }

    @Test
    void shouldUseCalibrationForVariantBAndPersistDiagnostics() {
        UserMessage savedProfile = new UserMessage();
        savedProfile.setHeight(BigDecimal.valueOf(165));
        savedProfile.setWeight(BigDecimal.valueOf(52));
        savedProfile.setSchoolId(12L);
        when(userMessageMapper.selectByMessageUserId(32L)).thenReturn(savedProfile);
        when(sSizesMapper.selectAllSizes()).thenReturn(List.of(
                size(1L, "160", 155, 165, 45, 55),
                size(2L, "165", 160, 170, 50, 60)
        ));
        when(experimentAssigner.assign(32L)).thenReturn(new SizeRecommendationExperimentAssignment(true, "exp-size", "B", "v1"));
        CalibrationBundle bundle = CalibrationBundle.empty(List.of(1L, 2L));
        bundle.getOffsetsBySizeId().put(1L, BigDecimal.valueOf(15));
        bundle.getDetailsBySizeId().put(1L, new LinkedHashMap<>(Map.of("paramId", 101L, "offset", BigDecimal.valueOf(15))));
        when(calibrationService.loadCalibrationSet(eq(12L), eq(88L), any())).thenReturn(bundle);
        when(calibrationService.applyScoreOffsets(anyMap(), eq(bundle), anyMap())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var baseScores = (LinkedHashMap<Long, BigDecimal>) invocation.getArgument(0);
            LinkedHashMap<Long, BigDecimal> adjusted = new LinkedHashMap<>();
            adjusted.put(1L, baseScores.get(1L).add(BigDecimal.valueOf(15)));
            adjusted.put(2L, baseScores.get(2L));
            return adjusted;
        });

        SizeRecommendationRequest request = new SizeRecommendationRequest();
        request.setUniformId(88L);
        SizeRecommendationResponse response = service.recommend(32L, request);

        ArgumentCaptor<RecommendationLog> logCaptor = ArgumentCaptor.forClass(RecommendationLog.class);
        verify(recommendationLogMapper).insert(logCaptor.capture());
        RecommendationLog savedLog = logCaptor.getValue();

        assertTrue(response.isCalibrationApplied());
        assertEquals("160", response.getRecommended().getSizeName());
        assertEquals("B", savedLog.getExperimentVariant());
        assertEquals(Boolean.TRUE, response.getCalibrationDetails().get("parameterHit"));
        assertEquals(Boolean.TRUE, response.getCalibrationDetails().get("scoreChanged"));
        assertEquals(Boolean.TRUE, response.getCalibrationDetails().get("recommendationChanged"));
        assertTrue(savedLog.getCalibrationDetails().contains("\"recommendationChanged\":true"));
    }

    @Test
    void shouldKeepVariantBAttributionWhenCalibrationMisses() {
        UserMessage savedProfile = new UserMessage();
        savedProfile.setHeight(BigDecimal.valueOf(165));
        savedProfile.setWeight(BigDecimal.valueOf(52));
        savedProfile.setSchoolId(12L);
        when(userMessageMapper.selectByMessageUserId(33L)).thenReturn(savedProfile);
        when(sSizesMapper.selectAllSizes()).thenReturn(List.of(
                size(1L, "160", 155, 165, 45, 55),
                size(2L, "165", 160, 170, 50, 60)
        ));
        when(experimentAssigner.assign(33L)).thenReturn(new SizeRecommendationExperimentAssignment(true, "exp-size", "B", "v1"));
        when(calibrationService.loadCalibrationSet(eq(12L), eq(88L), any()))
                .thenReturn(CalibrationBundle.empty(List.of(1L, 2L)));

        SizeRecommendationRequest request = new SizeRecommendationRequest();
        request.setUniformId(88L);
        SizeRecommendationResponse response = service.recommend(33L, request);

        ArgumentCaptor<RecommendationLog> logCaptor = ArgumentCaptor.forClass(RecommendationLog.class);
        verify(recommendationLogMapper).insert(logCaptor.capture());

        assertFalse(response.isCalibrationApplied());
        assertEquals(Boolean.FALSE, response.getCalibrationDetails().get("parameterHit"));
        assertEquals("B", logCaptor.getValue().getExperimentVariant());
    }

    @Test
    void shouldApplyCalibrationOffsetsAndResortCandidates() {
        UserMessage savedProfile = new UserMessage();
        savedProfile.setHeight(BigDecimal.valueOf(165));
        savedProfile.setWeight(BigDecimal.valueOf(52));
        savedProfile.setSchoolId(12L);
        when(userMessageMapper.selectByMessageUserId(11L)).thenReturn(savedProfile);
        when(sSizesMapper.selectAllSizes()).thenReturn(List.of(
                size(1L, "160", 155, 165, 45, 55),
                size(2L, "165", 160, 170, 50, 60)
        ));
        CalibrationBundle bundle = CalibrationBundle.empty(List.of(1L, 2L));
        bundle.getOffsetsBySizeId().put(1L, BigDecimal.valueOf(15));
        bundle.getOffsetsBySizeId().put(2L, BigDecimal.valueOf(-10));
        bundle.getProvenanceBySizeId().put(1L, "PRODUCT:88:SIZE:1");
        bundle.getProvenanceBySizeId().put(2L, "PRODUCT:88:SIZE:2");
        bundle.getDetailsBySizeId().put(1L, new LinkedHashMap<>(Map.of(
                "paramId", 101L,
                "version", 4,
                "calibrationType", "SCORE_OFFSET",
                "source", "PRODUCT:88:SIZE:1",
                "offset", BigDecimal.valueOf(15),
                "sampleSize", 42,
                "uniqueUserCount", 16,
                "confidenceLevel", "HIGH",
                "status", "ACTIVE"
        )));
        when(calibrationService.loadCalibrationSet(eq(12L), eq(88L), any())).thenReturn(bundle);
        when(calibrationService.applyScoreOffsets(anyMap(), eq(bundle), anyMap())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var baseScores = (LinkedHashMap<Long, BigDecimal>) invocation.getArgument(0);
            LinkedHashMap<Long, BigDecimal> adjusted = new LinkedHashMap<>();
            adjusted.put(1L, baseScores.get(1L).add(BigDecimal.valueOf(15)));
            adjusted.put(2L, baseScores.get(2L).subtract(BigDecimal.valueOf(10)));
            return adjusted;
        });

        SizeRecommendationRequest request = new SizeRecommendationRequest();
        request.setUniformId(88L);

        SizeRecommendationResponse response = service.recommend(11L, request);

        ArgumentCaptor<RecommendationLog> logCaptor = ArgumentCaptor.forClass(RecommendationLog.class);
        verify(recommendationLogMapper).insert(logCaptor.capture());
        RecommendationLog savedLog = logCaptor.getValue();
        @SuppressWarnings("unchecked")
        Map<Long, Map<String, Object>> detailsBySizeId =
                (Map<Long, Map<String, Object>>) response.getCalibrationDetails().get("detailsBySizeId");

        assertTrue(response.isCalibrationApplied());
        assertEquals("160", response.getRecommended().getSizeName());
        assertTrue(response.getReasons().stream().anyMatch(reason -> reason.contains("群体尺码反馈")));
        assertEquals(2L, response.getCalibrationDetails().get("baseBestSizeId"));
        assertEquals(1L, response.getCalibrationDetails().get("calibratedBestSizeId"));
        assertEquals(101L, detailsBySizeId.get(1L).get("paramId"));
        assertEquals(4, detailsBySizeId.get(1L).get("version"));
        assertEquals("SCORE_OFFSET", detailsBySizeId.get(1L).get("calibrationType"));
        assertEquals(BigDecimal.valueOf(15).setScale(4), detailsBySizeId.get(1L).get("effectiveOffset"));
        assertEquals(Boolean.TRUE, detailsBySizeId.get(1L).get("applied"));
        assertTrue(savedLog.getCalibrationDetails().contains("\"paramId\":101"));
        assertTrue(savedLog.getCalibrationDetails().contains("\"version\":4"));
    }

    @Test
    void shouldNotLetPositiveCalibrationPromoteUnsafeCandidate() {
        UserMessage savedProfile = new UserMessage();
        savedProfile.setHeight(BigDecimal.valueOf(168));
        savedProfile.setWeight(BigDecimal.valueOf(58));
        savedProfile.setSchoolId(12L);
        when(userMessageMapper.selectByMessageUserId(21L)).thenReturn(savedProfile);
        when(sSizesMapper.selectAllSizes()).thenReturn(List.of(
                size(1L, "160", 150, 160, 45, 55),
                size(2L, "165", 160, 170, 50, 60)
        ));
        CalibrationBundle bundle = CalibrationBundle.empty(List.of(1L, 2L));
        bundle.getOffsetsBySizeId().put(1L, BigDecimal.valueOf(20));
        bundle.getOffsetsBySizeId().put(2L, BigDecimal.ZERO);
        when(calibrationService.loadCalibrationSet(eq(12L), eq(88L), any())).thenReturn(bundle);
        when(calibrationService.applyScoreOffsets(anyMap(), eq(bundle), anyMap())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var baseScores = (LinkedHashMap<Long, BigDecimal>) invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            var safety = (LinkedHashMap<Long, Boolean>) invocation.getArgument(2);
            LinkedHashMap<Long, BigDecimal> adjusted = new LinkedHashMap<>();
            baseScores.forEach((sizeId, score) -> {
                BigDecimal offset = bundle.getOffsetsBySizeId().getOrDefault(sizeId, BigDecimal.ZERO);
                adjusted.put(sizeId, Boolean.TRUE.equals(safety.get(sizeId)) ? score.add(offset) : score);
            });
            return adjusted;
        });

        SizeRecommendationRequest request = new SizeRecommendationRequest();
        request.setUniformId(88L);
        SizeRecommendationResponse response = service.recommend(21L, request);

        assertFalse(response.isCalibrationApplied());
        assertEquals("165", response.getRecommended().getSizeName());
        assertEquals(1L, response.getCalibrationDetails().get("safetyGateSkipCount"));
    }

    @Test
    void shouldApplyPersonalizationAfterCalibrationAndPersistDetails() {
        UserMessage savedProfile = new UserMessage();
        savedProfile.setHeight(BigDecimal.valueOf(162));
        savedProfile.setWeight(BigDecimal.valueOf(53));
        savedProfile.setSchoolId(12L);
        when(userMessageMapper.selectByMessageUserId(19L)).thenReturn(savedProfile);
        when(sSizesMapper.selectAllSizes()).thenReturn(List.of(
                size(1L, "160", 160, 170, 50, 60),
                size(2L, "165", 160, 170, 50, 60)
        ));
        CalibrationBundle bundle = CalibrationBundle.empty(List.of(1L, 2L));
        bundle.getOffsetsBySizeId().put(1L, BigDecimal.valueOf(4));
        bundle.getOffsetsBySizeId().put(2L, BigDecimal.ZERO);
        when(calibrationService.loadCalibrationSet(eq(12L), eq(88L), any())).thenReturn(bundle);
        when(calibrationService.applyScoreOffsets(anyMap(), eq(bundle), anyMap())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var baseScores = (LinkedHashMap<Long, BigDecimal>) invocation.getArgument(0);
            LinkedHashMap<Long, BigDecimal> adjusted = new LinkedHashMap<>();
            adjusted.put(1L, baseScores.get(1L).add(BigDecimal.valueOf(4)));
            adjusted.put(2L, baseScores.get(2L));
            return adjusted;
        });
        when(sizePreferenceService.resolveCategoryKey(88L)).thenReturn("JACKET");
        SizePreferenceAdjustmentResult adjustment = new SizePreferenceAdjustmentResult();
        adjustment.setApplied(true);
        adjustment.setCategoryKey("JACKET");
        adjustment.setPreference("LOOSE");
        adjustment.setSummary("已参考个人偏宽松尺码偏好");
        adjustment.getOffsetsBySizeId().put(1L, BigDecimal.ZERO);
        adjustment.getOffsetsBySizeId().put(2L, BigDecimal.valueOf(8));
        when(sizePreferenceService.buildAdjustment(eq(19L), eq("JACKET"), any())).thenReturn(adjustment);

        SizeRecommendationRequest request = new SizeRecommendationRequest();
        request.setUniformId(88L);
        SizeRecommendationResponse response = service.recommend(19L, request);

        ArgumentCaptor<RecommendationLog> logCaptor = ArgumentCaptor.forClass(RecommendationLog.class);
        verify(recommendationLogMapper).insert(logCaptor.capture());
        RecommendationLog savedLog = logCaptor.getValue();

        assertTrue(response.isCalibrationApplied());
        assertTrue(response.isPersonalizationApplied());
        assertEquals("165", response.getRecommended().getSizeName());
        assertEquals("JACKET", response.getPersonalizationDetails().get("categoryKey"));
        assertEquals(1L, response.getPersonalizationDetails().get("baseBestSizeId"));
        assertEquals(1L, response.getPersonalizationDetails().get("calibratedBestSizeId"));
        assertEquals(2L, response.getPersonalizationDetails().get("personalizedBestSizeId"));
        assertEquals(Boolean.TRUE, savedLog.getCalibrationApplied());
        assertEquals(Boolean.TRUE, savedLog.getPersonalizationApplied());
        assertTrue(savedLog.getPersonalizationDetails().contains("\"calibratedBestSizeName\":\"160\""));
        assertTrue(savedLog.getPersonalizationDetails().contains("\"personalizedBestSizeName\":\"165\""));
        assertTrue(response.getReasons().stream().anyMatch(reason -> reason.contains("历史穿着反馈") || reason.contains("主动设置")));
    }

    private SSizes size(Long id, String name, long minHeight, long maxHeight, long minWeight, long maxWeight) {
        SSizes size = new SSizes();
        size.setId(id);
        size.setSizeName(name);
        size.setMinHeight(minHeight);
        size.setMaxHeight(maxHeight);
        size.setMinWeight(BigDecimal.valueOf(minWeight));
        size.setMaxWeight(BigDecimal.valueOf(maxWeight));
        return size;
    }

    private SSizes sizeWithChest(Long id, String name, long minHeight, long maxHeight, long minWeight, long maxWeight, long minChest, long maxChest) {
        SSizes size = size(id, name, minHeight, maxHeight, minWeight, maxWeight);
        size.setMinChest(BigDecimal.valueOf(minChest));
        size.setMaxChest(BigDecimal.valueOf(maxChest));
        return size;
    }

    private SSizes sizeWithAllOptional(Long id,
                                       String name,
                                       long minHeight,
                                       long maxHeight,
                                       long minWeight,
                                       long maxWeight,
                                       long minChest,
                                       long maxChest,
                                       long minWaist,
                                       long maxWaist,
                                       long minHip,
                                       long maxHip,
                                       long minShoulder,
                                       long maxShoulder) {
        SSizes size = sizeWithChest(id, name, minHeight, maxHeight, minWeight, maxWeight, minChest, maxChest);
        size.setMinWaist(BigDecimal.valueOf(minWaist));
        size.setMaxWaist(BigDecimal.valueOf(maxWaist));
        size.setMinHip(BigDecimal.valueOf(minHip));
        size.setMaxHip(BigDecimal.valueOf(maxHip));
        size.setMinShoulder(BigDecimal.valueOf(minShoulder));
        size.setMaxShoulder(BigDecimal.valueOf(maxShoulder));
        return size;
    }
}
