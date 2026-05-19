package com.suios.admin.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.analytics.dto.BehaviorFunnelStageCountDto;
import com.suios.admin.analytics.dto.DropOffDistributionRowDto;
import com.suios.admin.analytics.dto.RecommendationAdoptionStatsDto;
import com.suios.admin.analytics.dto.RecommendationEntryComparisonRowDto;
import com.suios.admin.analytics.dto.StalledCartRowDto;
import com.suios.admin.analytics.dto.StalledCartsResponse;
import com.suios.admin.analytics.mapper.BehaviorFunnelMapper;
import com.suios.admin.common.security.AuthContext;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BehaviorFunnelAnalyticsIntegrationTest {

    private static final LocalDate ANALYSIS_END_DATE = LocalDate.of(2026, 5, 18);
    private static final LocalDate ANALYSIS_START_DATE = ANALYSIS_END_DATE.minusDays(7);
    private static final LocalDateTime REFERENCE_NOW = LocalDateTime.of(2026, 5, 18, 10, 0);
    private static final LocalDateTime BASE_TIME = REFERENCE_NOW.minusDays(1);

    private AuthContext authContext;
    private SeededBehaviorFunnelMapper behaviorFunnelMapper;
    private BehaviorFunnelService service;

    @BeforeEach
    void setUp() {
        authContext = mock(AuthContext.class);
        when(authContext.getCurrentUserId()).thenReturn(1L);
        behaviorFunnelMapper = new SeededBehaviorFunnelMapper();
        service = new BehaviorFunnelService(behaviorFunnelMapper, authContext);
        ReflectionTestUtils.setField(service, "maxPageSize", 50);
        ReflectionTestUtils.setField(service, "maxFunnelRangeDays", 90);
        ReflectionTestUtils.setField(service, "sessionInactivityGapMinutes", 30);
        ReflectionTestUtils.setField(service, "stalledCartThresholdHours", 48);
    }

    @Test
    void shouldAggregateBehaviorAnalyticsEndToEndFromSeededRecords() {
        var funnel = service.getBehaviorFunnel(null, null, null, null, ANALYSIS_START_DATE, ANALYSIS_END_DATE);
        assertEquals(6L, funnel.getTotalSessions());
        assertEquals(6L, funnel.getStages().get(0).getSessionsReached());
        assertEquals(BigDecimal.valueOf(0.3333).setScale(4), funnel.getStages().get(0).getConversionRate());
        assertEquals(BigDecimal.valueOf(1.0000).setScale(4), funnel.getStages().get(1).getConversionRate());
        assertEquals(BigDecimal.valueOf(1.0000).setScale(4), funnel.getStages().get(2).getConversionRate());
        assertEquals(BigDecimal.valueOf(0.5000).setScale(4), funnel.getStages().get(3).getConversionRate());

        var entryComparison = service.getEntryComparison(null, null, null, null, ANALYSIS_START_DATE, ANALYSIS_END_DATE);
        assertEquals(3, entryComparison.getRows().size());
        assertEquals("mall-home-dialog", entryComparison.getRows().get(0).getRequestSource());
        assertEquals("unknown", entryComparison.getRows().get(1).getRequestSource());
        assertEquals("wishlist", entryComparison.getRows().get(2).getRequestSource());
        assertEquals(BigDecimal.valueOf(1.0000).setScale(4), entryComparison.getRows().get(0).getOrderConversionRate());
        assertEquals(BigDecimal.valueOf(0.0000).setScale(4), entryComparison.getRows().get(2).getOrderConversionRate());

        RecommendationAdoptionStatsDto adoption = service.getRecommendationAdoption(null, null, null, null, ANALYSIS_START_DATE, ANALYSIS_END_DATE);
        assertEquals(2L, adoption.getLinkedOrderCount());
        assertEquals(1L, adoption.getAdoptedCount());
        assertEquals(1L, adoption.getSizeChangeCount());
        assertEquals(1L, adoption.getWithFeedbackCount());
        assertEquals(1L, adoption.getWithoutFeedbackCount());

        var dropOff = service.getDropOffDistribution(null, null, null, null, ANALYSIS_START_DATE, ANALYSIS_END_DATE, 20);
        assertEquals(4, dropOff.getRows().size());
        assertEquals(5L, dropOff.getTotalSessions());
        assertEquals("VIEW_ACTIVE_UNIFORMS", dropOff.getRows().get(0).getLastActionType());
        assertEquals("ADD_TO_CART_SUCCESS", dropOff.getRows().get(1).getLastActionType());
        assertEquals("VIEW_ACTIVE_STYLE_GUIDES", dropOff.getRows().get(2).getLastActionType());
        assertEquals("VIEW_UNIFORM_DETAIL", dropOff.getRows().get(3).getLastActionType());

        StalledCartsResponse stalled = service.getStalledCarts(null, null, null, ANALYSIS_START_DATE, ANALYSIS_END_DATE, 20, 0);
        assertEquals(1L, stalled.getTotal());
        assertEquals(1, stalled.getRecords().size());
        assertEquals("lihua", stalled.getRecords().get(0).getUserAccount());
        assertTrue(stalled.getRecords().get(0).getHoursSinceAdd() >= 48L);
        assertFalse(stalled.getRecords().get(0).getLastActivityAt().isAfter(REFERENCE_NOW));
    }

    @Test
    void shouldSplitSessionsWhenInactivityGapExceedsThreshold() {
        List<BehaviorFunnelStageCountDto> rows = behaviorFunnelMapper.selectFunnelStageCounts(
                null,
                null,
                null,
                null,
                ANALYSIS_START_DATE.atStartOfDay(),
                ANALYSIS_END_DATE.atTime(23, 59, 59),
                30
        );

        long browseCount = rows.stream()
                .filter(row -> "browse".equals(row.getStage()))
                .mapToLong(BehaviorFunnelStageCountDto::getSessionsReached)
                .findFirst()
                .orElse(0L);
        assertEquals(6L, browseCount);
    }

    private final class SeededBehaviorFunnelMapper implements BehaviorFunnelMapper {

        private final List<ActivitySeed> activities = List.of(
                new ActivitySeed(1L, 1L, "s1", "VIEW_ACTIVE_UNIFORMS", "UNIFORM", 88L, BASE_TIME, null),
                new ActivitySeed(2L, 1L, "s1", "VIEW_UNIFORM_DETAIL", "UNIFORM", 88L, BASE_TIME.plusMinutes(2), null),
                new ActivitySeed(3L, 1L, "s1", "SIZE_RECOMMENDATION_SUCCESS", "SIZE", 5L, BASE_TIME.plusMinutes(3), "recommendationLogId:100"),
                new ActivitySeed(4L, 1L, "s1", "ADD_TO_CART_SUCCESS", "CART", 88L, BASE_TIME.plusMinutes(4), "recommendationLogId:100"),
                new ActivitySeed(5L, 1L, "s1", "PLACE_ORDER_SUCCESS", "UNIFORM", 88L, BASE_TIME.plusMinutes(5), null),
                new ActivitySeed(6L, 2L, "s2", "VIEW_ACTIVE_UNIFORMS", "UNIFORM", 88L, BASE_TIME.plusHours(1), null),
                new ActivitySeed(7L, 2L, "s2", "VIEW_UNIFORM_DETAIL", "UNIFORM", 88L, BASE_TIME.plusHours(1).plusMinutes(1), null),
                new ActivitySeed(8L, 3L, "s3", "VIEW_ACTIVE_UNIFORMS", "UNIFORM", 99L, BASE_TIME.plusHours(2), null),
                new ActivitySeed(9L, 3L, "s3", "SIZE_RECOMMENDATION_SUCCESS", "SIZE", 6L, BASE_TIME.plusHours(2).plusMinutes(1), "recommendationLogId:101"),
                new ActivitySeed(10L, 3L, "s3", "ADD_TO_CART_SUCCESS", "CART", 99L, BASE_TIME.plusHours(2).plusMinutes(2), "recommendationLogId:101"),
                new ActivitySeed(11L, 4L, "s4", "VIEW_ACTIVE_STYLE_GUIDES", "STYLE_GUIDE", 0L, BASE_TIME.plusHours(3), null),
                new ActivitySeed(12L, 5L, "gap-session", "VIEW_ACTIVE_UNIFORMS", "UNIFORM", 88L, BASE_TIME.plusHours(4), null),
                new ActivitySeed(13L, 5L, "gap-session", "VIEW_ACTIVE_UNIFORMS", "UNIFORM", 88L, BASE_TIME.plusHours(5).plusMinutes(5), null)
        );

        private final List<RecommendationSeed> recommendations = List.of(
                new RecommendationSeed(100L, 1L, 12L, 3L, 88L, "mall-home-dialog", 5L, 700L, BASE_TIME.plusMinutes(3)),
                new RecommendationSeed(101L, 3L, 12L, 3L, 99L, "wishlist", 6L, null, BASE_TIME.plusHours(2).plusMinutes(1)),
                new RecommendationSeed(102L, 4L, 12L, 3L, 88L, null, 5L, 701L, BASE_TIME.plusHours(3).plusMinutes(1))
        );

        private final List<OrderSeed> orderItems = List.of(
                new OrderSeed(700L, 1L, 88L, 5L, BASE_TIME.plusMinutes(5)),
                new OrderSeed(701L, 4L, 88L, 6L, BASE_TIME.plusHours(3).plusMinutes(5))
        );

        private final Set<Long> feedbackOrderItemIds = Set.of(700L);

        private final List<CartSeed> carts = List.of(
                new CartSeed(900L, 3L, 99L, 6L, 1, REFERENCE_NOW.minusHours(72), 101L),
                new CartSeed(901L, 1L, 88L, 5L, 1, REFERENCE_NOW.minusHours(72), 100L)
        );

        @Override
        public List<BehaviorFunnelStageCountDto> selectFunnelStageCounts(Long schoolId, Long gradeId, Long uniformId, String source, LocalDateTime startTime, LocalDateTime endTime, Integer gapMinutes) {
            Map<String, Long> counts = new LinkedHashMap<>();
            for (List<ActivitySeed> session : sessionize(startTime, endTime, gapMinutes)) {
                for (String stage : stagesForSession(session)) {
                    counts.merge(stage, 1L, Long::sum);
                }
            }
            return counts.entrySet().stream()
                    .map(entry -> {
                        BehaviorFunnelStageCountDto dto = new BehaviorFunnelStageCountDto();
                        dto.setStage(entry.getKey());
                        dto.setSessionsReached(entry.getValue());
                        return dto;
                    })
                    .toList();
        }

        @Override
        public List<RecommendationEntryComparisonRowDto> selectEntryComparison(Long schoolId, Long gradeId, Long uniformId, String source, LocalDateTime startTime, LocalDateTime endTime) {
            Map<String, List<RecommendationSeed>> grouped = recommendations.stream()
                    .filter(row -> within(row.createTime, startTime, endTime))
                    .collect(Collectors.groupingBy(row -> normalizeSource(row.requestSource), LinkedHashMap::new, Collectors.toList()));
            return grouped.entrySet().stream()
                    .map(entry -> {
                        long exposure = entry.getValue().size();
                        long linkedCart = entry.getValue().stream().filter(row -> hasLinkedCart(row.logId)).count();
                        long linkedOrder = entry.getValue().stream().filter(row -> row.orderItemId != null).count();
                        long adopted = entry.getValue().stream().filter(row -> row.orderItemId != null && Objects.equals(orderSize(row.orderItemId), row.recommendedSizeId)).count();
                        RecommendationEntryComparisonRowDto dto = new RecommendationEntryComparisonRowDto();
                        dto.setRequestSource(entry.getKey());
                        dto.setExposureCount(exposure);
                        dto.setLinkedCartCount(linkedCart);
                        dto.setLinkedOrderCount(linkedOrder);
                        dto.setAdoptedCount(adopted);
                        return dto;
                    })
                    .toList();
        }

        @Override
        public RecommendationAdoptionStatsDto selectRecommendationAdoption(Long schoolId, Long gradeId, Long uniformId, String source, LocalDateTime startTime, LocalDateTime endTime) {
            List<RecommendationSeed> rows = recommendations.stream()
                    .filter(row -> within(row.createTime, startTime, endTime))
                    .filter(row -> source == null || Objects.equals(normalizeSource(row.requestSource), source))
                    .toList();
            RecommendationAdoptionStatsDto dto = new RecommendationAdoptionStatsDto();
            dto.setLinkedOrderCount(rows.stream().filter(row -> row.orderItemId != null).count());
            dto.setAdoptedCount(rows.stream().filter(row -> row.orderItemId != null && Objects.equals(orderSize(row.orderItemId), row.recommendedSizeId)).count());
            dto.setWithFeedbackCount(rows.stream().filter(row -> row.orderItemId != null && feedbackOrderItemIds.contains(row.orderItemId)).count());
            return dto;
        }

        @Override
        public List<DropOffDistributionRowDto> selectDropOffDistribution(Long schoolId, Long gradeId, Long uniformId, String source, LocalDateTime startTime, LocalDateTime endTime, Integer gapMinutes) {
            return sessionize(startTime, endTime, gapMinutes).stream()
                    .filter(session -> session.stream().noneMatch(event -> "PLACE_ORDER_SUCCESS".equals(event.actionType)))
                    .collect(Collectors.groupingBy(session -> session.get(session.size() - 1).actionType, LinkedHashMap::new, Collectors.counting()))
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        DropOffDistributionRowDto dto = new DropOffDistributionRowDto();
                        dto.setLastActionType(entry.getKey());
                        dto.setSessionCount(entry.getValue());
                        return dto;
                    })
                    .toList();
        }

        @Override
        public IPage<StalledCartRowDto> selectStalledCarts(Page<StalledCartRowDto> page, Long schoolId, Long gradeId, Long uniformId, LocalDateTime startTime, LocalDateTime endTime, Integer thresholdHours) {
            List<StalledCartRowDto> rows = carts.stream()
                    .filter(row -> within(row.addedAt, startTime, endTime))
                    .filter(row -> Duration.between(row.addedAt, REFERENCE_NOW).toHours() >= thresholdHours)
                    .filter(row -> orderItems.stream().noneMatch(order -> Objects.equals(order.userId, row.userId) && Objects.equals(order.uniformId, row.uniformId) && !order.orderDate.isBefore(row.addedAt)))
                    .map(this::toRow)
                    .sorted(Comparator.comparing(StalledCartRowDto::getAddedAt))
                    .toList();

            long startIndex = Math.max(0L, (page.getCurrent() - 1) * page.getSize());
            long endIndex = Math.min(rows.size(), startIndex + page.getSize());
            List<StalledCartRowDto> records = startIndex >= rows.size() ? List.of() : rows.subList((int) startIndex, (int) endIndex);
            Page<StalledCartRowDto> result = new Page<>(page.getCurrent(), page.getSize());
            result.setTotal(rows.size());
            result.setRecords(records);
            return result;
        }

        private List<List<ActivitySeed>> sessionize(LocalDateTime startTime, LocalDateTime endTime, Integer gapMinutes) {
            Map<String, List<ActivitySeed>> grouped = activities.stream()
                    .filter(row -> within(row.logTime, startTime, endTime))
                    .collect(Collectors.groupingBy(this::activityKey, LinkedHashMap::new, Collectors.toList()));
            List<List<ActivitySeed>> sessions = new ArrayList<>();
            for (List<ActivitySeed> bucket : grouped.values()) {
                List<ActivitySeed> sorted = bucket.stream()
                        .sorted(Comparator.comparing(ActivitySeed::logTime).thenComparing(ActivitySeed::logId))
                        .toList();
                List<ActivitySeed> current = new ArrayList<>();
                ActivitySeed previous = null;
                for (ActivitySeed row : sorted) {
                    if (previous != null && Duration.between(previous.logTime, row.logTime).toMinutes() > gapMinutes) {
                        sessions.add(List.copyOf(current));
                        current.clear();
                    }
                    current.add(row);
                    previous = row;
                }
                if (!current.isEmpty()) {
                    sessions.add(List.copyOf(current));
                }
            }
            return sessions;
        }

        private List<String> stagesForSession(List<ActivitySeed> session) {
            List<String> stages = new ArrayList<>();
            for (ActivitySeed row : session) {
                String stage = switch (row.actionType) {
                    case "VIEW_ACTIVE_UNIFORMS", "VIEW_ACTIVE_STYLE_GUIDES" -> "browse";
                    case "VIEW_UNIFORM_DETAIL", "VIEW_STYLE_GUIDE_DETAIL" -> "detail";
                    case "SIZE_RECOMMENDATION_SUCCESS" -> "recommend";
                    case "ADD_TO_CART_SUCCESS" -> "cart";
                    case "PLACE_ORDER_SUCCESS" -> "order";
                    default -> null;
                };
                if (stage != null && !stages.contains(stage)) {
                    stages.add(stage);
                }
            }
            return stages;
        }

        private boolean hasLinkedCart(Long recommendationLogId) {
            return activities.stream()
                    .filter(row -> "ADD_TO_CART_SUCCESS".equals(row.actionType))
                    .anyMatch(row -> row.logDetail != null && row.logDetail.contains("recommendationLogId:" + recommendationLogId));
        }

        private Long orderSize(Long orderItemId) {
            return orderItems.stream()
                    .filter(row -> Objects.equals(row.orderItemId, orderItemId))
                    .map(row -> row.sizeId)
                    .findFirst()
                    .orElse(null);
        }

        private StalledCartRowDto toRow(CartSeed seed) {
            StalledCartRowDto row = new StalledCartRowDto();
            row.setUserId(seed.userId);
            row.setUserAccount(seed.userId == 3L ? "lihua" : "zhangsan");
            row.setUniformId(seed.uniformId);
            row.setUniformName(seed.uniformId == 99L ? "冬季外套" : "夏季短袖");
            row.setSizeId(seed.sizeId);
            row.setSizeName(seed.sizeId == 6L ? "170" : "165");
            row.setQuantity(seed.quantity);
            row.setAddedAt(seed.addedAt);
            row.setHoursSinceAdd(Duration.between(seed.addedAt, REFERENCE_NOW).toHours());
            row.setRecommendationLogId(seed.recommendationLogId);
            row.setLastActivityAt(REFERENCE_NOW.minusHours(2));
            return row;
        }

        private boolean within(LocalDateTime value, LocalDateTime startTime, LocalDateTime endTime) {
            return !value.isBefore(startTime) && !value.isAfter(endTime);
        }

        private String activityKey(ActivitySeed row) {
            if (row.sessionId != null && !row.sessionId.isBlank()) {
                return "session:" + row.sessionId;
            }
            if (row.userId != null) {
                return "user:" + row.userId;
            }
            return "anon:" + row.logId;
        }

        private String normalizeSource(String source) {
            if (source == null || source.isBlank()) {
                return "unknown";
            }
            String canonical = source.trim().toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
            return Set.of("mall-home-dialog", "wishlist", "order-feedback", "unknown").contains(canonical) ? canonical : "unknown";
        }

        private record ActivitySeed(Long logId,
                                    Long userId,
                                    String sessionId,
                                    String actionType,
                                    String targetType,
                                    Long targetId,
                                    LocalDateTime logTime,
                                    String logDetail) {
        }

        private record RecommendationSeed(Long logId,
                                          Long userId,
                                          Long schoolId,
                                          Long gradeId,
                                          Long uniformId,
                                          String requestSource,
                                          Long recommendedSizeId,
                                          Long orderItemId,
                                          LocalDateTime createTime) {
        }

        private record OrderSeed(Long orderItemId,
                                 Long userId,
                                 Long uniformId,
                                 Long sizeId,
                                 LocalDateTime orderDate) {
        }

        private record CartSeed(Long cartItemId,
                                Long userId,
                                Long uniformId,
                                Long sizeId,
                                Integer quantity,
                                LocalDateTime addedAt,
                                Long recommendationLogId) {
        }
    }
}
