package com.suios.admin.dashboard.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponse {

    private Long uniformTotal;

    private Long todayOrders;

    private Long pendingReviews;

    private Long todayVisits;

    private List<DashboardTrendPoint> orderTrend;

    private List<DashboardRecentOrder> recentOrders;
}
