package com.suios.admin.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suios.admin.order.entity.SOrderItem;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SOrderItemMapper extends BaseMapper<SOrderItem> {

    @Select("""
            SELECT *
            FROM s_order_items
            WHERE order_id = #{orderId}
            ORDER BY order_item_id DESC
            """)
    List<SOrderItem> selectByOrderId(@Param("orderId") Long orderId);
}
