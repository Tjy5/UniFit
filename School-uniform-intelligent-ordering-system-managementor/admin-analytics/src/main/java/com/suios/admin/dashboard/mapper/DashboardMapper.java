package com.suios.admin.dashboard.mapper;

import com.suios.admin.dashboard.dto.DashboardRecentOrder;
import com.suios.admin.dashboard.dto.DashboardTrendPoint;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface DashboardMapper {

    @Select("SELECT COUNT(*) FROM s_uniform")
    Long countUniformTotal();

    @Select("SELECT COUNT(*) FROM s_orders WHERE DATE(order_date) = CURDATE()")
    Long countTodayOrders();

    @Select("SELECT COUNT(*) FROM s_reviews WHERE status = 0")
    Long countPendingReviews();

    @Select("SELECT COUNT(*) FROM s_user_activity_logs WHERE DATE(log_time) = CURDATE()")
    Long countTodayVisits();

    @Select("""
            SELECT DATE_FORMAT(order_date, '%Y-%m-%d') AS day,
                   COUNT(*) AS order_count
            FROM s_orders
            WHERE order_date >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
            GROUP BY DATE_FORMAT(order_date, '%Y-%m-%d')
            ORDER BY day ASC
            """)
    List<DashboardTrendPoint> selectOrderTrend();

    @Select("""
            SELECT o.id,
                   ua.user_account AS user_account,
                   o.total_price,
                   o.status,
                   o.order_date
            FROM s_orders o
            LEFT JOIN user_account ua ON o.user_id = ua.user_id
            ORDER BY o.order_date DESC, o.id DESC
            LIMIT #{limit}
            """)
    List<DashboardRecentOrder> selectRecentOrders(@Param("limit") int limit);
}
