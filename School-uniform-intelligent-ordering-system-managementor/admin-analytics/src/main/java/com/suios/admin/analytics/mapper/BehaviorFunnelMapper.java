package com.suios.admin.analytics.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.analytics.dto.BehaviorFunnelStageCountDto;
import com.suios.admin.analytics.dto.DropOffDistributionRowDto;
import com.suios.admin.analytics.dto.RecommendationAdoptionStatsDto;
import com.suios.admin.analytics.dto.RecommendationEntryComparisonRowDto;
import com.suios.admin.analytics.dto.StalledCartRowDto;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BehaviorFunnelMapper {

    @Select("""
            <script>
            WITH base_events AS (
                SELECT
                    l.log_id,
                    l.user_id,
                    l.session_id,
                    l.action_type,
                    l.target_type,
                    l.target_id,
                    l.ip_address,
                    l.user_agent,
                    l.log_time,
                    CASE
                        WHEN l.session_id IS NOT NULL AND l.session_id != '' THEN CONCAT('session:', l.session_id)
                        WHEN l.user_id IS NOT NULL THEN CONCAT('user:', l.user_id)
                        ELSE CONCAT('anon:', COALESCE(l.ip_address, ''), ':', MD5(COALESCE(l.user_agent, '')))
                    END AS activity_key
                FROM s_user_activity_logs l
                WHERE l.log_time <![CDATA[>=]]> #{startTime}
                  AND l.log_time <![CDATA[<=]]> #{endTime}
                  AND l.action_type IN (
                      'VIEW_ACTIVE_UNIFORMS',
                      'VIEW_ACTIVE_STYLE_GUIDES',
                      'VIEW_UNIFORM_DETAIL',
                      'VIEW_STYLE_GUIDE_DETAIL',
                      'SIZE_RECOMMENDATION_SUCCESS',
                      'ADD_TO_CART_SUCCESS',
                      'PLACE_ORDER_SUCCESS'
                  )
            ),
            ordered_events AS (
                SELECT
                    base_events.*,
                    LAG(log_time) OVER (PARTITION BY activity_key ORDER BY log_time, log_id) AS prev_log_time
                FROM base_events
            ),
            marked_events AS (
                SELECT
                    ordered_events.*,
                    CASE
                        WHEN prev_log_time IS NULL THEN 1
                        WHEN TIMESTAMPDIFF(MINUTE, prev_log_time, log_time) > #{gapMinutes} THEN 1
                        ELSE 0
                    END AS new_session_flag
                FROM ordered_events
            ),
            events AS (
                SELECT
                    marked_events.*,
                    CONCAT(activity_key, '#',
                           SUM(new_session_flag) OVER (PARTITION BY activity_key ORDER BY log_time, log_id)) AS session_key
                FROM marked_events
            ),
            session_meta AS (
                SELECT
                    session_key,
                    MAX(user_id) AS user_id,
                    MIN(log_time) AS session_start,
                    MAX(log_time) AS session_end
                FROM events
                GROUP BY session_key
            ),
            staged AS (
                SELECT
                    e.session_key,
                    CASE
                        WHEN e.action_type IN ('VIEW_ACTIVE_UNIFORMS', 'VIEW_ACTIVE_STYLE_GUIDES') THEN 'browse'
                        WHEN e.action_type IN ('VIEW_UNIFORM_DETAIL', 'VIEW_STYLE_GUIDE_DETAIL') THEN 'detail'
                        WHEN e.action_type = 'SIZE_RECOMMENDATION_SUCCESS' THEN 'recommend'
                        WHEN e.action_type = 'ADD_TO_CART_SUCCESS' THEN 'cart'
                        WHEN e.action_type = 'PLACE_ORDER_SUCCESS' THEN 'order'
                    END AS stage
                FROM events e
                JOIN session_meta sm ON sm.session_key = e.session_key
                WHERE 1 = 1
                <if test="uniformId != null">
                    AND (
                        EXISTS (
                            SELECT 1
                            FROM events fu
                            WHERE fu.session_key = sm.session_key
                              AND fu.target_type = 'UNIFORM'
                              AND fu.target_id = CAST(#{uniformId} AS CHAR)
                        )
                        OR EXISTS (
                            SELECT 1
                            FROM recommendation_log rl
                            WHERE rl.user_id = sm.user_id
                              AND rl.uniform_id = #{uniformId}
                              AND rl.create_time <![CDATA[>=]]> sm.session_start
                              AND rl.create_time <![CDATA[<=]]> sm.session_end
                        )
                        OR EXISTS (
                            SELECT 1
                            FROM s_orders so
                            JOIN s_order_items oi ON oi.order_id = so.id
                            WHERE so.user_id = sm.user_id
                              AND oi.uniform_id = #{uniformId}
                              AND so.order_date <![CDATA[>=]]> sm.session_start
                              AND so.order_date <![CDATA[<=]]> sm.session_end
                        )
                    )
                </if>
                <if test="schoolId != null">
                    AND (
                        EXISTS (
                            SELECT 1
                            FROM events fs
                            JOIN s_uniform su ON su.id = CAST(fs.target_id AS UNSIGNED)
                            WHERE fs.session_key = sm.session_key
                              AND fs.target_type = 'UNIFORM'
                              AND su.school_id = #{schoolId}
                        )
                        OR EXISTS (
                            SELECT 1
                            FROM recommendation_log rl
                            JOIN s_uniform su ON su.id = rl.uniform_id
                            WHERE rl.user_id = sm.user_id
                              AND su.school_id = #{schoolId}
                              AND rl.create_time <![CDATA[>=]]> sm.session_start
                              AND rl.create_time <![CDATA[<=]]> sm.session_end
                        )
                        OR EXISTS (
                            SELECT 1
                            FROM s_orders so
                            JOIN s_order_items oi ON oi.order_id = so.id
                            JOIN s_uniform su ON su.id = oi.uniform_id
                            WHERE so.user_id = sm.user_id
                              AND su.school_id = #{schoolId}
                              AND so.order_date <![CDATA[>=]]> sm.session_start
                              AND so.order_date <![CDATA[<=]]> sm.session_end
                        )
                    )
                </if>
                <if test="gradeId != null">
                    AND (
                        EXISTS (
                            SELECT 1
                            FROM events fg
                            JOIN s_uniform su ON su.id = CAST(fg.target_id AS UNSIGNED)
                            WHERE fg.session_key = sm.session_key
                              AND fg.target_type = 'UNIFORM'
                              AND su.grade_id = #{gradeId}
                        )
                        OR EXISTS (
                            SELECT 1
                            FROM recommendation_log rl
                            JOIN s_uniform su ON su.id = rl.uniform_id
                            WHERE rl.user_id = sm.user_id
                              AND su.grade_id = #{gradeId}
                              AND rl.create_time <![CDATA[>=]]> sm.session_start
                              AND rl.create_time <![CDATA[<=]]> sm.session_end
                        )
                        OR EXISTS (
                            SELECT 1
                            FROM s_orders so
                            JOIN s_order_items oi ON oi.order_id = so.id
                            JOIN s_uniform su ON su.id = oi.uniform_id
                            WHERE so.user_id = sm.user_id
                              AND su.grade_id = #{gradeId}
                              AND so.order_date <![CDATA[>=]]> sm.session_start
                              AND so.order_date <![CDATA[<=]]> sm.session_end
                        )
                    )
                </if>
                <if test="source != null and source != ''">
                    AND EXISTS (
                        SELECT 1
                        FROM recommendation_log rl
                        WHERE rl.user_id = sm.user_id
                          AND CASE
                                  WHEN rl.request_source IS NULL OR TRIM(rl.request_source) = '' THEN 'unknown'
                                  WHEN LOWER(REPLACE(REPLACE(TRIM(rl.request_source), '_', '-'), ' ', '-')) IN
                                      ('mall-home-dialog', 'wishlist', 'order-feedback', 'product-detail', 'unknown')
                                      THEN LOWER(REPLACE(REPLACE(TRIM(rl.request_source), '_', '-'), ' ', '-'))
                                  ELSE 'unknown'
                              END = #{source}
                          AND rl.create_time <![CDATA[>=]]> sm.session_start
                          AND rl.create_time <![CDATA[<=]]> sm.session_end
                    )
                </if>
            )
            SELECT
                stage,
                COUNT(DISTINCT session_key) AS sessions_reached
            FROM staged
            WHERE stage IS NOT NULL
            GROUP BY stage
            </script>
            """)
    List<BehaviorFunnelStageCountDto> selectFunnelStageCounts(@Param("schoolId") Long schoolId,
                                                              @Param("gradeId") Long gradeId,
                                                              @Param("uniformId") Long uniformId,
                                                              @Param("source") String source,
                                                              @Param("startTime") LocalDateTime startTime,
                                                              @Param("endTime") LocalDateTime endTime,
                                                              @Param("gapMinutes") Integer gapMinutes);

    @Select("""
            <script>
            SELECT
                CASE
                    WHEN rl.request_source IS NULL OR TRIM(rl.request_source) = '' THEN 'unknown'
                    WHEN LOWER(REPLACE(REPLACE(TRIM(rl.request_source), '_', '-'), ' ', '-')) IN
                         ('mall-home-dialog', 'wishlist', 'order-feedback', 'product-detail', 'unknown')
                        THEN LOWER(REPLACE(REPLACE(TRIM(rl.request_source), '_', '-'), ' ', '-'))
                    ELSE 'unknown'
                END AS request_source,
                COUNT(*) AS exposure_count,
                COUNT(DISTINCT cart.log_id) AS linked_cart_count,
                COUNT(DISTINCT CASE WHEN rl.order_item_id IS NOT NULL THEN rl.order_item_id END) AS linked_order_count,
                COUNT(DISTINCT CASE WHEN rl.order_item_id IS NOT NULL AND rl.recommended_size_id = oi.size_id THEN rl.order_item_id END) AS adopted_count
            FROM recommendation_log rl
            LEFT JOIN s_user_activity_logs cart
                   ON cart.action_type = 'ADD_TO_CART_SUCCESS'
                  AND cart.log_detail LIKE CONCAT('%recommendationLogId:', rl.log_id, '%')
            LEFT JOIN s_order_items oi ON oi.order_item_id = rl.order_item_id
            LEFT JOIN s_uniform su ON su.id = rl.uniform_id
            <where>
                rl.create_time <![CDATA[>=]]> #{startTime}
                AND rl.create_time <![CDATA[<=]]> #{endTime}
                <if test="schoolId != null">
                    AND rl.school_id = #{schoolId}
                </if>
                <if test="gradeId != null">
                    AND su.grade_id = #{gradeId}
                </if>
                <if test="uniformId != null">
                    AND rl.uniform_id = #{uniformId}
                </if>
                <if test="source != null and source != ''">
                    AND CASE
                            WHEN rl.request_source IS NULL OR TRIM(rl.request_source) = '' THEN 'unknown'
                            WHEN LOWER(REPLACE(REPLACE(TRIM(rl.request_source), '_', '-'), ' ', '-')) IN
                                 ('mall-home-dialog', 'wishlist', 'order-feedback', 'product-detail', 'unknown')
                                THEN LOWER(REPLACE(REPLACE(TRIM(rl.request_source), '_', '-'), ' ', '-'))
                            ELSE 'unknown'
                        END = #{source}
                </if>
            </where>
            GROUP BY request_source
            </script>
            """)
    List<RecommendationEntryComparisonRowDto> selectEntryComparison(@Param("schoolId") Long schoolId,
                                                                    @Param("gradeId") Long gradeId,
                                                                    @Param("uniformId") Long uniformId,
                                                                    @Param("source") String source,
                                                                    @Param("startTime") LocalDateTime startTime,
                                                                    @Param("endTime") LocalDateTime endTime);

    @Select("""
            <script>
            SELECT
                COUNT(DISTINCT rl.order_item_id) AS linked_order_count,
                COUNT(DISTINCT CASE WHEN rl.recommended_size_id = oi.size_id THEN rl.order_item_id END) AS adopted_count,
                COUNT(DISTINCT CASE WHEN sf.feedback_id IS NOT NULL THEN rl.order_item_id END) AS with_feedback_count
            FROM recommendation_log rl
            JOIN s_order_items oi ON oi.order_item_id = rl.order_item_id
            LEFT JOIN size_feedback sf ON sf.order_item_id = rl.order_item_id
            LEFT JOIN s_uniform su ON su.id = rl.uniform_id
            <where>
                rl.order_item_id IS NOT NULL
                AND rl.create_time <![CDATA[>=]]> #{startTime}
                AND rl.create_time <![CDATA[<=]]> #{endTime}
                <if test="schoolId != null">
                    AND rl.school_id = #{schoolId}
                </if>
                <if test="gradeId != null">
                    AND su.grade_id = #{gradeId}
                </if>
                <if test="uniformId != null">
                    AND rl.uniform_id = #{uniformId}
                </if>
                <if test="source != null and source != ''">
                    AND CASE
                            WHEN rl.request_source IS NULL OR TRIM(rl.request_source) = '' THEN 'unknown'
                            WHEN LOWER(REPLACE(REPLACE(TRIM(rl.request_source), '_', '-'), ' ', '-')) IN
                                 ('mall-home-dialog', 'wishlist', 'order-feedback', 'product-detail', 'unknown')
                                THEN LOWER(REPLACE(REPLACE(TRIM(rl.request_source), '_', '-'), ' ', '-'))
                            ELSE 'unknown'
                        END = #{source}
                </if>
            </where>
            </script>
            """)
    RecommendationAdoptionStatsDto selectRecommendationAdoption(@Param("schoolId") Long schoolId,
                                                                @Param("gradeId") Long gradeId,
                                                                @Param("uniformId") Long uniformId,
                                                                @Param("source") String source,
                                                                @Param("startTime") LocalDateTime startTime,
                                                                @Param("endTime") LocalDateTime endTime);

    @Select("""
            <script>
            WITH base_events AS (
                SELECT
                    l.log_id,
                    l.user_id,
                    l.session_id,
                    l.action_type,
                    l.target_type,
                    l.target_id,
                    l.ip_address,
                    l.user_agent,
                    l.log_time,
                    CASE
                        WHEN l.session_id IS NOT NULL AND l.session_id != '' THEN CONCAT('session:', l.session_id)
                        WHEN l.user_id IS NOT NULL THEN CONCAT('user:', l.user_id)
                        ELSE CONCAT('anon:', COALESCE(l.ip_address, ''), ':', MD5(COALESCE(l.user_agent, '')))
                    END AS activity_key
                FROM s_user_activity_logs l
                WHERE l.log_time <![CDATA[>=]]> #{startTime}
                  AND l.log_time <![CDATA[<=]]> #{endTime}
            ),
            ordered_events AS (
                SELECT
                    base_events.*,
                    LAG(log_time) OVER (PARTITION BY activity_key ORDER BY log_time, log_id) AS prev_log_time
                FROM base_events
            ),
            marked_events AS (
                SELECT
                    ordered_events.*,
                    CASE
                        WHEN prev_log_time IS NULL THEN 1
                        WHEN TIMESTAMPDIFF(MINUTE, prev_log_time, log_time) > #{gapMinutes} THEN 1
                        ELSE 0
                    END AS new_session_flag
                FROM ordered_events
            ),
            events AS (
                SELECT
                    marked_events.*,
                    CONCAT(activity_key, '#',
                           SUM(new_session_flag) OVER (PARTITION BY activity_key ORDER BY log_time, log_id)) AS session_key
                FROM marked_events
            ),
            session_meta AS (
                SELECT
                    session_key,
                    MAX(user_id) AS user_id,
                    MIN(log_time) AS session_start,
                    MAX(log_time) AS session_end,
                    MAX(CASE WHEN action_type = 'PLACE_ORDER_SUCCESS' THEN 1 ELSE 0 END) AS has_order
                FROM events
                GROUP BY session_key
            ),
            last_events AS (
                SELECT
                    e.session_key,
                    e.action_type AS last_action_type,
                    ROW_NUMBER() OVER (PARTITION BY e.session_key ORDER BY e.log_time DESC, e.log_id DESC) AS row_num
                FROM events e
                JOIN session_meta sm ON sm.session_key = e.session_key
                WHERE sm.has_order = 0
                <if test="uniformId != null">
                    AND (
                        EXISTS (
                            SELECT 1
                            FROM events fu
                            WHERE fu.session_key = sm.session_key
                              AND fu.target_type = 'UNIFORM'
                              AND fu.target_id = CAST(#{uniformId} AS CHAR)
                        )
                        OR EXISTS (
                            SELECT 1
                            FROM recommendation_log rl
                            WHERE rl.user_id = sm.user_id
                              AND rl.uniform_id = #{uniformId}
                              AND rl.create_time <![CDATA[>=]]> sm.session_start
                              AND rl.create_time <![CDATA[<=]]> sm.session_end
                        )
                    )
                </if>
                <if test="schoolId != null">
                    AND (
                        EXISTS (
                            SELECT 1
                            FROM events fs
                            JOIN s_uniform su ON su.id = CAST(fs.target_id AS UNSIGNED)
                            WHERE fs.session_key = sm.session_key
                              AND fs.target_type = 'UNIFORM'
                              AND su.school_id = #{schoolId}
                        )
                        OR EXISTS (
                            SELECT 1
                            FROM recommendation_log rl
                            JOIN s_uniform su ON su.id = rl.uniform_id
                            WHERE rl.user_id = sm.user_id
                              AND su.school_id = #{schoolId}
                              AND rl.create_time <![CDATA[>=]]> sm.session_start
                              AND rl.create_time <![CDATA[<=]]> sm.session_end
                        )
                    )
                </if>
                <if test="gradeId != null">
                    AND (
                        EXISTS (
                            SELECT 1
                            FROM events fg
                            JOIN s_uniform su ON su.id = CAST(fg.target_id AS UNSIGNED)
                            WHERE fg.session_key = sm.session_key
                              AND fg.target_type = 'UNIFORM'
                              AND su.grade_id = #{gradeId}
                        )
                        OR EXISTS (
                            SELECT 1
                            FROM recommendation_log rl
                            JOIN s_uniform su ON su.id = rl.uniform_id
                            WHERE rl.user_id = sm.user_id
                              AND su.grade_id = #{gradeId}
                              AND rl.create_time <![CDATA[>=]]> sm.session_start
                              AND rl.create_time <![CDATA[<=]]> sm.session_end
                        )
                    )
                </if>
                <if test="source != null and source != ''">
                    AND EXISTS (
                        SELECT 1
                        FROM recommendation_log rl
                        WHERE rl.user_id = sm.user_id
                          AND CASE
                                  WHEN rl.request_source IS NULL OR TRIM(rl.request_source) = '' THEN 'unknown'
                                  WHEN LOWER(REPLACE(REPLACE(TRIM(rl.request_source), '_', '-'), ' ', '-')) IN
                                      ('mall-home-dialog', 'wishlist', 'order-feedback', 'product-detail', 'unknown')
                                      THEN LOWER(REPLACE(REPLACE(TRIM(rl.request_source), '_', '-'), ' ', '-'))
                                  ELSE 'unknown'
                              END = #{source}
                          AND rl.create_time <![CDATA[>=]]> sm.session_start
                          AND rl.create_time <![CDATA[<=]]> sm.session_end
                    )
                </if>
            ),
            grouped AS (
                SELECT
                    last_action_type,
                    COUNT(*) AS session_count
                FROM last_events
                WHERE row_num = 1
                GROUP BY last_action_type
            )
            SELECT
                last_action_type,
                session_count,
                CASE
                    WHEN SUM(session_count) OVER () = 0 THEN 0
                    ELSE ROUND(session_count / SUM(session_count) OVER (), 4)
                END AS share
            FROM grouped
            ORDER BY session_count DESC, last_action_type ASC
            </script>
            """)
    List<DropOffDistributionRowDto> selectDropOffDistribution(@Param("schoolId") Long schoolId,
                                                              @Param("gradeId") Long gradeId,
                                                              @Param("uniformId") Long uniformId,
                                                              @Param("source") String source,
                                                              @Param("startTime") LocalDateTime startTime,
                                                              @Param("endTime") LocalDateTime endTime,
                                                              @Param("gapMinutes") Integer gapMinutes);

    @Select("""
            <script>
            SELECT
                c.user_id,
                ua.user_account,
                c.uniform_id,
                COALESCE(su.name, CONCAT('商品#', c.uniform_id)) AS uniform_name,
                c.size_id,
                COALESCE(sz.size_name, CONCAT('尺码#', c.size_id)) AS size_name,
                c.quantity,
                c.added_at,
                TIMESTAMPDIFF(HOUR, c.added_at, NOW()) AS hours_since_add,
                c.recommendation_log_id,
                MAX(l.log_time) AS last_activity_at
            FROM s_shopping_cart_items c
            LEFT JOIN user_account ua ON ua.user_id = c.user_id
            LEFT JOIN s_uniform su ON su.id = c.uniform_id
            LEFT JOIN s_sizes sz ON sz.id = c.size_id
            LEFT JOIN s_user_activity_logs l ON l.user_id = c.user_id
            <where>
                c.added_at <![CDATA[<=]]> DATE_SUB(NOW(), INTERVAL #{thresholdHours} HOUR)
                AND c.added_at <![CDATA[>=]]> #{startTime}
                AND c.added_at <![CDATA[<=]]> #{endTime}
                <if test="schoolId != null">
                    AND su.school_id = #{schoolId}
                </if>
                <if test="gradeId != null">
                    AND su.grade_id = #{gradeId}
                </if>
                <if test="uniformId != null">
                    AND c.uniform_id = #{uniformId}
                </if>
                AND NOT EXISTS (
                    SELECT 1
                    FROM s_orders so
                    JOIN s_order_items oi ON oi.order_id = so.id
                    WHERE so.user_id = c.user_id
                      AND oi.uniform_id = c.uniform_id
                      AND so.order_date <![CDATA[>=]]> c.added_at
                )
            </where>
            GROUP BY
                c.cart_item_id,
                c.user_id,
                ua.user_account,
                c.uniform_id,
                su.name,
                c.size_id,
                sz.size_name,
                c.quantity,
                c.added_at,
                c.recommendation_log_id
            ORDER BY c.added_at ASC, c.cart_item_id ASC
            </script>
            """)
    IPage<StalledCartRowDto> selectStalledCarts(Page<StalledCartRowDto> page,
                                                @Param("schoolId") Long schoolId,
                                                @Param("gradeId") Long gradeId,
                                                @Param("uniformId") Long uniformId,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime,
                                                @Param("thresholdHours") Integer thresholdHours);
}
