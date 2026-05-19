package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.SOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SOrderItemMapper {
    int insert(SOrderItem item);
    int insertBatch(@Param("list") List<SOrderItem> items);
    List<SOrderItem> selectByOrderId(Long orderId);

    // ⭐ 新增方法: 根据订单项ID查询
    SOrderItem selectOrderItemById(@Param("orderItemId") Long orderItemId);

    // ⭐ 新增方法: 更新订单项的评价ID
    int updateOrderItemReviewId(@Param("orderItemId") Long orderItemId, @Param("reviewId") Long reviewId);
}
