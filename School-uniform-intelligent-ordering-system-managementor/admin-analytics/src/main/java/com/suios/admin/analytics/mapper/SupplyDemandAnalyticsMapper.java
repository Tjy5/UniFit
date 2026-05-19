package com.suios.admin.analytics.mapper;

import com.suios.admin.analytics.dto.SupplyForecastDto;
import com.suios.admin.analytics.dto.SupplyLowStockAlertDto;
import com.suios.admin.analytics.dto.SupplySalesAnalyticsDto;
import com.suios.admin.analytics.dto.SupplyTrendPointDto;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SupplyDemandAnalyticsMapper {

    @Select("""
            <script>
            SELECT
                <choose>
                    <when test="groupBy == 'school'">
                        COALESCE(CAST(u.school_id AS CHAR), 'unknown') AS group_key,
                        COALESCE(sc.school_name, CONCAT('学校#', u.school_id), '未关联学校') AS group_name
                    </when>
                    <when test="groupBy == 'grade'">
                        COALESCE(CAST(u.grade_id AS CHAR), 'unknown') AS group_key,
                        COALESCE(g.grade_name, CONCAT('年级#', u.grade_id), '未关联年级') AS group_name
                    </when>
                    <when test="groupBy == 'uniform'">
                        CAST(oi.uniform_id AS CHAR) AS group_key,
                        COALESCE(u.name, oi.uniform_name_snapshot, CONCAT('校服#', oi.uniform_id)) AS group_name
                    </when>
                    <when test="groupBy == 'category'">
                        COALESCE(u.category_key, 'UNKNOWN') AS group_key,
                        COALESCE(u.category_key, '未分类') AS group_name
                    </when>
                    <otherwise>
                        CASE WHEN oi.size_id IS NULL THEN 'unknown' ELSE CAST(oi.size_id AS CHAR) END AS group_key,
                        CASE WHEN oi.size_id IS NULL THEN '未记录尺码' ELSE COALESCE(sz.size_name, CONCAT('尺码#', oi.size_id)) END AS group_name
                    </otherwise>
                </choose>,
                SUM(oi.quantity) AS total_quantity,
                COALESCE(SUM(oi.item_total_price), 0) AS total_amount,
                COUNT(DISTINCT o.id) AS order_count,
                MAX(oi.uniform_id) AS uniform_id,
                MAX(oi.size_id) AS size_id,
                MAX(COALESCE(u.name, oi.uniform_name_snapshot)) AS uniform_name,
                MAX(CASE WHEN oi.size_id IS NULL THEN '未记录尺码' ELSE COALESCE(sz.size_name, CONCAT('尺码#', oi.size_id)) END) AS size_name,
                MAX(sc.school_name) AS school_name,
                MAX(g.grade_name) AS grade_name,
                MAX(u.category_key) AS category_key
            FROM s_order_items oi
            JOIN s_orders o ON o.id = oi.order_id
            LEFT JOIN s_uniform u ON u.id = oi.uniform_id
            LEFT JOIN s_schools sc ON sc.school_id = u.school_id
            LEFT JOIN s_grades g ON g.grade_id = u.grade_id
            LEFT JOIN s_sizes sz ON sz.id = oi.size_id
            <where>
                o.order_date <![CDATA[>=]]> #{startTime}
                AND o.order_date <![CDATA[<=]]> #{endTime}
                AND o.payment_status = 'PAID'
                AND o.status IN (1, 2, 3)
                <if test="schoolId != null">AND u.school_id = #{schoolId}</if>
                <if test="gradeId != null">AND u.grade_id = #{gradeId}</if>
                <if test="uniformId != null">AND oi.uniform_id = #{uniformId}</if>
                <if test="sizeId != null">AND oi.size_id = #{sizeId}</if>
                <if test="categoryKey != null and categoryKey != ''">AND u.category_key = #{categoryKey}</if>
            </where>
            GROUP BY
                <choose>
                    <when test="groupBy == 'school'">u.school_id, sc.school_name</when>
                    <when test="groupBy == 'grade'">u.grade_id, g.grade_name</when>
                    <when test="groupBy == 'uniform'">oi.uniform_id, COALESCE(u.name, oi.uniform_name_snapshot)</when>
                    <when test="groupBy == 'category'">u.category_key</when>
                    <otherwise>oi.size_id, CASE WHEN oi.size_id IS NULL THEN '未记录尺码' ELSE COALESCE(sz.size_name, CONCAT('尺码#', oi.size_id)) END</otherwise>
                </choose>
            ORDER BY total_quantity DESC, total_amount DESC
            </script>
            """)
    List<SupplySalesAnalyticsDto> selectSalesAnalytics(@Param("groupBy") String groupBy,
                                                       @Param("schoolId") Long schoolId,
                                                       @Param("gradeId") Long gradeId,
                                                       @Param("uniformId") Long uniformId,
                                                       @Param("sizeId") Long sizeId,
                                                       @Param("categoryKey") String categoryKey,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);

    @Select("""
            <script>
            SELECT
                <choose>
                    <when test="bucketType == 'month'">DATE_FORMAT(o.order_date, '%Y-%m')</when>
                    <when test="bucketType == 'week'">DATE_FORMAT(DATE_SUB(DATE(o.order_date), INTERVAL WEEKDAY(o.order_date) DAY), '%Y-%m-%d')</when>
                    <otherwise>DATE_FORMAT(o.order_date, '%Y-%m-%d')</otherwise>
                </choose> AS bucket,
                SUM(oi.quantity) AS total_quantity,
                COALESCE(SUM(oi.item_total_price), 0) AS total_amount,
                COUNT(DISTINCT o.id) AS order_count
            FROM s_order_items oi
            JOIN s_orders o ON o.id = oi.order_id
            LEFT JOIN s_uniform u ON u.id = oi.uniform_id
            <where>
                o.order_date <![CDATA[>=]]> #{startTime}
                AND o.order_date <![CDATA[<=]]> #{endTime}
                AND o.payment_status = 'PAID'
                AND o.status IN (1, 2, 3)
                <if test="schoolId != null">AND u.school_id = #{schoolId}</if>
                <if test="gradeId != null">AND u.grade_id = #{gradeId}</if>
                <if test="uniformId != null">AND oi.uniform_id = #{uniformId}</if>
                <if test="sizeId != null">AND oi.size_id = #{sizeId}</if>
                <if test="categoryKey != null and categoryKey != ''">AND u.category_key = #{categoryKey}</if>
            </where>
            GROUP BY bucket
            ORDER BY bucket ASC
            </script>
            """)
    List<SupplyTrendPointDto> selectTrend(@Param("bucketType") String bucketType,
                                          @Param("schoolId") Long schoolId,
                                          @Param("gradeId") Long gradeId,
                                          @Param("uniformId") Long uniformId,
                                          @Param("sizeId") Long sizeId,
                                          @Param("categoryKey") String categoryKey,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    @Select("""
            <script>
            SELECT
                sku.sku_id,
                sku.uniform_id,
                sku.size_id,
                u.name AS uniform_name,
                sc.school_name,
                g.grade_name,
                u.category_key,
                sz.size_name,
                sku.stock_quantity,
                sku.reserved_quantity,
                (sku.stock_quantity - sku.reserved_quantity) AS available_quantity,
                sku.safety_stock,
                sku.reorder_point,
                GREATEST(sku.reorder_point - (sku.stock_quantity - sku.reserved_quantity), 0) AS shortage_quantity,
                CASE
                    WHEN (sku.stock_quantity - sku.reserved_quantity) &lt; 0 OR (sku.stock_quantity - sku.reserved_quantity) &lt; sku.safety_stock THEN 'CRITICAL'
                    WHEN (sku.stock_quantity - sku.reserved_quantity) &lt;= sku.reorder_point THEN 'WARNING'
                    ELSE 'OK'
                END AS severity
            FROM s_inventory_sku sku
            LEFT JOIN s_uniform u ON u.id = sku.uniform_id
            LEFT JOIN s_schools sc ON sc.school_id = u.school_id
            LEFT JOIN s_grades g ON g.grade_id = u.grade_id
            LEFT JOIN s_sizes sz ON sz.id = sku.size_id
            <where>
                sku.status = 'ACTIVE'
                AND (sku.stock_quantity - sku.reserved_quantity) &lt;= sku.reorder_point
                <if test="schoolId != null">AND u.school_id = #{schoolId}</if>
                <if test="gradeId != null">AND u.grade_id = #{gradeId}</if>
                <if test="uniformId != null">AND sku.uniform_id = #{uniformId}</if>
                <if test="sizeId != null">AND sku.size_id = #{sizeId}</if>
                <if test="categoryKey != null and categoryKey != ''">AND u.category_key = #{categoryKey}</if>
            </where>
            ORDER BY FIELD(severity, 'CRITICAL', 'WARNING', 'OK'), shortage_quantity DESC, available_quantity ASC
            </script>
            """)
    List<SupplyLowStockAlertDto> selectLowStockAlerts(@Param("schoolId") Long schoolId,
                                                      @Param("gradeId") Long gradeId,
                                                      @Param("uniformId") Long uniformId,
                                                      @Param("sizeId") Long sizeId,
                                                      @Param("categoryKey") String categoryKey);

    @Select("""
            <script>
            SELECT
                sku.sku_id,
                sku.uniform_id,
                sku.size_id,
                u.name AS uniform_name,
                sc.school_name,
                g.grade_name,
                u.category_key,
                sz.size_name,
                (sku.stock_quantity - sku.reserved_quantity) AS available_quantity,
                sku.safety_stock,
                sku.reorder_point,
                sku.lead_time_days,
                COALESCE(SUM(CASE WHEN o.id IS NOT NULL THEN oi.quantity ELSE 0 END), 0) AS historical_quantity,
                COUNT(DISTINCT CASE WHEN o.id IS NOT NULL THEN DATE(o.order_date) END) AS sales_day_count
            FROM s_inventory_sku sku
            LEFT JOIN s_uniform u ON u.id = sku.uniform_id
            LEFT JOIN s_schools sc ON sc.school_id = u.school_id
            LEFT JOIN s_grades g ON g.grade_id = u.grade_id
            LEFT JOIN s_sizes sz ON sz.id = sku.size_id
            LEFT JOIN s_order_items oi ON oi.uniform_id = sku.uniform_id AND oi.size_id = sku.size_id
            LEFT JOIN s_orders o ON o.id = oi.order_id
                AND o.order_date <![CDATA[>=]]> #{startTime}
                AND o.order_date <![CDATA[<=]]> #{endTime}
                AND o.payment_status = 'PAID'
                AND o.status IN (1, 2, 3)
            <where>
                sku.status = 'ACTIVE'
                <if test="schoolId != null">AND u.school_id = #{schoolId}</if>
                <if test="gradeId != null">AND u.grade_id = #{gradeId}</if>
                <if test="uniformId != null">AND sku.uniform_id = #{uniformId}</if>
                <if test="sizeId != null">AND sku.size_id = #{sizeId}</if>
                <if test="categoryKey != null and categoryKey != ''">AND u.category_key = #{categoryKey}</if>
            </where>
            GROUP BY sku.sku_id, sku.uniform_id, sku.size_id, u.name, sc.school_name, g.grade_name, u.category_key, sz.size_name,
                     sku.stock_quantity, sku.reserved_quantity, sku.safety_stock, sku.reorder_point, sku.lead_time_days
            ORDER BY historical_quantity DESC, sku.sku_id DESC
            </script>
            """)
    List<SupplyForecastDto> selectForecastBase(@Param("schoolId") Long schoolId,
                                               @Param("gradeId") Long gradeId,
                                               @Param("uniformId") Long uniformId,
                                               @Param("sizeId") Long sizeId,
                                               @Param("categoryKey") String categoryKey,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);
}
