package com.suios.admin.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.inventory.entity.InventorySku;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface InventorySkuMapper extends BaseMapper<InventorySku> {

    @Select("""
            <script>
            SELECT sku.*,
                   u.name AS uniform_name,
                   u.category_key AS category_key,
                   sc.school_name AS school_name,
                   g.grade_name AS grade_name,
                   sz.size_name AS size_name,
                   (sku.stock_quantity - sku.reserved_quantity) AS available_quantity
            FROM s_inventory_sku sku
            LEFT JOIN s_uniform u ON sku.uniform_id = u.id
            LEFT JOIN s_schools sc ON u.school_id = sc.school_id
            LEFT JOIN s_grades g ON u.grade_id = g.grade_id
            LEFT JOIN s_sizes sz ON sku.size_id = sz.id
            <where>
                <if test="schoolId != null">
                    AND u.school_id = #{schoolId}
                </if>
                <if test="gradeId != null">
                    AND u.grade_id = #{gradeId}
                </if>
                <if test="uniformId != null">
                    AND sku.uniform_id = #{uniformId}
                </if>
                <if test="sizeId != null">
                    AND sku.size_id = #{sizeId}
                </if>
                <if test="categoryKey != null and categoryKey != ''">
                    AND u.category_key = #{categoryKey}
                </if>
                <if test="status != null and status != ''">
                    AND sku.status = #{status}
                </if>
            </where>
            ORDER BY sku.sku_id DESC
            </script>
            """)
    IPage<InventorySku> selectPageWithContext(Page<InventorySku> page,
                                              @Param("schoolId") Long schoolId,
                                              @Param("gradeId") Long gradeId,
                                              @Param("uniformId") Long uniformId,
                                              @Param("sizeId") Long sizeId,
                                              @Param("categoryKey") String categoryKey,
                                              @Param("status") String status);

    @Select("""
            <script>
            SELECT sku.*,
                   u.name AS uniform_name,
                   u.category_key AS category_key,
                   sc.school_name AS school_name,
                   g.grade_name AS grade_name,
                   sz.size_name AS size_name,
                   (sku.stock_quantity - sku.reserved_quantity) AS available_quantity
            FROM s_inventory_sku sku
            LEFT JOIN s_uniform u ON sku.uniform_id = u.id
            LEFT JOIN s_schools sc ON u.school_id = sc.school_id
            LEFT JOIN s_grades g ON u.grade_id = g.grade_id
            LEFT JOIN s_sizes sz ON sku.size_id = sz.id
            <where>
                <if test="schoolId != null">
                    AND u.school_id = #{schoolId}
                </if>
                <if test="gradeId != null">
                    AND u.grade_id = #{gradeId}
                </if>
                <if test="uniformId != null">
                    AND sku.uniform_id = #{uniformId}
                </if>
                <if test="sizeId != null">
                    AND sku.size_id = #{sizeId}
                </if>
                <if test="categoryKey != null and categoryKey != ''">
                    AND u.category_key = #{categoryKey}
                </if>
                <if test="status != null and status != ''">
                    AND sku.status = #{status}
                </if>
            </where>
            ORDER BY sku.sku_id DESC
            </script>
            """)
    List<InventorySku> selectListWithContext(@Param("schoolId") Long schoolId,
                                             @Param("gradeId") Long gradeId,
                                             @Param("uniformId") Long uniformId,
                                             @Param("sizeId") Long sizeId,
                                             @Param("categoryKey") String categoryKey,
                                             @Param("status") String status);

    @Select("""
            SELECT sku.*,
                   u.name AS uniform_name,
                   u.category_key AS category_key,
                   sc.school_name AS school_name,
                   g.grade_name AS grade_name,
                   sz.size_name AS size_name,
                   (sku.stock_quantity - sku.reserved_quantity) AS available_quantity
            FROM s_inventory_sku sku
            LEFT JOIN s_uniform u ON sku.uniform_id = u.id
            LEFT JOIN s_schools sc ON u.school_id = sc.school_id
            LEFT JOIN s_grades g ON u.grade_id = g.grade_id
            LEFT JOIN s_sizes sz ON sku.size_id = sz.id
            WHERE sku.sku_id = #{skuId}
            """)
    InventorySku selectByIdWithContext(@Param("skuId") Long skuId);

    @Select("""
            SELECT *
            FROM s_inventory_sku
            WHERE uniform_id = #{uniformId}
              AND size_id = #{sizeId}
              AND status = 'ACTIVE'
            ORDER BY sku_id DESC
            LIMIT 1
            """)
    InventorySku selectActiveByUniformAndSize(@Param("uniformId") Long uniformId, @Param("sizeId") Long sizeId);

    @Select("""
            SELECT *
            FROM s_inventory_sku
            WHERE sku_id = #{skuId}
            FOR UPDATE
            """)
    InventorySku selectByIdForUpdate(@Param("skuId") Long skuId);
}
