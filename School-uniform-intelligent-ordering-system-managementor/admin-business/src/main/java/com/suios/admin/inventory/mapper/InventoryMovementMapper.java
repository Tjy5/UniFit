package com.suios.admin.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suios.admin.inventory.entity.InventoryMovement;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface InventoryMovementMapper extends BaseMapper<InventoryMovement> {

    @Select("""
            <script>
            SELECT *
            FROM s_inventory_movements
            <where>
                <if test="skuId != null">
                    AND sku_id = #{skuId}
                </if>
                <if test="movementType != null and movementType != ''">
                    AND movement_type = #{movementType}
                </if>
                <if test="relatedOrderId != null">
                    AND related_order_id = #{relatedOrderId}
                </if>
                <if test="startTime != null">
                    AND create_time &gt;= #{startTime}
                </if>
                <if test="endTime != null">
                    AND create_time &lt;= #{endTime}
                </if>
            </where>
            ORDER BY create_time DESC, movement_id DESC
            </script>
            """)
    List<InventoryMovement> selectByFilters(@Param("skuId") Long skuId,
                                            @Param("movementType") String movementType,
                                            @Param("relatedOrderId") Long relatedOrderId,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT COUNT(1)
            FROM s_inventory_movements
            WHERE related_order_id = #{orderId}
              AND movement_type = #{movementType}
            """)
    long countByOrderAndType(@Param("orderId") Long orderId, @Param("movementType") String movementType);

    @Select("""
            SELECT *
            FROM s_inventory_movements
            WHERE related_order_id = #{orderId}
              AND movement_type = #{movementType}
            ORDER BY movement_id ASC
            """)
    List<InventoryMovement> selectByOrderAndType(@Param("orderId") Long orderId, @Param("movementType") String movementType);
}
