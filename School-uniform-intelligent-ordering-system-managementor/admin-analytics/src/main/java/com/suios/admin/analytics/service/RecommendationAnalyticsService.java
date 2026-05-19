package com.suios.admin.analytics.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.analytics.dto.FeedbackDistributionDto;
import com.suios.admin.analytics.dto.FeedbackDistributionResponse;
import com.suios.admin.analytics.dto.LowConfidenceHotspotsResponse;
import com.suios.admin.analytics.dto.RecommendationExperimentMetricsResponse;
import com.suios.admin.analytics.dto.RecommendationExperimentVariantMetricsDto;
import com.suios.admin.analytics.dto.RecommendationStatsDto;
import com.suios.admin.analytics.dto.RecommendationTrendPointDto;
import com.suios.admin.analytics.mapper.RecommendationAnalyticsMapper;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.security.AuthContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RecommendationAnalyticsService {

    private final RecommendationAnalyticsMapper recommendationAnalyticsMapper;
    private final AuthContext authContext;

    @Value("${analytics.max-page-size:200}")
    private int maxPageSize;

    public RecommendationAnalyticsService(RecommendationAnalyticsMapper recommendationAnalyticsMapper,
                                          AuthContext authContext) {
        this.recommendationAnalyticsMapper = recommendationAnalyticsMapper;
        this.authContext = authContext;
    }

    public RecommendationStatsDto getRecommendationStats(Long schoolId,
                                                         Long uniformId,
                                                         LocalDate startDate,
                                                         LocalDate endDate) {
        requireAdminAuthenticated();
        LocalDateRange range = requireDateRange(startDate, endDate);
        RecommendationStatsDto stats = recommendationAnalyticsMapper.selectRecommendationStats(
                schoolId,
                uniformId,
                range.startTime(),
                range.endTime()
        );
        if (stats == null) {
            stats = new RecommendationStatsDto();
        }

        stats.setTotalRecommendations(defaultLong(stats.getTotalRecommendations()));
        stats.setLinkedOrderCount(defaultLong(stats.getLinkedOrderCount()));
        stats.setTotalFeedbacks(defaultLong(stats.getTotalFeedbacks()));
        stats.setFitCount(defaultLong(stats.getFitCount()));
        stats.setTooLargeCount(defaultLong(stats.getTooLargeCount()));
        stats.setTooSmallCount(defaultLong(stats.getTooSmallCount()));
        stats.setLowConfidenceCount(defaultLong(stats.getLowConfidenceCount()));
        stats.setAvgConfidenceScore(stats.getAvgConfidenceScore() == null ? 0 : stats.getAvgConfidenceScore());

        stats.setLinkedOrderCoverage(ratio(stats.getLinkedOrderCount(), stats.getTotalRecommendations()));
        stats.setFeedbackCoverage(ratio(stats.getTotalFeedbacks(), stats.getLinkedOrderCount()));
        stats.setFitRate(ratio(stats.getFitCount(), stats.getTotalFeedbacks()));
        stats.setLowConfidenceRate(ratio(stats.getLowConfidenceCount(), stats.getTotalRecommendations()));

        Map<String, Long> satisfactionDistribution = new LinkedHashMap<>();
        satisfactionDistribution.put("FIT", stats.getFitCount());
        satisfactionDistribution.put("TOO_LARGE", stats.getTooLargeCount());
        satisfactionDistribution.put("TOO_SMALL", stats.getTooSmallCount());
        stats.setSatisfactionDistribution(satisfactionDistribution);

        List<RecommendationTrendPointDto> rawTrend = recommendationAnalyticsMapper.selectRecommendationTrend(
                schoolId,
                uniformId,
                range.startTime(),
                range.endTime()
        );
        stats.setTrend(fillTrendGaps(startDate, endDate, rawTrend));
        return stats;
    }

    public FeedbackDistributionResponse getFeedbackDistribution(String groupBy,
                                                                Long schoolId,
                                                                Long uniformId,
                                                                LocalDate startDate,
                                                                LocalDate endDate,
                                                                Integer limit,
                                                                Integer offset,
                                                                String sortBy,
                                                                String sortOrder) {
        requireAdminAuthenticated();
        LocalDateRange range = requireDateRange(startDate, endDate);
        String normalizedGroupBy = normalizeGroupBy(groupBy);
        String normalizedSortBy = normalizeSortBy(sortBy);
        String normalizedSortOrder = "desc".equalsIgnoreCase(sortOrder) ? "desc" : "asc";
        int safeLimit = clampLimit(limit);
        int safeOffset = Math.max(0, offset == null ? 0 : offset);
        long pageNum = safeOffset / safeLimit + 1L;

        IPage<FeedbackDistributionDto> page = recommendationAnalyticsMapper.selectFeedbackDistribution(
                new Page<>(pageNum, safeLimit),
                normalizedGroupBy,
                schoolId,
                uniformId,
                range.startTime(),
                range.endTime(),
                normalizedSortBy,
                normalizedSortOrder
        );

        FeedbackDistributionResponse response = new FeedbackDistributionResponse();
        response.setGroups(page.getRecords() == null ? List.of() : page.getRecords());
        response.setTotal(page.getTotal());
        response.setLimit(safeLimit);
        response.setOffset(safeOffset);
        return response;
    }

    public LowConfidenceHotspotsResponse getLowConfidenceHotspots(Long schoolId,
                                                                  Long uniformId,
                                                                  Integer threshold,
                                                                  Integer limit,
                                                                  LocalDate startDate,
                                                                  LocalDate endDate) {
        requireAdminAuthenticated();
        int safeThreshold = threshold == null ? 60 : Math.max(0, Math.min(threshold, 100));
        int safeLimit = clampLimit(limit == null ? 20 : limit);
        LocalDateRange range = optionalDateRange(startDate, endDate);

        LowConfidenceHotspotsResponse response = new LowConfidenceHotspotsResponse();
        response.setHotspots(Objects.requireNonNullElseGet(recommendationAnalyticsMapper.selectLowConfidenceHotspots(
                schoolId,
                uniformId,
                safeThreshold,
                safeLimit,
                range == null ? null : range.startTime(),
                range == null ? null : range.endTime()
        ), List::of));
        return response;
    }

    public RecommendationExperimentMetricsResponse getExperimentMetrics(Long schoolId,
                                                                        Long uniformId,
                                                                        LocalDate startDate,
                                                                        LocalDate endDate) {
        requireAdminAuthenticated();
        LocalDateRange range = requireDateRange(startDate, endDate);
        List<RecommendationExperimentVariantMetricsDto> rawRows = Objects.requireNonNullElseGet(
                recommendationAnalyticsMapper.selectExperimentMetrics(schoolId, uniformId, range.startTime(), range.endTime()),
                List::of
        );
        Map<String, RecommendationExperimentVariantMetricsDto> byVariant = rawRows.stream()
                .filter(row -> row != null && row.getExperimentVariant() != null)
                .collect(Collectors.toMap(
                        RecommendationExperimentVariantMetricsDto::getExperimentVariant,
                        this::normalizeExperimentMetrics,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        RecommendationExperimentMetricsResponse response = new RecommendationExperimentMetricsResponse();
        response.setVariants(List.of(
                byVariant.getOrDefault("A", emptyExperimentVariant("A")),
                byVariant.getOrDefault("B", emptyExperimentVariant("B"))
        ));
        return response;
    }

    private void requireAdminAuthenticated() {
        if (authContext.getCurrentUserId() == null) {
            throw new BizException("登录状态无效");
        }
    }

    private LocalDateRange requireDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BizException("请选择开始日期和结束日期");
        }
        if (endDate.isBefore(startDate)) {
            throw new BizException("结束日期不能早于开始日期");
        }
        return new LocalDateRange(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
    }

    private LocalDateRange optionalDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }
        LocalDate safeStart = startDate == null ? endDate : startDate;
        LocalDate safeEnd = endDate == null ? startDate : endDate;
        if (safeEnd.isBefore(safeStart)) {
            throw new BizException("结束日期不能早于开始日期");
        }
        return new LocalDateRange(safeStart.atStartOfDay(), safeEnd.atTime(LocalTime.MAX));
    }

    private int clampLimit(Integer limit) {
        int safeLimit = limit == null ? 20 : limit;
        safeLimit = Math.max(1, safeLimit);
        return Math.min(safeLimit, Math.max(1, maxPageSize));
    }

    private String normalizeGroupBy(String groupBy) {
        if ("school".equalsIgnoreCase(groupBy) || "product".equalsIgnoreCase(groupBy) || "size".equalsIgnoreCase(groupBy)) {
            return groupBy.toLowerCase();
        }
        throw new BizException("不支持的分组类型");
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "fitRate";
        }
        return switch (sortBy) {
            case "fitRate", "totalFeedback", "fit", "tooLarge", "tooSmall", "tooLargeRate", "tooSmallRate" -> sortBy;
            default -> throw new BizException("不支持的排序字段");
        };
    }

    private List<RecommendationTrendPointDto> fillTrendGaps(LocalDate startDate,
                                                            LocalDate endDate,
                                                            List<RecommendationTrendPointDto> rawTrend) {
        Map<String, RecommendationTrendPointDto> trendMap = (rawTrend == null ? new ArrayList<RecommendationTrendPointDto>() : rawTrend)
                .stream()
                .collect(Collectors.toMap(RecommendationTrendPointDto::getDay, Function.identity(), (left, right) -> right));

        List<RecommendationTrendPointDto> normalized = new ArrayList<>();
        for (LocalDate cursor = startDate; !cursor.isAfter(endDate); cursor = cursor.plusDays(1)) {
            String day = cursor.toString();
            RecommendationTrendPointDto point = trendMap.get(day);
            if (point == null) {
                point = new RecommendationTrendPointDto();
                point.setDay(day);
                point.setTotalRecommendations(0L);
                point.setLinkedOrderCount(0L);
                point.setTotalFeedbacks(0L);
                point.setFitCount(0L);
                point.setTooLargeCount(0L);
                point.setTooSmallCount(0L);
                point.setFitRate(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
                point.setAvgConfidenceScore(0);
            } else {
                point.setTotalRecommendations(defaultLong(point.getTotalRecommendations()));
                point.setLinkedOrderCount(defaultLong(point.getLinkedOrderCount()));
                point.setTotalFeedbacks(defaultLong(point.getTotalFeedbacks()));
                point.setFitCount(defaultLong(point.getFitCount()));
                point.setTooLargeCount(defaultLong(point.getTooLargeCount()));
                point.setTooSmallCount(defaultLong(point.getTooSmallCount()));
                point.setFitRate(defaultDecimal(point.getFitRate()));
                point.setAvgConfidenceScore(point.getAvgConfidenceScore() == null ? 0 : point.getAvgConfidenceScore());
            }
            normalized.add(point);
        }
        return normalized;
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private RecommendationExperimentVariantMetricsDto normalizeExperimentMetrics(RecommendationExperimentVariantMetricsDto metrics) {
        metrics.setTotalRecommendations(defaultLong(metrics.getTotalRecommendations()));
        metrics.setLinkedOrderCount(defaultLong(metrics.getLinkedOrderCount()));
        metrics.setTotalFeedbacks(defaultLong(metrics.getTotalFeedbacks()));
        metrics.setFitCount(defaultLong(metrics.getFitCount()));
        metrics.setTooLargeCount(defaultLong(metrics.getTooLargeCount()));
        metrics.setTooSmallCount(defaultLong(metrics.getTooSmallCount()));
        metrics.setLowConfidenceCount(defaultLong(metrics.getLowConfidenceCount()));
        metrics.setCalibrationHitCount(defaultLong(metrics.getCalibrationHitCount()));
        metrics.setCalibrationAppliedCount(defaultLong(metrics.getCalibrationAppliedCount()));
        metrics.setRecommendationChangedCount(defaultLong(metrics.getRecommendationChangedCount()));
        metrics.setSafetyGateSkipCount(defaultLong(metrics.getSafetyGateSkipCount()));
        metrics.setAvgConfidenceScore(metrics.getAvgConfidenceScore() == null ? 0 : metrics.getAvgConfidenceScore());

        metrics.setFitRate(ratio(metrics.getFitCount(), metrics.getTotalFeedbacks()));
        metrics.setLowConfidenceRate(ratio(metrics.getLowConfidenceCount(), metrics.getTotalRecommendations()));
        metrics.setFeedbackSubmissionRate(ratio(metrics.getTotalFeedbacks(), metrics.getLinkedOrderCount()));
        metrics.setRecommendationToOrderConversionRate(ratio(metrics.getLinkedOrderCount(), metrics.getTotalRecommendations()));
        metrics.setSizeIssueRate(ratio(metrics.getTooLargeCount() + metrics.getTooSmallCount(), metrics.getTotalFeedbacks()));

        if ("B".equals(metrics.getExperimentVariant())) {
            metrics.setCalibrationHitRate(ratio(metrics.getCalibrationHitCount(), metrics.getTotalRecommendations()));
            metrics.setCalibrationApplicationRate(ratio(metrics.getCalibrationAppliedCount(), metrics.getTotalRecommendations()));
            metrics.setRecommendationChangeRate(ratio(metrics.getRecommendationChangedCount(), metrics.getTotalRecommendations()));
        } else {
            metrics.setCalibrationHitCount(0L);
            metrics.setCalibrationAppliedCount(0L);
            metrics.setRecommendationChangedCount(0L);
            metrics.setSafetyGateSkipCount(0L);
            metrics.setCalibrationHitRate(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            metrics.setCalibrationApplicationRate(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            metrics.setRecommendationChangeRate(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        }
        return metrics;
    }

    private RecommendationExperimentVariantMetricsDto emptyExperimentVariant(String variant) {
        RecommendationExperimentVariantMetricsDto metrics = new RecommendationExperimentVariantMetricsDto();
        metrics.setExperimentVariant(variant);
        return normalizeExperimentMetrics(metrics);
    }

    private record LocalDateRange(LocalDateTime startTime, LocalDateTime endTime) {
    }
}
