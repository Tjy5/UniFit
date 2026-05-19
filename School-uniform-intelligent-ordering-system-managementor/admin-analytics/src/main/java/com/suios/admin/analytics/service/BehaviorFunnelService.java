package com.suios.admin.analytics.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.analytics.dto.BehaviorFunnelResponse;
import com.suios.admin.analytics.dto.BehaviorFunnelStageCountDto;
import com.suios.admin.analytics.dto.BehaviorFunnelStageDto;
import com.suios.admin.analytics.dto.DropOffDistributionResponse;
import com.suios.admin.analytics.dto.DropOffDistributionRowDto;
import com.suios.admin.analytics.dto.RecommendationAdoptionStatsDto;
import com.suios.admin.analytics.dto.RecommendationEntryComparisonResponse;
import com.suios.admin.analytics.dto.RecommendationEntryComparisonRowDto;
import com.suios.admin.analytics.dto.StalledCartRowDto;
import com.suios.admin.analytics.dto.StalledCartsResponse;
import com.suios.admin.analytics.mapper.BehaviorFunnelMapper;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.security.AuthContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BehaviorFunnelService {

    private static final List<StageDefinition> STAGES = List.of(
            new StageDefinition("browse", "浏览"),
            new StageDefinition("detail", "详情"),
            new StageDefinition("recommend", "推荐"),
            new StageDefinition("cart", "加购"),
            new StageDefinition("order", "下单")
    );

    private static final Set<String> KNOWN_SOURCES = Set.of(
            "mall-home-dialog",
            "wishlist",
            "order-feedback",
            "product-detail",
            "unknown"
    );

    private final BehaviorFunnelMapper behaviorFunnelMapper;
    private final AuthContext authContext;

    @Value("${analytics.max-page-size:200}")
    private int maxPageSize;

    @Value("${analytics.max-funnel-range-days:90}")
    private int maxFunnelRangeDays;

    @Value("${funnel.session-inactivity-gap-minutes:30}")
    private int sessionInactivityGapMinutes;

    @Value("${funnel.stalled-cart-threshold-hours:48}")
    private int stalledCartThresholdHours;

    public BehaviorFunnelService(BehaviorFunnelMapper behaviorFunnelMapper,
                                 AuthContext authContext) {
        this.behaviorFunnelMapper = behaviorFunnelMapper;
        this.authContext = authContext;
    }

    public BehaviorFunnelResponse getBehaviorFunnel(Long schoolId,
                                                    Long gradeId,
                                                    Long uniformId,
                                                    String source,
                                                    LocalDate startDate,
                                                    LocalDate endDate) {
        requireAdminAuthenticated();
        LocalDateRange range = requireDateRange(startDate, endDate);
        String normalizedSource = normalizeSourceFilter(source);
        List<BehaviorFunnelStageCountDto> rows = behaviorFunnelMapper.selectFunnelStageCounts(
                schoolId,
                gradeId,
                uniformId,
                normalizedSource,
                range.startTime(),
                range.endTime(),
                safeSessionGap()
        );
        if (rows == null) {
            rows = List.of();
        }

        Map<String, Long> countsByStage = rows.stream()
                .filter(row -> row != null && row.getStage() != null)
                .collect(Collectors.toMap(
                        BehaviorFunnelStageCountDto::getStage,
                        row -> defaultLong(row.getSessionsReached()),
                        Long::sum,
                        LinkedHashMap::new
                ));

        List<BehaviorFunnelStageDto> stages = new ArrayList<>();
        for (int i = 0; i < STAGES.size(); i++) {
            StageDefinition definition = STAGES.get(i);
            long current = countsByStage.getOrDefault(definition.stage(), 0L);
            long next = i + 1 < STAGES.size() ? countsByStage.getOrDefault(STAGES.get(i + 1).stage(), 0L) : 0L;

            BehaviorFunnelStageDto stage = new BehaviorFunnelStageDto();
            stage.setStage(definition.stage());
            stage.setLabel(definition.label());
            stage.setSessionsReached(current);
            stage.setConversionRate(i + 1 < STAGES.size() ? ratio(next, current) : zeroRate());
            stages.add(stage);
        }

        BehaviorFunnelResponse response = new BehaviorFunnelResponse();
        response.setStages(stages);
        response.setTotalSessions(stages.isEmpty() ? 0L : stages.get(0).getSessionsReached());
        response.setSessionInactivityGapMinutes(safeSessionGap());
        return response;
    }

    public RecommendationEntryComparisonResponse getEntryComparison(Long schoolId,
                                                                    Long gradeId,
                                                                    Long uniformId,
                                                                    String source,
                                                                    LocalDate startDate,
                                                                    LocalDate endDate) {
        requireAdminAuthenticated();
        LocalDateRange range = requireDateRange(startDate, endDate);
        String normalizedSource = normalizeSourceFilter(source);
        List<RecommendationEntryComparisonRowDto> rows = behaviorFunnelMapper.selectEntryComparison(
                schoolId,
                gradeId,
                uniformId,
                normalizedSource,
                range.startTime(),
                range.endTime()
        );
        rows = (rows == null ? List.<RecommendationEntryComparisonRowDto>of() : rows).stream()
                .map(this::normalizeEntryComparisonRow)
                .collect(Collectors.toCollection(ArrayList::new));

        ensureUnknownEntryRow(rows, normalizedSource);
        rows.sort(Comparator.comparing(RecommendationEntryComparisonRowDto::getOrderConversionRate).reversed()
                .thenComparing(RecommendationEntryComparisonRowDto::getRequestSource));

        RecommendationEntryComparisonResponse response = new RecommendationEntryComparisonResponse();
        response.setRows(rows);
        return response;
    }

    public RecommendationAdoptionStatsDto getRecommendationAdoption(Long schoolId,
                                                                    Long gradeId,
                                                                    Long uniformId,
                                                                    String source,
                                                                    LocalDate startDate,
                                                                    LocalDate endDate) {
        requireAdminAuthenticated();
        LocalDateRange range = requireDateRange(startDate, endDate);
        RecommendationAdoptionStatsDto stats = behaviorFunnelMapper.selectRecommendationAdoption(
                schoolId,
                gradeId,
                uniformId,
                normalizeSourceFilter(source),
                range.startTime(),
                range.endTime()
        );
        return normalizeAdoptionStats(stats);
    }

    public DropOffDistributionResponse getDropOffDistribution(Long schoolId,
                                                              Long gradeId,
                                                              Long uniformId,
                                                              String source,
                                                              LocalDate startDate,
                                                              LocalDate endDate,
                                                              Integer limit) {
        requireAdminAuthenticated();
        LocalDateRange range = requireDateRange(startDate, endDate);
        int safeLimit = clampLimit(limit == null ? 20 : limit);
        List<DropOffDistributionRowDto> rows = behaviorFunnelMapper.selectDropOffDistribution(
                schoolId,
                gradeId,
                uniformId,
                normalizeSourceFilter(source),
                range.startTime(),
                range.endTime(),
                safeSessionGap()
        );
        if (rows == null) {
            rows = List.of();
        }

        List<DropOffDistributionRowDto> merged = mergeOtherBucket(rows, safeLimit);
        long totalSessions = merged.stream().mapToLong(row -> defaultLong(row.getSessionCount())).sum();
        merged.forEach(row -> row.setShare(ratio(defaultLong(row.getSessionCount()), totalSessions)));

        DropOffDistributionResponse response = new DropOffDistributionResponse();
        response.setRows(merged);
        response.setTotalSessions(totalSessions);
        response.setLimit(safeLimit);
        return response;
    }

    public StalledCartsResponse getStalledCarts(Long schoolId,
                                                Long gradeId,
                                                Long uniformId,
                                                LocalDate startDate,
                                                LocalDate endDate,
                                                Integer limit,
                                                Integer offset) {
        requireAdminAuthenticated();
        LocalDateRange range = requireDateRange(startDate, endDate);
        int safeLimit = clampLimit(limit == null ? 20 : limit);
        int safeOffset = Math.max(0, offset == null ? 0 : offset);
        long pageNum = safeOffset / safeLimit + 1L;

        IPage<StalledCartRowDto> page = behaviorFunnelMapper.selectStalledCarts(
                new Page<>(pageNum, safeLimit),
                schoolId,
                gradeId,
                uniformId,
                range.startTime(),
                range.endTime(),
                safeStalledCartThresholdHours()
        );

        StalledCartsResponse response = new StalledCartsResponse();
        response.setRecords(page == null || page.getRecords() == null ? List.of() : page.getRecords());
        response.setTotal(page == null ? 0L : page.getTotal());
        response.setLimit(safeLimit);
        response.setOffset(safeOffset);
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
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1L;
        int safeMaxRange = Math.max(1, maxFunnelRangeDays);
        if (days > safeMaxRange) {
            throw new BizException("行为分析日期范围不能超过" + safeMaxRange + "天");
        }
        return new LocalDateRange(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
    }

    private int clampLimit(Integer limit) {
        int safeLimit = limit == null ? 20 : limit;
        safeLimit = Math.max(1, safeLimit);
        return Math.min(safeLimit, Math.max(1, maxPageSize));
    }

    private int safeSessionGap() {
        return Math.max(1, sessionInactivityGapMinutes);
    }

    private int safeStalledCartThresholdHours() {
        return Math.max(1, stalledCartThresholdHours);
    }

    private String normalizeSourceFilter(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        String canonical = source.trim()
                .toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replace(' ', '-');
        return KNOWN_SOURCES.contains(canonical) ? canonical : "unknown";
    }

    private RecommendationEntryComparisonRowDto normalizeEntryComparisonRow(RecommendationEntryComparisonRowDto row) {
        RecommendationEntryComparisonRowDto normalized = row == null ? new RecommendationEntryComparisonRowDto() : row;
        normalized.setRequestSource(normalizeSourceFilter(normalized.getRequestSource()) == null
                ? "unknown"
                : normalizeSourceFilter(normalized.getRequestSource()));
        normalized.setExposureCount(defaultLong(normalized.getExposureCount()));
        normalized.setLinkedCartCount(defaultLong(normalized.getLinkedCartCount()));
        normalized.setLinkedOrderCount(defaultLong(normalized.getLinkedOrderCount()));
        normalized.setAdoptedCount(defaultLong(normalized.getAdoptedCount()));
        normalized.setCartConversionRate(ratio(normalized.getLinkedCartCount(), normalized.getExposureCount()));
        normalized.setOrderConversionRate(ratio(normalized.getLinkedOrderCount(), normalized.getExposureCount()));
        normalized.setAdoptionRate(ratio(normalized.getAdoptedCount(), normalized.getLinkedOrderCount()));
        return normalized;
    }

    private void ensureUnknownEntryRow(List<RecommendationEntryComparisonRowDto> rows, String sourceFilter) {
        if (sourceFilter != null && !"unknown".equals(sourceFilter)) {
            return;
        }
        boolean hasUnknown = rows.stream().anyMatch(row -> "unknown".equals(row.getRequestSource()));
        if (!hasUnknown) {
            RecommendationEntryComparisonRowDto unknown = new RecommendationEntryComparisonRowDto();
            unknown.setRequestSource("unknown");
            rows.add(normalizeEntryComparisonRow(unknown));
        }
    }

    private RecommendationAdoptionStatsDto normalizeAdoptionStats(RecommendationAdoptionStatsDto stats) {
        RecommendationAdoptionStatsDto normalized = stats == null ? new RecommendationAdoptionStatsDto() : stats;
        normalized.setLinkedOrderCount(defaultLong(normalized.getLinkedOrderCount()));
        normalized.setAdoptedCount(defaultLong(normalized.getAdoptedCount()));
        normalized.setWithFeedbackCount(defaultLong(normalized.getWithFeedbackCount()));
        normalized.setSizeChangeCount(Math.max(0L, normalized.getLinkedOrderCount() - normalized.getAdoptedCount()));
        normalized.setWithoutFeedbackCount(Math.max(0L, normalized.getLinkedOrderCount() - normalized.getWithFeedbackCount()));
        normalized.setAdoptionRate(ratio(normalized.getAdoptedCount(), normalized.getLinkedOrderCount()));
        normalized.setSizeChangeRate(ratio(normalized.getSizeChangeCount(), normalized.getLinkedOrderCount()));
        return normalized;
    }

    List<DropOffDistributionRowDto> mergeOtherBucket(List<DropOffDistributionRowDto> rawRows, int limit) {
        List<DropOffDistributionRowDto> sorted = (rawRows == null ? List.<DropOffDistributionRowDto>of() : rawRows)
                .stream()
                .filter(Objects::nonNull)
                .map(this::normalizeDropOffRow)
                .sorted(Comparator.comparing(DropOffDistributionRowDto::getSessionCount).reversed()
                        .thenComparing(DropOffDistributionRowDto::getLastActionType))
                .collect(Collectors.toCollection(ArrayList::new));
        if (sorted.size() <= limit) {
            return sorted;
        }
        List<DropOffDistributionRowDto> kept = new ArrayList<>(sorted.subList(0, limit));
        long otherCount = sorted.subList(limit, sorted.size())
                .stream()
                .mapToLong(row -> defaultLong(row.getSessionCount()))
                .sum();
        if (otherCount > 0) {
            DropOffDistributionRowDto other = new DropOffDistributionRowDto();
            other.setLastActionType("OTHER");
            other.setSessionCount(otherCount);
            other.setShare(zeroRate());
            kept.add(other);
        }
        return kept;
    }

    private DropOffDistributionRowDto normalizeDropOffRow(DropOffDistributionRowDto row) {
        row.setLastActionType(row.getLastActionType() == null || row.getLastActionType().isBlank()
                ? "UNKNOWN"
                : row.getLastActionType());
        row.setSessionCount(defaultLong(row.getSessionCount()));
        row.setShare(row.getShare() == null ? zeroRate() : row.getShare().setScale(4, RoundingMode.HALF_UP));
        return row;
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return zeroRate();
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroRate() {
        return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }

    private record StageDefinition(String stage, String label) {
    }

    private record LocalDateRange(LocalDateTime startTime, LocalDateTime endTime) {
    }
}
