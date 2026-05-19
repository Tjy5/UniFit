package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.SOrders;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface SOrdersMapper {
    // 根据用户ID查询订单 (will use new resultMap)
    List<SOrders> selectByUserId(Long userId);

    // 根据订单ID查询订单 (will use new resultMap)
    SOrders selectById(Long id); // NEW

    // 新建订单 (will be modified for new fields and to get generated ID)
    int insertOrder(SOrders order);

    // 修改订单（动态更新）
    int updateOrder(SOrders order);

    // (Optional) Update just the total price after items are inserted
    int updateOrderTotalPrice(SOrders order);
}
