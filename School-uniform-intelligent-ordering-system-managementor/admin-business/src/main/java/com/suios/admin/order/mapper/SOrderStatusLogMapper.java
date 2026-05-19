package com.suios.admin.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suios.admin.order.entity.SOrderStatusLog;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SOrderStatusLogMapper extends BaseMapper<SOrderStatusLog> {

    @Select("""
            SELECT *
            FROM s_order_status_logs
            WHERE order_id = #{orderId}
            ORDER BY create_time ASC, id ASC
            """)
    List<SOrderStatusLog> selectByOrderId(@Param("orderId") Long orderId);

    @Select("""
            SELECT *
            FROM s_order_status_logs
            WHERE order_id = #{orderId}
              AND event_type = #{eventType}
            ORDER BY create_time DESC, id DESC
            LIMIT 1
            """)
    SOrderStatusLog selectLatestByOrderIdAndEventType(@Param("orderId") Long orderId,
                                                      @Param("eventType") String eventType);

    @Select("""
            SELECT COUNT(*)
            FROM s_order_status_logs
            WHERE order_id = #{orderId}
            """)
    int countByOrderId(@Param("orderId") Long orderId);
}
