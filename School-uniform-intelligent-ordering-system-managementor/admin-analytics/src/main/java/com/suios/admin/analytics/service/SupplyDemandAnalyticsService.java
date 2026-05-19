package com.suios.admin.analytics.service;

import com.suios.admin.analytics.dto.ReplenishmentSuggestionDto;
import com.suios.admin.analytics.dto.SupplyForecastDto;
import com.suios.admin.analytics.dto.SupplyLowStockAlertDto;
import com.suios.admin.analytics.dto.SupplySalesAnalyticsDto;
import com.suios.admin.analytics.dto.SupplyTrendPointDto;
import com.suios.admin.analytics.mapper.SupplyDemandAnalyticsMapper;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.security.AuthContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SupplyDemandAnalyticsService {

    private final SupplyDemandAnalyticsMapper mapper;
    private final AuthContext authContext;

    public SupplyDemandAnalyticsService(SupplyDemandAnalyticsMapper mapper, AuthContext authContext) {
        this.mapper = mapper;
        this.authContext = authContext;
    }

    public List<SupplySalesAnalyticsDto> salesAnalytics(String groupBy,
                                                        Long schoolId,
                                                        Long gradeId,
                                                        Long uniformId,
                                                        Long sizeId,
                                                        String categoryKey,
                                                        LocalDate startDate,
                                                        LocalDate endDate) {
        requireAdminAuthenticated();
        DateRange range = requireDateRange(startDate, endDate);
        return mapper.selectSalesAnalytics(normalizeGroupBy(groupBy), schoolId, gradeId, uniformId, sizeId, categoryKey, range.startTime(), range.endTime());
    }

    public List<SupplyTrendPointDto> trend(String bucketType,
                                           Long schoolId,
                                           Long gradeId,
                                           Long uniformId,
                                           Long sizeId,
                                           String categoryKey,
                                           LocalDate startDate,
                                           LocalDate endDate) {
        requireAdminAuthenticated();
        DateRange range = requireDateRange(startDate, endDate);
        String normalizedBucket = normalizeBucketType(bucketType);
        List<SupplyTrendPointDto> rawRows = Objects.requireNonNullElseGet(
                mapper.selectTrend(normalizedBucket, schoolId, gradeId, uniformId, sizeId, categoryKey, range.startTime(), range.endTime()),
                List::of
        );
        return fillTrendGaps(normalizedBucket, startDate, endDate, rawRows);
    }

    public List<SupplyLowStockAlertDto> lowStockAlerts(Long schoolId,
                                                       Long gradeId,
                                                       Long uniformId,
                                                       Long sizeId,
                                                       String categoryKey,
                                                       String severity) {
        requireAdminAuthenticated();
        String normalizedSeverity = StringUtils.hasText(severity) ? severity.trim().toUpperCase() : null;
        List<SupplyLowStockAlertDto> rows = mapper.selectLowStockAlerts(schoolId, gradeId, uniformId, sizeId, categoryKey);
        if (rows == null) {
            rows = List.of();
        }
        return rows.stream()
                .filter(row -> normalizedSeverity == null || normalizedSeverity.equals(row.getSeverity()))
                .toList();
    }

    public List<SupplyForecastDto> forecasts(Long schoolId,
                                             Long gradeId,
                                             Long uniformId,
                                             Long sizeId,
                                             String categoryKey,
                                             Integer lookbackDays,
                                             Integer horizonDays) {
        requireAdminAuthenticated();
        int safeLookback = clamp(lookbackDays == null ? 90 : lookbackDays, 7, 365);
        int safeHorizon = clamp(horizonDays == null ? 30 : horizonDays, 1, 180);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeLookback - 1L);
        DateRange range = requireDateRange(startDate, endDate);
        List<SupplyForecastDto> rows = mapper.selectForecastBase(schoolId, gradeId, uniformId, sizeId, categoryKey, range.startTime(), range.endTime());
        if (rows == null) {
            rows = List.of();
        }
        return rows.stream()
                .map(row -> normalizeForecast(row, safeLookback, safeHorizon))
                .toList();
    }

    public List<ReplenishmentSuggestionDto> replenishmentSuggestions(Long schoolId,
                                                                     Long gradeId,
                                                                     Long uniformId,
                                                                     Long sizeId,
                                                                     String categoryKey,
                                                                     Integer lookbackDays,
                                                                     Integer horizonDays,
                                                                     Boolean includeAll) {
        boolean showAll = Boolean.TRUE.equals(includeAll);
        return forecasts(schoolId, gradeId, uniformId, sizeId, categoryKey, lookbackDays, horizonDays)
                .stream()
                .map(this::toSuggestion)
                .filter(row -> showAll || row.getSuggestedQuantity() > 0)
                .toList();
    }

    private SupplyForecastDto normalizeForecast(SupplyForecastDto row, int lookbackDays, int horizonDays) {
        long historicalQuantity = defaultLong(row.getHistoricalQuantity());
        long salesDayCount = defaultLong(row.getSalesDayCount());
        BigDecimal dailyAverage = BigDecimal.valueOf(historicalQuantity)
                .divide(BigDecimal.valueOf(lookbackDays), 4, RoundingMode.HALF_UP);
        long forecastDemand = dailyAverage
                .multiply(BigDecimal.valueOf(horizonDays))
                .setScale(0, RoundingMode.CEILING)
                .longValue();
        BigDecimal dataCompleteness = BigDecimal.valueOf(salesDayCount)
                .divide(BigDecimal.valueOf(lookbackDays), 4, RoundingMode.HALF_UP);
        row.setHistoricalQuantity(historicalQuantity);
        row.setSalesDayCount(salesDayCount);
        row.setLookbackDays(lookbackDays);
        row.setHorizonDays(horizonDays);
        row.setDailyAverage(dailyAverage);
        row.setForecastDemand(forecastDemand);
        row.setDataCompleteness(dataCompleteness);
        row.setReasonCodes(reasonCodes(historicalQuantity, dataCompleteness));
        row.setAvailableQuantity(defaultLong(row.getAvailableQuantity()));
        return row;
    }

    private ReplenishmentSuggestionDto toSuggestion(SupplyForecastDto forecast) {
        ReplenishmentSuggestionDto suggestion = new ReplenishmentSuggestionDto();
        suggestion.setSkuId(forecast.getSkuId());
        suggestion.setUniformId(forecast.getUniformId());
        suggestion.setSizeId(forecast.getSizeId());
        suggestion.setUniformName(forecast.getUniformName());
        suggestion.setSchoolName(forecast.getSchoolName());
        suggestion.setGradeName(forecast.getGradeName());
        suggestion.setCategoryKey(forecast.getCategoryKey());
        suggestion.setSizeName(forecast.getSizeName());
        suggestion.setAvailableQuantity(forecast.getAvailableQuantity());
        suggestion.setForecastDemand(forecast.getForecastDemand());
        suggestion.setSafetyStock(defaultLong(forecast.getSafetyStock()));
        suggestion.setReorderPoint(defaultLong(forecast.getReorderPoint()));
        suggestion.setLeadTimeDays(forecast.getLeadTimeDays() == null ? 0 : forecast.getLeadTimeDays());
        long leadTimeDemand = forecast.getDailyAverage()
                .multiply(BigDecimal.valueOf(suggestion.getLeadTimeDays()))
                .setScale(0, RoundingMode.CEILING)
                .longValue();
        long targetQuantity = defaultLong(forecast.getForecastDemand()) + suggestion.getSafetyStock() + leadTimeDemand;
        long suggestedQuantity = Math.max(0, targetQuantity - defaultLong(forecast.getAvailableQuantity()));
        suggestion.setSuggestedQuantity(suggestedQuantity);
        suggestion.setReasonText(suggestedQuantity > 0
                ? "预测需求高于当前可用库存，建议补货"
                : "当前可用库存覆盖预测需求");
        return suggestion;
    }

    private List<SupplyTrendPointDto> fillTrendGaps(String bucketType, LocalDate startDate, LocalDate endDate, List<SupplyTrendPointDto> rawRows) {
        Map<String, SupplyTrendPointDto> existing = new LinkedHashMap<>();
        for (SupplyTrendPointDto row : rawRows) {
            normalizeTrendPoint(row);
            existing.put(row.getBucket(), row);
        }
        List<SupplyTrendPointDto> result = new ArrayList<>();
        LocalDate cursor = bucketStart(bucketType, startDate);
        LocalDate endBucket = bucketStart(bucketType, endDate);
        while (!cursor.isAfter(endBucket)) {
            String key = bucketKey(bucketType, cursor);
            SupplyTrendPointDto point = existing.getOrDefault(key, emptyTrendPoint(key));
            normalizeTrendPoint(point);
            result.add(point);
            cursor = nextBucket(bucketType, cursor);
        }
        return result;
    }

    private SupplyTrendPointDto emptyTrendPoint(String bucket) {
        SupplyTrendPointDto point = new SupplyTrendPointDto();
        point.setBucket(bucket);
        point.setTotalQuantity(0L);
        point.setTotalAmount(BigDecimal.ZERO);
        point.setOrderCount(0L);
        return point;
    }

    private void normalizeTrendPoint(SupplyTrendPointDto point) {
        point.setTotalQuantity(defaultLong(point.getTotalQuantity()));
        point.setOrderCount(defaultLong(point.getOrderCount()));
        point.setTotalAmount(point.getTotalAmount() == null ? BigDecimal.ZERO : point.getTotalAmount());
    }

    private LocalDate bucketStart(String bucketType, LocalDate date) {
        return switch (bucketType) {
            case "week" -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "month" -> date.withDayOfMonth(1);
            default -> date;
        };
    }

    private LocalDate nextBucket(String bucketType, LocalDate date) {
        return switch (bucketType) {
            case "week" -> date.plusWeeks(1);
            case "month" -> date.plusMonths(1);
            default -> date.plusDays(1);
        };
    }

    private String bucketKey(String bucketType, LocalDate date) {
        return switch (bucketType) {
            case "month" -> date.toString().substring(0, 7);
            default -> date.toString();
        };
    }

    private List<String> reasonCodes(long historicalQuantity, BigDecimal dataCompleteness) {
        List<String> codes = new ArrayList<>();
        if (historicalQuantity == 0) {
            codes.add("NO_HISTORICAL_SALES");
        }
        if (dataCompleteness.compareTo(BigDecimal.valueOf(0.3)) < 0) {
            codes.add("LOW_DATA_COMPLETENESS");
        }
        if (codes.isEmpty()) {
            codes.add("HISTORICAL_AVERAGE");
        }
        return codes;
    }

    private void requireAdminAuthenticated() {
        if (authContext.getCurrentUserId() == null) {
            throw new BizException("登录状态无效");
        }
    }

    private DateRange requireDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BizException("请选择开始日期和结束日期");
        }
        if (endDate.isBefore(startDate)) {
            throw new BizException("结束日期不能早于开始日期");
        }
        return new DateRange(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
    }

    private String normalizeGroupBy(String groupBy) {
        String normalized = StringUtils.hasText(groupBy) ? groupBy.trim().toLowerCase() : "uniform";
        if (!List.of("school", "grade", "uniform", "category", "size").contains(normalized)) {
            throw new BizException("销售分析分组维度不合法");
        }
        return normalized;
    }

    private String normalizeBucketType(String bucketType) {
        String normalized = StringUtils.hasText(bucketType) ? bucketType.trim().toLowerCase() : "day";
        if (!List.of("day", "week", "month").contains(normalized)) {
            throw new BizException("趋势桶类型不合法");
        }
        return normalized;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private record DateRange(LocalDateTime startTime, LocalDateTime endTime) {
    }
}
