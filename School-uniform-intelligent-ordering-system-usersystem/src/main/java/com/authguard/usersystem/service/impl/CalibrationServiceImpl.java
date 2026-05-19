package com.authguard.usersystem.service.impl;

import com.authguard.usersystem.config.CalibrationProperties;
import com.authguard.usersystem.dto.CalibrationBundle;
import com.authguard.usersystem.dto.CalibrationFeedbackRecord;
import com.authguard.usersystem.dto.FeedbackAggregation;
import com.authguard.usersystem.entity.CalibrationAudit;
import com.authguard.usersystem.entity.CalibrationParams;
import com.authguard.usersystem.entity.SSizes;
import com.authguard.usersystem.mapper.CalibrationAuditMapper;
import com.authguard.usersystem.mapper.CalibrationParamsMapper;
import com.authguard.usersystem.mapper.SSizesMapper;
import com.authguard.usersystem.mapper.SizeFeedbackMapper;
import com.authguard.usersystem.service.CalibrationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CalibrationServiceImpl implements CalibrationService {

    private static final String GLOBAL_SCOPE = "GLOBAL";
    private static final String SCHOOL_SCOPE = "SCHOOL";
    private static final String PRODUCT_SCOPE = "PRODUCT";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    private static final BigDecimal DIRECTION_CONFIDENCE_MARGIN = BigDecimal.valueOf(0.20);
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    private final CalibrationParamsMapper calibrationParamsMapper;
    private final CalibrationAuditMapper calibrationAuditMapper;
    private final SizeFeedbackMapper sizeFeedbackMapper;
    private final SSizesMapper sSizesMapper;
    private final CalibrationProperties calibrationProperties;
    private final ObjectMapper objectMapper;
    private final Map<String, CacheEntry> bundleCache = new ConcurrentHashMap<>();

    @Autowired
    public CalibrationServiceImpl(CalibrationParamsMapper calibrationParamsMapper,
                                  CalibrationAuditMapper calibrationAuditMapper,
                                  SizeFeedbackMapper sizeFeedbackMapper,
                                  @Autowired(required = false) SSizesMapper sSizesMapper,
                                  CalibrationProperties calibrationProperties,
                                  @Autowired(required = false) ObjectMapper objectMapper) {
        this.calibrationParamsMapper = calibrationParamsMapper;
        this.calibrationAuditMapper = calibrationAuditMapper;
        this.sizeFeedbackMapper = sizeFeedbackMapper;
        this.sSizesMapper = sSizesMapper;
        this.calibrationProperties = calibrationProperties;
        this.objectMapper = objectMapper;
    }

    public CalibrationServiceImpl(CalibrationParamsMapper calibrationParamsMapper,
                                  CalibrationAuditMapper calibrationAuditMapper,
                                  SizeFeedbackMapper sizeFeedbackMapper,
                                  CalibrationProperties calibrationProperties,
                                  ObjectMapper objectMapper) {
        this(calibrationParamsMapper, calibrationAuditMapper, sizeFeedbackMapper, null, calibrationProperties, objectMapper);
    }

    @Override
    public CalibrationBundle loadCalibrationSet(Long schoolId, Long uniformId, Collection<Long> candidateSizeIds) {
        List<Long> candidateIds = candidateSizeIds == null
                ? List.of()
                : candidateSizeIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        CalibrationBundle emptyBundle = CalibrationBundle.empty(candidateIds);
        if (!calibrationProperties.isEnabled() || uniformId == null || candidateIds.isEmpty()) {
            return emptyBundle;
        }

        String cacheKey = buildCacheKey(schoolId, uniformId, candidateIds);
        Long latestAuditRevision = latestAuditRevision();
        CacheEntry cacheEntry = bundleCache.get(cacheKey);
        if (cacheEntry != null
                && cacheEntry.expiresAt().isAfter(Instant.now())
                && Objects.equals(cacheEntry.auditRevision(), latestAuditRevision)) {
            return cloneBundle(cacheEntry.bundle());
        }

        Date now = new Date();
        CalibrationBundle bundle = CalibrationBundle.empty(candidateIds);
        for (Long candidateSizeId : candidateIds) {
            CalibrationParams matchedParam = resolveParam(now, schoolId, uniformId, candidateSizeId);
            if (matchedParam != null) {
                bundle.getOffsetsBySizeId().put(candidateSizeId, defaultOffset(matchedParam.getAdjustmentValue()));
                bundle.getProvenanceBySizeId().put(candidateSizeId, toProvenance(matchedParam));
                bundle.getDetailsBySizeId().put(candidateSizeId, toDetails(matchedParam));
            }
        }

        bundleCache.put(
                cacheKey,
                new CacheEntry(
                        cloneBundle(bundle),
                        Instant.now().plusSeconds(Math.max(1, calibrationProperties.getCacheTtlMinutes()) * 60L),
                        latestAuditRevision
                )
        );
        return bundle;
    }

    @Override
    public Map<Long, BigDecimal> applyScoreOffsets(Map<Long, BigDecimal> baseScores, CalibrationBundle bundle) {
        return applyScoreOffsets(baseScores, bundle, Map.of());
    }

    @Override
    public Map<Long, BigDecimal> applyScoreOffsets(Map<Long, BigDecimal> baseScores,
                                                   CalibrationBundle bundle,
                                                   Map<Long, Boolean> requiredDimensionsSafeBySizeId) {
        Map<Long, BigDecimal> adjustedScores = new LinkedHashMap<>();
        if (baseScores == null || baseScores.isEmpty()) {
            return adjustedScores;
        }

        for (Map.Entry<Long, BigDecimal> entry : baseScores.entrySet()) {
            BigDecimal baseScore = entry.getValue() == null ? BigDecimal.ZERO : entry.getValue();
            BigDecimal offset = bundle == null
                    ? BigDecimal.ZERO
                    : bundle.getOffsetsBySizeId().getOrDefault(entry.getKey(), BigDecimal.ZERO);
            boolean safe = requiredDimensionsSafeBySizeId == null
                    || !requiredDimensionsSafeBySizeId.containsKey(entry.getKey())
                    || Boolean.TRUE.equals(requiredDimensionsSafeBySizeId.get(entry.getKey()));
            BigDecimal effectiveOffset = offset.compareTo(BigDecimal.ZERO) > 0 && !safe
                    ? BigDecimal.ZERO
                    : offset;
            adjustedScores.put(entry.getKey(), clampScore(baseScore.add(effectiveOffset)));
        }
        return adjustedScores;
    }

    @Override
    public List<FeedbackAggregation> aggregateFeedback(LocalDate startDate, LocalDate endDate) {
        Date startTime = Date.from(startDate.atStartOfDay(DEFAULT_ZONE).toInstant());
        Date endTime = Date.from(LocalDateTime.of(endDate, java.time.LocalTime.MAX).atZone(DEFAULT_ZONE).toInstant());
        List<CalibrationFeedbackRecord> records = sizeFeedbackMapper.selectCalibrationFeedbackWindow(startTime, endTime);
        Map<String, AggregationAccumulator> accumulators = new LinkedHashMap<>();

        for (CalibrationFeedbackRecord record : records) {
            BigDecimal weight = calculateWeight(record.getFeedbackTime(), endDate);
            addAggregation(accumulators, GLOBAL_SCOPE, null, null, record, weight);
            addAggregation(accumulators, GLOBAL_SCOPE, null, record.getRecommendedSizeId(), record, weight);

            if (record.getSchoolId() != null) {
                String schoolScopeId = String.valueOf(record.getSchoolId());
                addAggregation(accumulators, SCHOOL_SCOPE, schoolScopeId, null, record, weight);
                addAggregation(accumulators, SCHOOL_SCOPE, schoolScopeId, record.getRecommendedSizeId(), record, weight);
            }

            if (record.getUniformId() != null) {
                String productScopeId = String.valueOf(record.getUniformId());
                addAggregation(accumulators, PRODUCT_SCOPE, productScopeId, null, record, weight);
                addAggregation(accumulators, PRODUCT_SCOPE, productScopeId, record.getRecommendedSizeId(), record, weight);
            }
        }

        return accumulators.values().stream()
                .map(AggregationAccumulator::toDto)
                .sorted(Comparator
                        .comparing(FeedbackAggregation::getScopeType)
                        .thenComparing(aggregation -> aggregation.getScopeId() == null ? "" : aggregation.getScopeId())
                        .thenComparing(aggregation -> aggregation.getTargetSizeId() == null ? -1L : aggregation.getTargetSizeId()))
                .toList();
    }

    @Override
    public List<CalibrationParams> generateCalibrationParams(List<FeedbackAggregation> aggregations) {
        Date now = new Date();
        List<CalibrationParams> params = new ArrayList<>();
        if (aggregations == null) {
            return params;
        }

        Map<Long, SSizes> sizeById = loadSizesById(aggregations);
        for (FeedbackAggregation aggregation : aggregations) {
            boolean observing = aggregation.getSampleSize() < calibrationProperties.getThresholds().getMinFeedbackCount()
                    || aggregation.getUniqueUserCount() < calibrationProperties.getThresholds().getMinUniqueUsers();

            Direction direction = resolveDirection(aggregation);
            params.add(buildParam(aggregation, aggregation.getTargetSizeId(), calculateCurrentSizeAdjustment(aggregation), observing, now, currentDirectionLabel(aggregation, direction)));

            Long adjacentSizeId = resolveAdjacentSizeId(aggregation.getTargetSizeId(), direction, sizeById);
            if (!observing && adjacentSizeId != null && direction != Direction.NONE) {
                params.add(buildParam(aggregation, adjacentSizeId, calculateAdjacentAdjustment(aggregation), false, now, direction.name()));
            }
        }
        return params;
    }

    @Override
    public void saveGeneratedParams(List<CalibrationParams> params, String operator, String reason) {
        if (params == null || params.isEmpty()) {
            return;
        }

        for (CalibrationParams param : params) {
            Date now = new Date();
            if (param.getVersion() == null || param.getVersion() <= 0) {
                param.setVersion(resolveNextVersion(param.getScopeType(), param.getScopeId(), param.getTargetSizeId()));
            }
            if (param.getEffectiveFrom() == null) {
                param.setEffectiveFrom(now);
            }
            param.setCreateBy(operator);
            param.setCreateTime(now);
            param.setUpdateBy(operator);
            param.setUpdateTime(now);
            calibrationParamsMapper.insert(param);

            CalibrationAudit audit = new CalibrationAudit();
            audit.setParamId(param.getParamId());
            audit.setAction("OBSERVING".equals(param.getStatus()) ? "OBSERVING" : "CREATE");
            audit.setOldValue(null);
            audit.setNewValue(writeJson(param));
            audit.setReason(reason);
            audit.setOperator(operator);
            audit.setCreateTime(now);
            calibrationAuditMapper.insert(audit);
        }

        clearCache();
    }

    @Override
    public void clearCache() {
        bundleCache.clear();
    }

    private CalibrationParams resolveParam(Date now, Long schoolId, Long uniformId, Long candidateSizeId) {
        List<CalibrationParams> candidates = new ArrayList<>();
        candidates.add(firstActive(PRODUCT_SCOPE, uniformId == null ? null : String.valueOf(uniformId), candidateSizeId, now));
        candidates.add(firstActive(PRODUCT_SCOPE, uniformId == null ? null : String.valueOf(uniformId), null, now));
        candidates.add(firstActive(SCHOOL_SCOPE, schoolId == null ? null : String.valueOf(schoolId), candidateSizeId, now));
        candidates.add(firstActive(SCHOOL_SCOPE, schoolId == null ? null : String.valueOf(schoolId), null, now));
        candidates.add(firstActive(GLOBAL_SCOPE, null, candidateSizeId, now));
        candidates.add(firstActive(GLOBAL_SCOPE, null, null, now));

        return candidates.stream().filter(Objects::nonNull).findFirst().orElse(null);
    }

    private CalibrationParams firstActive(String scopeType, String scopeId, Long targetSizeId, Date now) {
        List<CalibrationParams> params = calibrationParamsMapper.selectActiveByScope(scopeType, scopeId, targetSizeId, now);
        return params == null || params.isEmpty() ? null : params.get(0);
    }

    private String toProvenance(CalibrationParams param) {
        if (param == null) {
            return "NONE";
        }
        String scopeId = param.getScopeId();
        Long targetSizeId = param.getTargetSizeId();
        if (PRODUCT_SCOPE.equals(param.getScopeType())) {
            return targetSizeId == null ? "PRODUCT:" + scopeId : "PRODUCT:" + scopeId + ":SIZE:" + targetSizeId;
        }
        if (SCHOOL_SCOPE.equals(param.getScopeType())) {
            return targetSizeId == null ? "SCHOOL:" + scopeId : "SCHOOL:" + scopeId + ":SIZE:" + targetSizeId;
        }
        return targetSizeId == null ? "GLOBAL" : "GLOBAL:SIZE:" + targetSizeId;
    }

    private Map<String, Object> toDetails(CalibrationParams param) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (param == null) {
            return details;
        }
        details.put("paramId", param.getParamId());
        details.put("version", param.getVersion());
        details.put("calibrationType", param.getCalibrationType());
        details.put("source", toProvenance(param));
        details.put("offset", param.getAdjustmentValue());
        details.put("sampleSize", param.getSampleSize());
        details.put("uniqueUserCount", param.getUniqueUserCount());
        details.put("confidenceLevel", param.getConfidenceLevel());
        details.put("feedbackDistribution", param.getFeedbackDistribution());
        details.put("status", param.getStatus());
        return details;
    }

    private BigDecimal clampScore(BigDecimal score) {
        return score.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultOffset(BigDecimal value) {
        return value == null ? ZERO : value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateWeight(Date feedbackTime, LocalDate endDate) {
        if (feedbackTime == null) {
            return BigDecimal.ONE;
        }
        LocalDate feedbackDate = Instant.ofEpochMilli(feedbackTime.getTime()).atZone(DEFAULT_ZONE).toLocalDate();
        long daysAgo = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(feedbackDate, endDate));
        double weight = Math.exp(-1D * daysAgo / Math.max(1, calibrationProperties.getDecayDays()));
        return BigDecimal.valueOf(weight).setScale(6, RoundingMode.HALF_UP);
    }

    private void addAggregation(Map<String, AggregationAccumulator> accumulators,
                                String scopeType,
                                String scopeId,
                                Long targetSizeId,
                                CalibrationFeedbackRecord record,
                                BigDecimal weight) {
        if (targetSizeId == null && (PRODUCT_SCOPE.equals(scopeType) || SCHOOL_SCOPE.equals(scopeType) || GLOBAL_SCOPE.equals(scopeType))) {
            // scope-wide aggregation is always allowed
        }
        String key = scopeType + "|" + (scopeId == null ? "GLOBAL" : scopeId) + "|" + (targetSizeId == null ? "ALL" : targetSizeId);
        AggregationAccumulator accumulator = accumulators.computeIfAbsent(key, ignored -> new AggregationAccumulator(scopeType, scopeId, targetSizeId));
        accumulator.sampleSize++;
        accumulator.uniqueUsers.add(record.getUserId());
        accumulator.totalWeightedCount = accumulator.totalWeightedCount.add(weight);

        String satisfaction = record.getSatisfaction();
        if ("FIT".equalsIgnoreCase(satisfaction)) {
            accumulator.weightedFitCount = accumulator.weightedFitCount.add(weight);
        } else if ("TOO_LARGE".equalsIgnoreCase(satisfaction)) {
            accumulator.weightedTooLargeCount = accumulator.weightedTooLargeCount.add(weight);
        } else if ("TOO_SMALL".equalsIgnoreCase(satisfaction)) {
            accumulator.weightedTooSmallCount = accumulator.weightedTooSmallCount.add(weight);
        }
    }

    private BigDecimal calculateCurrentSizeAdjustment(FeedbackAggregation aggregation) {
        Direction direction = resolveDirection(aggregation);
        if (direction == Direction.TOO_SMALL || direction == Direction.TOO_LARGE) {
            BigDecimal directionRatio = direction == Direction.TOO_SMALL
                    ? ratio(aggregation.getWeightedTooSmallCount(), aggregation.getTotalWeightedCount())
                    : ratio(aggregation.getWeightedTooLargeCount(), aggregation.getTotalWeightedCount());
            BigDecimal severity = directionRatio
                    .subtract(BigDecimal.valueOf(0.5))
                    .max(BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(0.5), 4, RoundingMode.HALF_UP);
            return BigDecimal.valueOf(-5).subtract(severity.multiply(BigDecimal.TEN))
                    .max(BigDecimal.valueOf(-15))
                    .setScale(4, RoundingMode.HALF_UP);
        }
        return calculateFitRatioAdjustment(aggregation.getWeightedFitRatio());
    }

    private BigDecimal calculateAdjacentAdjustment(FeedbackAggregation aggregation) {
        BigDecimal directionRatio = resolveDirection(aggregation) == Direction.TOO_SMALL
                ? ratio(aggregation.getWeightedTooSmallCount(), aggregation.getTotalWeightedCount())
                : ratio(aggregation.getWeightedTooLargeCount(), aggregation.getTotalWeightedCount());
        BigDecimal severity = directionRatio
                .subtract(BigDecimal.valueOf(0.5))
                .max(BigDecimal.ZERO)
                .divide(BigDecimal.valueOf(0.5), 4, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(2).add(severity.multiply(BigDecimal.valueOf(3)))
                .min(BigDecimal.valueOf(5))
                .max(BigDecimal.ZERO)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFitRatioAdjustment(BigDecimal fitRatio) {
        if (fitRatio == null) {
            return ZERO;
        }
        if (fitRatio.compareTo(BigDecimal.valueOf(0.5)) < 0) {
            BigDecimal severity = BigDecimal.valueOf(0.5).subtract(fitRatio)
                    .divide(BigDecimal.valueOf(0.5), 4, RoundingMode.HALF_UP);
            return BigDecimal.valueOf(-5).subtract(severity.multiply(BigDecimal.TEN))
                    .max(BigDecimal.valueOf(-15))
                    .setScale(4, RoundingMode.HALF_UP);
        }
        if (fitRatio.compareTo(BigDecimal.valueOf(0.7)) >= 0) {
            BigDecimal severity = fitRatio.subtract(BigDecimal.valueOf(0.7))
                    .divide(BigDecimal.valueOf(0.3), 4, RoundingMode.HALF_UP);
            return BigDecimal.valueOf(2).add(severity.multiply(BigDecimal.valueOf(3)))
                    .min(BigDecimal.valueOf(5))
                    .setScale(4, RoundingMode.HALF_UP);
        }
        return ZERO;
    }

    private CalibrationParams buildParam(FeedbackAggregation aggregation,
                                         Long targetSizeId,
                                         BigDecimal adjustment,
                                         boolean observing,
                                         Date now,
                                         String direction) {
        CalibrationParams param = new CalibrationParams();
        param.setScopeType(aggregation.getScopeType());
        param.setScopeId(aggregation.getScopeId());
        param.setTargetSizeId(targetSizeId);
        param.setCalibrationType("SCORE_OFFSET");
        param.setAdjustmentValue(adjustment == null ? ZERO : adjustment.setScale(4, RoundingMode.HALF_UP));
        param.setStatus(observing ? "OBSERVING" : "ACTIVE");
        param.setEnabled(!observing);
        param.setIsManual(false);
        param.setSampleSize(aggregation.getSampleSize());
        param.setUniqueUserCount(aggregation.getUniqueUserCount());
        param.setFeedbackDistribution(enrichFeedbackDistribution(aggregation, direction, targetSizeId));
        param.setConfidenceLevel(resolveConfidenceLevel(aggregation));
        param.setEffectiveFrom(now);
        param.setEffectiveUntil(null);
        param.setVersion(resolveNextVersion(aggregation.getScopeType(), aggregation.getScopeId(), targetSizeId));
        param.setCreateBy("auto-job");
        param.setCreateTime(now);
        param.setUpdateBy("auto-job");
        param.setUpdateTime(now);
        return param;
    }

    private String enrichFeedbackDistribution(FeedbackAggregation aggregation, String direction, Long targetSizeId) {
        Map<String, Object> distribution = new LinkedHashMap<>();
        distribution.put("fit", aggregation.getWeightedFitCount());
        distribution.put("tooLarge", aggregation.getWeightedTooLargeCount());
        distribution.put("tooSmall", aggregation.getWeightedTooSmallCount());
        distribution.put("fitRatio", aggregation.getWeightedFitRatio());
        distribution.put("direction", direction);
        distribution.put("targetSizeId", targetSizeId);
        return writeJson(distribution);
    }

    private String currentDirectionLabel(FeedbackAggregation aggregation, Direction direction) {
        if (direction != Direction.NONE) {
            return direction.name();
        }
        return aggregation != null && aggregation.getWeightedFitRatio().compareTo(BigDecimal.valueOf(0.7)) >= 0
                ? "FIT"
                : "CURRENT";
    }

    private Direction resolveDirection(FeedbackAggregation aggregation) {
        if (aggregation == null || aggregation.getTargetSizeId() == null) {
            return Direction.NONE;
        }
        BigDecimal total = aggregation.getTotalWeightedCount();
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return Direction.NONE;
        }
        BigDecimal tooSmallRatio = ratio(aggregation.getWeightedTooSmallCount(), total);
        BigDecimal tooLargeRatio = ratio(aggregation.getWeightedTooLargeCount(), total);
        if (tooSmallRatio.subtract(tooLargeRatio).compareTo(DIRECTION_CONFIDENCE_MARGIN) >= 0) {
            return Direction.TOO_SMALL;
        }
        if (tooLargeRatio.subtract(tooSmallRatio).compareTo(DIRECTION_CONFIDENCE_MARGIN) >= 0) {
            return Direction.TOO_LARGE;
        }
        return Direction.NONE;
    }

    private BigDecimal ratio(BigDecimal count, BigDecimal total) {
        if (count == null || total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return count.divide(total, 4, RoundingMode.HALF_UP);
    }

    private Map<Long, SSizes> loadSizesById(List<FeedbackAggregation> aggregations) {
        Set<Long> targetIds = aggregations == null
                ? Set.of()
                : aggregations.stream()
                .map(FeedbackAggregation::getTargetSizeId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (targetIds.isEmpty() || sSizesMapper == null) {
            return Map.of();
        }
        List<SSizes> sizes = sSizesMapper.selectAllSizes();
        if (sizes == null || sizes.isEmpty()) {
            return Map.of();
        }
        return sizes.stream()
                .filter(size -> size != null && size.getId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        SSizes::getId,
                        size -> size,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Long resolveAdjacentSizeId(Long targetSizeId, Direction direction, Map<Long, SSizes> sizeById) {
        if (targetSizeId == null || direction == Direction.NONE || sizeById == null || sizeById.isEmpty()) {
            return null;
        }
        List<SSizes> ordered = sizeById.values().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing((SSizes size) -> nullableDecimal(size.getMinHeight()))
                        .thenComparing(size -> nullableDecimal(size.getMinWeight()))
                        .thenComparing(SSizes::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        for (int index = 0; index < ordered.size(); index++) {
            if (targetSizeId.equals(ordered.get(index).getId())) {
                int adjacentIndex = direction == Direction.TOO_SMALL ? index + 1 : index - 1;
                if (adjacentIndex < 0 || adjacentIndex >= ordered.size()) {
                    return null;
                }
                return ordered.get(adjacentIndex).getId();
            }
        }
        return null;
    }

    private BigDecimal nullableDecimal(Long value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private BigDecimal nullableDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String resolveConfidenceLevel(FeedbackAggregation aggregation) {
        if (aggregation.getSampleSize() >= 50 && aggregation.getUniqueUserCount() >= 20) {
            return "HIGH";
        }
        if (aggregation.getSampleSize() >= calibrationProperties.getThresholds().getMinFeedbackCount()
                && aggregation.getUniqueUserCount() >= calibrationProperties.getThresholds().getMinUniqueUsers()) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private Integer resolveNextVersion(String scopeType, String scopeId, Long targetSizeId) {
        Integer current = calibrationParamsMapper.selectLatestVersion(scopeType, scopeId, targetSizeId);
        return (current == null ? 0 : current) + 1;
    }

    private String buildCacheKey(Long schoolId, Long uniformId, Collection<Long> candidateSizeIds) {
        return (schoolId == null ? "null" : schoolId)
                + "|"
                + (uniformId == null ? "null" : uniformId)
                + "|"
                + candidateSizeIds;
    }

    private Long latestAuditRevision() {
        Long revision = calibrationAuditMapper.selectLatestAuditRevision();
        return revision == null ? 0L : revision;
    }

    private CalibrationBundle cloneBundle(CalibrationBundle source) {
        CalibrationBundle copy = new CalibrationBundle();
        copy.setOffsetsBySizeId(new LinkedHashMap<>(source.getOffsetsBySizeId()));
        copy.setProvenanceBySizeId(new LinkedHashMap<>(source.getProvenanceBySizeId()));
        Map<Long, Map<String, Object>> details = new LinkedHashMap<>();
        source.getDetailsBySizeId().forEach((sizeId, value) ->
                details.put(sizeId, value == null ? Map.of() : new LinkedHashMap<>(value)));
        copy.setDetailsBySizeId(details);
        return copy;
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

    private record CacheEntry(CalibrationBundle bundle, Instant expiresAt, Long auditRevision) {
    }

    private enum Direction {
        NONE,
        TOO_SMALL,
        TOO_LARGE
    }

    private final class AggregationAccumulator {
        private final String scopeType;
        private final String scopeId;
        private final Long targetSizeId;
        private int sampleSize;
        private final Set<Long> uniqueUsers = new LinkedHashSet<>();
        private BigDecimal weightedFitCount = BigDecimal.ZERO;
        private BigDecimal weightedTooLargeCount = BigDecimal.ZERO;
        private BigDecimal weightedTooSmallCount = BigDecimal.ZERO;
        private BigDecimal totalWeightedCount = BigDecimal.ZERO;

        private AggregationAccumulator(String scopeType, String scopeId, Long targetSizeId) {
            this.scopeType = scopeType;
            this.scopeId = scopeId;
            this.targetSizeId = targetSizeId;
        }

        private FeedbackAggregation toDto() {
            FeedbackAggregation aggregation = new FeedbackAggregation();
            aggregation.setScopeType(scopeType);
            aggregation.setScopeId(scopeId);
            aggregation.setTargetSizeId(targetSizeId);
            aggregation.setSampleSize(sampleSize);
            aggregation.setUniqueUserCount(uniqueUsers.size());
            aggregation.setWeightedFitCount(weightedFitCount.setScale(6, RoundingMode.HALF_UP));
            aggregation.setWeightedTooLargeCount(weightedTooLargeCount.setScale(6, RoundingMode.HALF_UP));
            aggregation.setWeightedTooSmallCount(weightedTooSmallCount.setScale(6, RoundingMode.HALF_UP));
            aggregation.setTotalWeightedCount(totalWeightedCount.setScale(6, RoundingMode.HALF_UP));

            Map<String, Object> distribution = new LinkedHashMap<>();
            distribution.put("fit", aggregation.getWeightedFitCount());
            distribution.put("tooLarge", aggregation.getWeightedTooLargeCount());
            distribution.put("tooSmall", aggregation.getWeightedTooSmallCount());
            distribution.put("fitRatio", aggregation.getWeightedFitRatio());
            aggregation.setFeedbackDistribution(writeJson(distribution));
            return aggregation;
        }
    }
}
