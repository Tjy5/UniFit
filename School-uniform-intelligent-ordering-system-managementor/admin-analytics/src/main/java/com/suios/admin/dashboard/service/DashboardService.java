package com.suios.admin.dashboard.service;

import com.suios.admin.dashboard.dto.DashboardRecentOrder;
import com.suios.admin.dashboard.dto.DashboardStatsResponse;
import com.suios.admin.dashboard.dto.DashboardTrendPoint;
import com.suios.admin.dashboard.mapper.DashboardMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private static final int TREND_DAYS = 7;
    private static final int RECENT_ORDER_LIMIT = 5;

    private final DashboardMapper dashboardMapper;

    public DashboardService(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    public DashboardStatsResponse getStats() {
        return DashboardStatsResponse.builder()
                .uniformTotal(defaultZero(dashboardMapper.countUniformTotal()))
                .todayOrders(defaultZero(dashboardMapper.countTodayOrders()))
                .pendingReviews(defaultZero(dashboardMapper.countPendingReviews()))
                .todayVisits(defaultZero(dashboardMapper.countTodayVisits()))
                .orderTrend(fillTrendGaps(dashboardMapper.selectOrderTrend()))
                .recentOrders(defaultOrders(dashboardMapper.selectRecentOrders(RECENT_ORDER_LIMIT)))
                .build();
    }

    private List<DashboardTrendPoint> fillTrendGaps(List<DashboardTrendPoint> rawTrend) {
        Map<String, DashboardTrendPoint> trendMap = rawTrend.stream()
                .collect(Collectors.toMap(DashboardTrendPoint::getDay, Function.identity(), (left, right) -> right));

        List<DashboardTrendPoint> normalized = new ArrayList<>();
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            String day = LocalDate.now().minusDays(i).toString();
            DashboardTrendPoint point = trendMap.get(day);
            normalized.add(new DashboardTrendPoint(day, point == null ? 0L : defaultZero(point.getOrderCount())));
        }
        return normalized;
    }

    private Long defaultZero(Long value) {
        return value == null ? 0L : value;
    }

    private List<DashboardRecentOrder> defaultOrders(List<DashboardRecentOrder> orders) {
        return orders == null ? List.of() : orders;
    }
}
