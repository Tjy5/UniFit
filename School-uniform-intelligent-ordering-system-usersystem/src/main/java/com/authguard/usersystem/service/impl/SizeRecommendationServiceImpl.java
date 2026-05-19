package com.authguard.usersystem.service.impl;

import com.authguard.usersystem.dto.ConfidenceResult;
import com.authguard.usersystem.dto.CalibrationBundle;
import com.authguard.usersystem.dto.SizeDimensionMatchDto;
import com.authguard.usersystem.dto.SizeRecommendationExperimentAssignment;
import com.authguard.usersystem.dto.SizePreferenceAdjustmentResult;
import com.authguard.usersystem.dto.RecommendationSource;
import com.authguard.usersystem.dto.SizeRecommendationCandidateDto;
import com.authguard.usersystem.dto.SizeRecommendationRequest;
import com.authguard.usersystem.dto.SizeRecommendationResponse;
import com.authguard.usersystem.entity.RecommendationLog;
import com.authguard.usersystem.entity.SSizes;
import com.authguard.usersystem.entity.UserMessage;
import com.authguard.usersystem.mapper.RecommendationLogMapper;
import com.authguard.usersystem.mapper.SSizesMapper;
import com.authguard.usersystem.mapper.UserMessageMapper;
import com.authguard.usersystem.service.CalibrationService;
import com.authguard.usersystem.service.ConfidenceCalculator;
import com.authguard.usersystem.service.SizePreferenceService;
import com.authguard.usersystem.service.SizeRecommendationExperimentAssigner;
import com.authguard.usersystem.service.SizeRecommendationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SizeRecommendationServiceImpl implements SizeRecommendationService {

    private final UserMessageMapper userMessageMapper;
    private final SSizesMapper sSizesMapper;
    private final ConfidenceCalculator confidenceCalculator;
    private final RecommendationLogMapper recommendationLogMapper;
    private final CalibrationService calibrationService;
    private final SizePreferenceService sizePreferenceService;
    private final SizeRecommendationExperimentAssigner experimentAssigner;
    private final ObjectMapper objectMapper;

    public SizeRecommendationServiceImpl(UserMessageMapper userMessageMapper,
                                         SSizesMapper sSizesMapper,
                                         ConfidenceCalculator confidenceCalculator,
                                         RecommendationLogMapper recommendationLogMapper,
                                         @Autowired(required = false) CalibrationService calibrationService,
                                         @Autowired(required = false) SizePreferenceService sizePreferenceService,
                                         @Autowired(required = false) SizeRecommendationExperimentAssigner experimentAssigner,
                                         @Autowired(required = false) ObjectMapper objectMapper) {
        this.userMessageMapper = userMessageMapper;
        this.sSizesMapper = sSizesMapper;
        this.confidenceCalculator = confidenceCalculator;
        this.recommendationLogMapper = recommendationLogMapper;
        this.calibrationService = calibrationService;
        this.sizePreferenceService = sizePreferenceService;
        this.experimentAssigner = experimentAssigner;
        this.objectMapper = objectMapper;
    }

    @Override
    public SizeRecommendationResponse recommend(Long userId, SizeRecommendationRequest request) {
        SizeRecommendationRequest safeRequest = request == null ? new SizeRecommendationRequest() : request;
        UserMessage savedProfile = userId == null ? null : userMessageMapper.selectByMessageUserId(userId);
        MeasurementProfile profile = mergeProfile(safeRequest, savedProfile);
        SizeRecommendationExperimentAssignment experimentAssignment = assignExperiment(userId);

        SizeRecommendationResponse response = new SizeRecommendationResponse();
        response.setCalibrationApplied(false);
        response.setPersonalizationApplied(false);
        String categoryKey = resolveCategoryKey(safeRequest);
        if (profile.height == null || profile.weight == null) {
            response.setAvailable(false);
            response.setMessage("请先完善身高和体重信息");
            response.setConfidenceMessage("缺少基础资料，暂时无法生成推荐");
            RecommendationLog log = logRecommendation(
                    userId,
                    safeRequest,
                    savedProfile,
                    profile,
                    response,
                    null,
                    null,
                    null,
                    CalibrationBundle.empty(List.of()),
                    experimentAssignment
            );
            response.setRecommendationLogId(log.getLogId());
            return response;
        }

        List<SSizes> allSizes = sSizesMapper.selectAllSizes();
        if (allSizes == null || allSizes.isEmpty()) {
            response.setAvailable(false);
            response.setMessage("当前暂无可用尺码数据");
            response.setConfidenceMessage("尺码基础数据为空，暂时无法推荐");
            RecommendationLog log = logRecommendation(
                    userId,
                    safeRequest,
                    savedProfile,
                    profile,
                    response,
                    null,
                    null,
                    null,
                    CalibrationBundle.empty(List.of()),
                    experimentAssignment
            );
            response.setRecommendationLogId(log.getLogId());
            return response;
        }

        List<CandidateAssessment> baseAssessments = allSizes.stream()
                .map(size -> evaluateCandidate(size, profile))
                .sorted(Comparator.comparing(CandidateAssessment::score).reversed()
                        .thenComparing(candidate -> candidate.size().getId(), Comparator.nullsLast(Long::compareTo)))
                .toList();

        CalibrationBundle calibrationBundle = CalibrationBundle.empty(
                baseAssessments.stream().map(candidate -> candidate.size().getId()).toList()
        );
        List<CandidateAssessment> assessments = baseAssessments;
        if (experimentAssignment.isVariantA()) {
            annotateExperimentBypass(calibrationBundle);
        } else if (calibrationService != null && safeRequest.getUniformId() != null) {
            calibrationBundle = calibrationService.loadCalibrationSet(
                    savedProfile == null ? null : savedProfile.getSchoolId(),
                    safeRequest.getUniformId(),
                    baseAssessments.stream().map(candidate -> candidate.size().getId()).toList()
            );
            Map<Long, BigDecimal> baseScoreMap = baseAssessments.stream().collect(Collectors.toMap(
                    candidate -> candidate.size().getId(),
                    CandidateAssessment::score,
                    (left, right) -> left,
                    LinkedHashMap::new
            ));
            Map<Long, BigDecimal> adjustedScores = calibrationService.applyScoreOffsets(
                    baseScoreMap,
                    calibrationBundle,
                    baseAssessments.stream().collect(Collectors.toMap(
                            candidate -> candidate.size().getId(),
                            CandidateAssessment::requiredDimensionsSafe,
                            (left, right) -> left,
                            LinkedHashMap::new
                    ))
            );
            annotateEffectiveCalibrationOffsets(calibrationBundle, baseScoreMap, adjustedScores);
            assessments = baseAssessments.stream()
                    .map(candidate -> withScore(candidate, adjustedScores.getOrDefault(candidate.size().getId(), candidate.score())))
                    .sorted(Comparator.comparing(CandidateAssessment::score).reversed()
                            .thenComparing(candidate -> candidate.size().getId(), Comparator.nullsLast(Long::compareTo)))
                    .toList();
            response.setCalibrationApplied(baseAssessments.stream().anyMatch(candidate ->
                    adjustedScores.getOrDefault(candidate.size().getId(), candidate.score()).compareTo(candidate.score()) != 0
            ));
            if (response.isCalibrationApplied()) {
                log.info(
                        "Size calibration applied userId={} schoolId={} uniformId={} baseBest={} calibratedBest={} offsets={}",
                        userId,
                        savedProfile == null ? null : savedProfile.getSchoolId(),
                        safeRequest.getUniformId(),
                        baseAssessments.get(0).size().getId(),
                        assessments.get(0).size().getId(),
                        calibrationBundle.immutableOffsets()
                );
            } else if (calibrationBundle.hasNonZeroAdjustment()) {
                log.info(
                        "Size calibration skipped by safety gate userId={} uniformId={} offsets={}",
                        userId,
                        safeRequest.getUniformId(),
                        calibrationBundle.immutableOffsets()
                );
            }
        }

        CandidateAssessment best = assessments.get(0);
        CandidateAssessment baseBest = baseAssessments.get(0);
        CandidateAssessment calibratedBest = best;
        SizePreferenceAdjustmentResult preferenceAdjustment = buildPreferenceAdjustment(userId, safeRequest, categoryKey, allSizes);
        if (preferenceAdjustment.isApplied()) {
            Map<Long, BigDecimal> effectiveOffsets = assessments.stream().collect(Collectors.toMap(
                    candidate -> candidate.size().getId(),
                    candidate -> candidate.requiredDimensionsSafe()
                            ? preferenceAdjustment.getOffsetsBySizeId().getOrDefault(candidate.size().getId(), BigDecimal.ZERO)
                            : BigDecimal.ZERO,
                    (left, right) -> left,
                    LinkedHashMap::new
            ));
            Map<Long, BigDecimal> adjustedScores = assessments.stream().collect(Collectors.toMap(
                    candidate -> candidate.size().getId(),
                    candidate -> clampScore(candidate.score().add(effectiveOffsets.getOrDefault(candidate.size().getId(), BigDecimal.ZERO))),
                    (left, right) -> left,
                    LinkedHashMap::new
            ));
            preferenceAdjustment.setOffsetsBySizeId(effectiveOffsets);
            List<CandidateAssessment> personalizedAssessments = assessments.stream()
                    .map(candidate -> withScore(candidate, adjustedScores.getOrDefault(candidate.size().getId(), candidate.score())))
                    .sorted(Comparator.comparing(CandidateAssessment::score).reversed()
                            .thenComparing(candidate -> candidate.size().getId(), Comparator.nullsLast(Long::compareTo)))
                    .toList();
            boolean scoreChanged = assessments.stream().anyMatch(candidate ->
                    adjustedScores.getOrDefault(candidate.size().getId(), candidate.score()).compareTo(candidate.score()) != 0
            );
            boolean orderChanged = !assessments.stream().map(candidate -> candidate.size().getId()).toList()
                    .equals(personalizedAssessments.stream().map(candidate -> candidate.size().getId()).toList());
            response.setPersonalizationApplied(scoreChanged || orderChanged);
            assessments = personalizedAssessments;
            best = assessments.get(0);
        }
        CandidateAssessment second = assessments.size() > 1 ? assessments.get(1) : null;
        BigDecimal gap = second == null
                ? best.score()
                : best.score().subtract(second.score()).max(BigDecimal.ZERO);
        long matchedDimensions = best.matches().stream().filter(DimensionAssessment::withinRange).count();
        int boundaryTouches = (int) best.matches().stream().filter(DimensionAssessment::boundary).count();
        ConfidenceResult confidence = confidenceCalculator.calculate(
                best.matches().size(),
                (int) matchedDimensions,
                best.score(),
                gap,
                best.requiredDimensionsSafe(),
                boundaryTouches
        );

        response.setAvailable(true);
        response.setRecommended(toCandidateDto(best));
        response.setAlternatives(assessments.stream().skip(1).limit(2).map(this::toCandidateDto).toList());
        response.setUsedDimensions(best.matches().stream().map(DimensionAssessment::label).toList());
        response.setReasons(best.reasons());
        response.setConfidence(confidence.getScore());
        response.setConfidenceLevel(confidence.getLevel());
        response.setConfidenceMessage(confidence.getMessage());
        response.setLowConfidence(confidence.isLowConfidence());
        response.setMessage(buildRecommendationMessage(best));
        response.setPreferenceSummary(preferenceAdjustment.getSummary());
        preferenceAdjustment.setApplied(response.isPersonalizationApplied());
        response.setCalibrationDetails(buildCalibrationDetails(
                response.isCalibrationApplied(),
                savedProfile == null ? null : savedProfile.getSchoolId(),
                safeRequest.getUniformId(),
                baseBest,
                calibratedBest,
                best,
                calibrationBundle
        ));
        response.setPersonalizationDetails(buildPersonalizationDetails(
                categoryKey,
                preferenceAdjustment,
                baseBest,
                calibratedBest,
                best
        ));

        if (profile.usedSavedProfileFields()) {
            List<String> mergedReasons = new ArrayList<>();
            mergedReasons.add("已自动补全您已保存的测量资料");
            mergedReasons.addAll(response.getReasons());
            response.setReasons(mergedReasons.stream().distinct().toList());
        }

        if (response.isCalibrationApplied()) {
            List<String> mergedReasons = new ArrayList<>();
            mergedReasons.add("已参考群体尺码反馈微调推荐顺序");
            mergedReasons.addAll(response.getReasons());
            response.setReasons(mergedReasons.stream().distinct().toList());
        }

        if (response.isPersonalizationApplied()) {
            List<String> mergedReasons = new ArrayList<>();
            String source = String.valueOf(response.getPersonalizationDetails().getOrDefault("preferenceSource", ""));
            mergedReasons.add("EXPLICIT".equals(source) ? "已参考您主动设置的穿着偏好" : "已参考历史穿着反馈微调推荐顺序");
            if (preferenceAdjustment.getSummary() != null && !preferenceAdjustment.getSummary().isBlank()) {
                mergedReasons.add(preferenceAdjustment.getSummary());
            }
            mergedReasons.addAll(response.getReasons());
            response.setReasons(mergedReasons.stream().distinct().toList());
        }

        RecommendationLog log = logRecommendation(
                userId,
                safeRequest,
                savedProfile,
                profile,
                response,
                baseBest,
                calibratedBest,
                best,
                calibrationBundle,
                experimentAssignment
        );
        response.setRecommendationLogId(log.getLogId());
        return response;
    }

    private MeasurementProfile mergeProfile(SizeRecommendationRequest request, UserMessage savedProfile) {
        boolean usedSavedFields = savedProfile != null && (
                (request.getHeight() == null && savedProfile.getHeight() != null)
                        || (request.getWeight() == null && savedProfile.getWeight() != null)
                        || (request.getChest() == null && savedProfile.getChest() != null)
                        || (request.getWaist() == null && savedProfile.getWaist() != null)
                        || (request.getHip() == null && savedProfile.getHip() != null)
                        || (request.getShoulder() == null && savedProfile.getShoulder() != null)
        );
        return new MeasurementProfile(
                firstNonNull(request.getHeight(), savedProfile == null ? null : savedProfile.getHeight()),
                firstNonNull(request.getWeight(), savedProfile == null ? null : savedProfile.getWeight()),
                firstNonNull(request.getChest(), savedProfile == null ? null : savedProfile.getChest()),
                firstNonNull(request.getWaist(), savedProfile == null ? null : savedProfile.getWaist()),
                firstNonNull(request.getHip(), savedProfile == null ? null : savedProfile.getHip()),
                firstNonNull(request.getShoulder(), savedProfile == null ? null : savedProfile.getShoulder()),
                usedSavedFields
        );
    }

    private CandidateAssessment evaluateCandidate(SSizes size, MeasurementProfile profile) {
        List<DimensionAssessment> matches = new ArrayList<>();
        addDimension(matches, "height", "身高", profile.height, toDecimal(size.getMinHeight()), toDecimal(size.getMaxHeight()), BigDecimal.valueOf(1.3), true);
        addDimension(matches, "weight", "体重", profile.weight, size.getMinWeight(), size.getMaxWeight(), BigDecimal.valueOf(1.2), true);
        addDimension(matches, "chest", "胸围", profile.chest, size.getMinChest(), size.getMaxChest(), BigDecimal.ONE, false);
        addDimension(matches, "waist", "腰围", profile.waist, size.getMinWaist(), size.getMaxWaist(), BigDecimal.valueOf(0.95), false);
        addDimension(matches, "hip", "臀围", profile.hip, size.getMinHip(), size.getMaxHip(), BigDecimal.valueOf(0.9), false);
        addDimension(matches, "shoulder", "肩宽", profile.shoulder, size.getMinShoulder(), size.getMaxShoulder(), BigDecimal.valueOf(0.85), false);

        BigDecimal totalWeight = matches.stream()
                .map(DimensionAssessment::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal weightedScore = matches.stream()
                .map(item -> item.score().multiply(item.weight()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal finalScore = totalWeight.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : weightedScore.divide(totalWeight, 2, RoundingMode.HALF_UP);
        boolean requiredDimensionsSafe = matches.stream()
                .filter(DimensionAssessment::required)
                .allMatch(DimensionAssessment::withinRange);

        List<String> reasons = new ArrayList<>();
        List<String> optionalLabels = matches.stream()
                .filter(item -> !item.required())
                .map(DimensionAssessment::label)
                .toList();
        if (optionalLabels.isEmpty()) {
            reasons.add("当前仅基于身高和体重进行推荐");
        } else {
            reasons.add("已结合" + String.join("、", optionalLabels) + "优化推荐结果");
        }
        reasons.addAll(matches.stream().map(DimensionAssessment::message).limit(3).toList());
        return new CandidateAssessment(size, finalScore, matches, reasons.stream().distinct().toList(), requiredDimensionsSafe);
    }

    private void addDimension(List<DimensionAssessment> matches,
                              String key,
                              String label,
                              BigDecimal userValue,
                              BigDecimal minValue,
                              BigDecimal maxValue,
                              BigDecimal weight,
                              boolean required) {
        if (userValue == null || (minValue == null && maxValue == null)) {
            return;
        }
        BigDecimal score;
        boolean withinRange = isWithinRange(userValue, minValue, maxValue);
        boolean boundary;
        String message;
        if (withinRange) {
            BigDecimal range = range(minValue, maxValue);
            BigDecimal marginRatio = marginRatio(userValue, minValue, maxValue, range);
            score = BigDecimal.valueOf(70).add(marginRatio.multiply(BigDecimal.valueOf(30))).setScale(2, RoundingMode.HALF_UP);
            boundary = marginRatio.compareTo(BigDecimal.valueOf(0.2)) < 0;
            message = boundary
                    ? label + "位于建议区间边缘，建议关注松紧度"
                    : label + "位于建议区间内";
        } else {
            BigDecimal rangeBase = fallbackRange(range(minValue, maxValue), userValue);
            BigDecimal distance = distanceToRange(userValue, minValue, maxValue);
            BigDecimal normalized = rangeBase.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ONE
                    : distance.divide(rangeBase, 4, RoundingMode.HALF_UP);
            score = BigDecimal.valueOf(Math.max(0D, 60D - normalized.multiply(BigDecimal.valueOf(60)).doubleValue()))
                    .setScale(2, RoundingMode.HALF_UP);
            boundary = true;
            message = buildOutOfRangeMessage(label, userValue, minValue, maxValue);
        }
        matches.add(new DimensionAssessment(key, label, userValue, minValue, maxValue, score, weight, withinRange, boundary, message, required));
    }

    private SizeRecommendationCandidateDto toCandidateDto(CandidateAssessment assessment) {
        SizeRecommendationCandidateDto dto = new SizeRecommendationCandidateDto();
        dto.setSizeId(assessment.size().getId());
        dto.setSizeName(assessment.size().getSizeName());
        dto.setScore(assessment.score());
        dto.setReasons(assessment.reasons());
        dto.setDimensionMatches(assessment.matches().stream().map(this::toDimensionDto).toList());
        return dto;
    }

    private SizeDimensionMatchDto toDimensionDto(DimensionAssessment assessment) {
        SizeDimensionMatchDto dto = new SizeDimensionMatchDto();
        dto.setDimensionKey(assessment.key());
        dto.setLabel(assessment.label());
        dto.setUserValue(assessment.userValue());
        dto.setMinValue(assessment.minValue());
        dto.setMaxValue(assessment.maxValue());
        dto.setMatchPercent(assessment.score().setScale(0, RoundingMode.HALF_UP).intValue());
        dto.setWithinRange(assessment.withinRange());
        dto.setMessage(assessment.message());
        return dto;
    }

    private String buildRecommendationMessage(CandidateAssessment best) {
        String dimensions = best.matches().stream()
                .map(DimensionAssessment::label)
                .collect(Collectors.joining("、"));
        return "已基于" + dimensions + "为您推荐 " + best.size().getSizeName() + " 码";
    }

    private RecommendationLog logRecommendation(Long userId,
                                   SizeRecommendationRequest request,
                                   UserMessage savedProfile,
                                   MeasurementProfile profile,
                                   SizeRecommendationResponse response,
                                   CandidateAssessment baseBest,
                                   CandidateAssessment calibratedBest,
                                   CandidateAssessment finalBest,
                                   CalibrationBundle calibrationBundle,
                                   SizeRecommendationExperimentAssignment experimentAssignment) {
        RecommendationLog log = new RecommendationLog();
        Long schoolId = savedProfile == null ? null : savedProfile.getSchoolId();
        log.setUserId(userId);
        log.setSchoolId(schoolId);
        log.setUniformId(request == null ? null : request.getUniformId());
        log.setCategoryKey(response.getPersonalizationDetails() == null
                ? resolveCategoryKey(request)
                : String.valueOf(response.getPersonalizationDetails().getOrDefault("categoryKey", resolveCategoryKey(request))));
        log.setRecommendedSizeId(response.getRecommended() == null ? null : response.getRecommended().getSizeId());
        log.setRecommendedSizeName(response.getRecommended() == null ? null : response.getRecommended().getSizeName());
        log.setOrderItemId(null);
        if (experimentAssignment != null && experimentAssignment.enabled()) {
            log.setExperimentKey(experimentAssignment.experimentKey());
            log.setExperimentVariant(experimentAssignment.variant());
            log.setStrategyVersion(experimentAssignment.strategyVersion());
        }
        String rawSource = request == null ? null : request.getSource();
        String normalizedSource = RecommendationSource.normalizeOrUnknown(rawSource);
        if (rawSource != null && !rawSource.isBlank() && !normalizedSource.equals(rawSource.trim())) {
            SizeRecommendationServiceImpl.log.info("Normalized recommendation request source from '{}' to '{}'", rawSource, normalizedSource);
        }
        log.setRequestSource(normalizedSource);
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("schoolId", schoolId);
        input.put("uniformId", request == null ? null : request.getUniformId());
        input.put("experimentKey", log.getExperimentKey());
        input.put("experimentVariant", log.getExperimentVariant());
        input.put("strategyVersion", log.getStrategyVersion());
        input.put("source", log.getRequestSource());
        input.put("height", profile.height);
        input.put("weight", profile.weight);
        input.put("chest", profile.chest);
        input.put("waist", profile.waist);
        input.put("hip", profile.hip);
        input.put("shoulder", profile.shoulder);
        log.setInputSummary(writeJson(input));
        log.setResultSummary(writeJson(response));
        log.setConfidenceScore(response.getConfidence());
        log.setConfidenceLevel(response.getConfidenceLevel());
        log.setCalibrationApplied(response.isCalibrationApplied());
        log.setPersonalizationApplied(response.isPersonalizationApplied());
        log.setCalibrationDetails(writeJson(response.getCalibrationDetails()));
        log.setPersonalizationDetails(writeJson(response.getPersonalizationDetails()));
        log.setCreateBy(userId == null ? "system" : String.valueOf(userId));
        log.setCreateTime(new Date());
        recommendationLogMapper.insert(log);
        return log;
    }

    private SizeRecommendationExperimentAssignment assignExperiment(Long userId) {
        return experimentAssigner == null
                ? SizeRecommendationExperimentAssignment.disabled()
                : experimentAssigner.assign(userId);
    }

    private String writeJson(Object value) {
        if (objectMapper == null) {
            return String.valueOf(value);
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private boolean isWithinRange(BigDecimal userValue, BigDecimal minValue, BigDecimal maxValue) {
        boolean meetsMin = minValue == null || userValue.compareTo(minValue) >= 0;
        boolean meetsMax = maxValue == null || userValue.compareTo(maxValue) <= 0;
        return meetsMin && meetsMax;
    }

    private BigDecimal distanceToRange(BigDecimal userValue, BigDecimal minValue, BigDecimal maxValue) {
        if (minValue != null && userValue.compareTo(minValue) < 0) {
            return minValue.subtract(userValue);
        }
        if (maxValue != null && userValue.compareTo(maxValue) > 0) {
            return userValue.subtract(maxValue);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal range(BigDecimal minValue, BigDecimal maxValue) {
        if (minValue == null || maxValue == null) {
            return BigDecimal.ZERO;
        }
        return maxValue.subtract(minValue).max(BigDecimal.ZERO);
    }

    private BigDecimal fallbackRange(BigDecimal range, BigDecimal userValue) {
        if (range != null && range.compareTo(BigDecimal.ZERO) > 0) {
            return range;
        }
        return userValue.multiply(BigDecimal.valueOf(0.1)).max(BigDecimal.TEN);
    }

    private BigDecimal marginRatio(BigDecimal userValue, BigDecimal minValue, BigDecimal maxValue, BigDecimal range) {
        if (range == null || range.compareTo(BigDecimal.ZERO) <= 0 || minValue == null || maxValue == null) {
            return BigDecimal.ONE;
        }
        BigDecimal lowerGap = userValue.subtract(minValue);
        BigDecimal upperGap = maxValue.subtract(userValue);
        BigDecimal nearest = lowerGap.min(upperGap).max(BigDecimal.ZERO);
        BigDecimal halfRange = range.divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        if (halfRange.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return nearest.divide(halfRange, 4, RoundingMode.HALF_UP).min(BigDecimal.ONE).max(BigDecimal.ZERO);
    }

    private String buildOutOfRangeMessage(String label, BigDecimal userValue, BigDecimal minValue, BigDecimal maxValue) {
        if (minValue != null && userValue.compareTo(minValue) < 0) {
            return label + "略低于建议下限 " + strip(minValue);
        }
        if (maxValue != null && userValue.compareTo(maxValue) > 0) {
            return label + "略高于建议上限 " + strip(maxValue);
        }
        return label + "未命中建议区间";
    }

    private String strip(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private BigDecimal toDecimal(Long value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private String resolveCategoryKey(SizeRecommendationRequest request) {
        if (sizePreferenceService == null || request == null || request.getUniformId() == null) {
            return SizePreferenceService.GENERAL_CATEGORY;
        }
        return sizePreferenceService.resolveCategoryKey(request.getUniformId());
    }

    private SizePreferenceAdjustmentResult buildPreferenceAdjustment(Long userId,
                                                                    SizeRecommendationRequest request,
                                                                    String categoryKey,
                                                                    List<SSizes> allSizes) {
        if (sizePreferenceService == null || userId == null || request == null || request.getUniformId() == null) {
            SizePreferenceAdjustmentResult result = new SizePreferenceAdjustmentResult();
            result.setCategoryKey(categoryKey);
            return result;
        }
        return sizePreferenceService.buildAdjustment(userId, categoryKey, allSizes);
    }

    private Map<String, Object> buildPersonalizationDetails(String categoryKey,
                                                           SizePreferenceAdjustmentResult adjustment,
                                                           CandidateAssessment baseBest,
                                                           CandidateAssessment calibratedBest,
                                                           CandidateAssessment finalBest) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("categoryKey", categoryKey);
        details.put("applied", adjustment != null && adjustment.isApplied());
        details.put("preference", adjustment == null ? null : adjustment.getPreference());
        details.put("confidenceLevel", adjustment == null ? null : adjustment.getConfidenceLevel());
        details.put("confidenceScore", adjustment == null ? null : adjustment.getConfidenceScore());
        details.put("sampleCount", adjustment == null ? null : adjustment.getSampleCount());
        details.put("preferenceSource", adjustment == null ? null : adjustment.getPreferenceSource());
        details.put("issuePartSummary", adjustment == null ? null : adjustment.getIssuePartSummary());
        details.put("issuePartCounts", adjustment == null ? Map.of() : adjustment.getIssuePartCounts());
        details.put("topIssueParts", adjustment == null ? List.of() : adjustment.getTopIssueParts());
        details.put("baseBestSizeId", baseBest == null ? null : baseBest.size().getId());
        details.put("baseBestSizeName", baseBest == null ? null : baseBest.size().getSizeName());
        details.put("calibratedBestSizeId", calibratedBest == null ? null : calibratedBest.size().getId());
        details.put("calibratedBestSizeName", calibratedBest == null ? null : calibratedBest.size().getSizeName());
        details.put("personalizedBestSizeId", finalBest == null ? null : finalBest.size().getId());
        details.put("personalizedBestSizeName", finalBest == null ? null : finalBest.size().getSizeName());
        details.put("offsetsBySizeId", adjustment == null ? Map.of() : adjustment.getOffsetsBySizeId());
        return details;
    }

    private Map<String, Object> buildCalibrationDetails(boolean applied,
                                                        Long schoolId,
                                                        Long uniformId,
                                                        CandidateAssessment baseBest,
                                                        CandidateAssessment calibratedBest,
                                                        CandidateAssessment finalBest,
                                                        CalibrationBundle calibrationBundle) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("applied", applied);
        details.put("schoolId", schoolId);
        details.put("uniformId", uniformId);
        details.put("baseBestSizeId", baseBest == null ? null : baseBest.size().getId());
        details.put("baseBestSizeName", baseBest == null ? null : baseBest.size().getSizeName());
        details.put("baseBestScore", baseBest == null ? null : baseBest.score());
        details.put("calibratedBestSizeId", calibratedBest == null ? null : calibratedBest.size().getId());
        details.put("calibratedBestSizeName", calibratedBest == null ? null : calibratedBest.size().getSizeName());
        details.put("calibratedBestScore", calibratedBest == null ? null : calibratedBest.score());
        details.put("finalBestSizeId", finalBest == null ? null : finalBest.size().getId());
        details.put("finalBestSizeName", finalBest == null ? null : finalBest.size().getSizeName());
        details.put("finalBestScore", finalBest == null ? null : finalBest.score());
        details.put("parameterHit", calibrationBundle != null && calibrationBundle.hasParameterHit());
        details.put("scoreChanged", applied);
        details.put("recommendationChanged", baseBest != null
                && calibratedBest != null
                && baseBest.size() != null
                && calibratedBest.size() != null
                && !java.util.Objects.equals(baseBest.size().getId(), calibratedBest.size().getId()));
        details.put("safetyGateSkipCount", calibrationBundle == null ? 0L : calibrationBundle.safetyGateSkipCount());
        details.put("offsetsBySizeId", calibrationBundle == null ? Map.of() : calibrationBundle.immutableOffsets());
        details.put("provenanceBySizeId", calibrationBundle == null ? Map.of() : calibrationBundle.getProvenanceBySizeId());
        details.put("detailsBySizeId", calibrationBundle == null ? Map.of() : calibrationBundle.getDetailsBySizeId());
        return details;
    }

    private void annotateExperimentBypass(CalibrationBundle calibrationBundle) {
        if (calibrationBundle == null || calibrationBundle.getDetailsBySizeId() == null) {
            return;
        }
        calibrationBundle.getDetailsBySizeId().replaceAll((sizeId, detail) -> {
            Map<String, Object> mutableDetail = detail == null || detail.isEmpty()
                    ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(detail);
            mutableDetail.put("experimentBypassed", true);
            mutableDetail.put("applied", false);
            mutableDetail.put("effectiveOffset", BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            return mutableDetail;
        });
    }

    private void annotateEffectiveCalibrationOffsets(CalibrationBundle calibrationBundle,
                                                     Map<Long, BigDecimal> baseScores,
                                                     Map<Long, BigDecimal> adjustedScores) {
        if (calibrationBundle == null || calibrationBundle.getDetailsBySizeId() == null) {
            return;
        }
        calibrationBundle.getDetailsBySizeId().forEach((sizeId, detail) -> {
            BigDecimal configuredOffset = calibrationBundle.getOffsetsBySizeId() == null
                    ? BigDecimal.ZERO
                    : calibrationBundle.getOffsetsBySizeId().getOrDefault(sizeId, BigDecimal.ZERO);
            if ((detail == null || detail.isEmpty()) && configuredOffset.compareTo(BigDecimal.ZERO) == 0) {
                return;
            }
            Map<String, Object> mutableDetail = detail == null ? new LinkedHashMap<>() : new LinkedHashMap<>(detail);
            BigDecimal before = baseScores == null ? null : baseScores.get(sizeId);
            BigDecimal after = adjustedScores == null ? null : adjustedScores.get(sizeId);
            BigDecimal effectiveOffset = before == null || after == null
                    ? BigDecimal.ZERO
                    : after.subtract(before).setScale(4, RoundingMode.HALF_UP);
            mutableDetail.put("scoreBefore", before);
            mutableDetail.put("scoreAfter", after);
            mutableDetail.put("effectiveOffset", effectiveOffset);
            mutableDetail.put("applied", effectiveOffset.compareTo(BigDecimal.ZERO) != 0);
            mutableDetail.put("parameterHit", configuredOffset != null && configuredOffset.compareTo(BigDecimal.ZERO) != 0);
            mutableDetail.put("safetyGateSkipped", configuredOffset != null
                    && configuredOffset.compareTo(BigDecimal.ZERO) > 0
                    && effectiveOffset.compareTo(BigDecimal.ZERO) == 0);
            calibrationBundle.getDetailsBySizeId().put(sizeId, mutableDetail);
        });
    }

    private BigDecimal clampScore(BigDecimal score) {
        if (score == null) {
            return BigDecimal.ZERO;
        }
        return score.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private CandidateAssessment withScore(CandidateAssessment candidate, BigDecimal newScore) {
        return new CandidateAssessment(
                candidate.size(),
                newScore == null ? candidate.score() : newScore,
                candidate.matches(),
                candidate.reasons(),
                candidate.requiredDimensionsSafe()
        );
    }

    private record MeasurementProfile(
            BigDecimal height,
            BigDecimal weight,
            BigDecimal chest,
            BigDecimal waist,
            BigDecimal hip,
            BigDecimal shoulder,
            boolean usedSavedProfileFields
    ) {
    }

    private record DimensionAssessment(
            String key,
            String label,
            BigDecimal userValue,
            BigDecimal minValue,
            BigDecimal maxValue,
            BigDecimal score,
            BigDecimal weight,
            boolean withinRange,
            boolean boundary,
            String message,
            boolean required
    ) {
    }

    private record CandidateAssessment(
            SSizes size,
            BigDecimal score,
            List<DimensionAssessment> matches,
            List<String> reasons,
            boolean requiredDimensionsSafe
    ) {
    }
}
