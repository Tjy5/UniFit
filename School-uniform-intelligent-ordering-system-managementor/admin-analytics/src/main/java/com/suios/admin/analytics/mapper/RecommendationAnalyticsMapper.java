package com.suios.admin.analytics.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.analytics.dto.FeedbackDistributionDto;
import com.suios.admin.analytics.dto.LowConfidenceHotspotDto;
import com.suios.admin.analytics.dto.RecommendationExperimentVariantMetricsDto;
import com.suios.admin.analytics.dto.RecommendationStatsDto;
import com.suios.admin.analytics.dto.RecommendationTrendPointDto;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RecommendationAnalyticsMapper {

    @Select("""
            <script>
            SELECT
                COUNT(*) AS total_recommendations,
                COUNT(DISTINCT CASE WHEN rl.order_item_id IS NOT NULL THEN rl.order_item_id END) AS linked_order_count,
                COUNT(DISTINCT sf.feedback_id) AS total_feedbacks,
                COALESCE(SUM(CASE WHEN sf.satisfaction = 'FIT' THEN 1 ELSE 0 END), 0) AS fit_count,
                COALESCE(SUM(CASE WHEN sf.satisfaction = 'TOO_LARGE' THEN 1 ELSE 0 END), 0) AS too_large_count,
                COALESCE(SUM(CASE WHEN sf.satisfaction = 'TOO_SMALL' THEN 1 ELSE 0 END), 0) AS too_small_count,
                COALESCE(SUM(CASE WHEN COALESCE(rl.confidence_score, 0) &lt; 60 THEN 1 ELSE 0 END), 0) AS low_confidence_count,
                CAST(COALESCE(ROUND(AVG(rl.confidence_score), 0), 0) AS SIGNED) AS avg_confidence_score
            FROM recommendation_log rl
            LEFT JOIN size_feedback sf ON sf.order_item_id = rl.order_item_id
            <where>
                rl.create_time <![CDATA[>=]]> #{startTime}
                AND rl.create_time <![CDATA[<=]]> #{endTime}
                <if test="schoolId != null">
                    AND rl.school_id = #{schoolId}
                </if>
                <if test="uniformId != null">
                    AND rl.uniform_id = #{uniformId}
                </if>
            </where>
            </script>
            """)
    RecommendationStatsDto selectRecommendationStats(@Param("schoolId") Long schoolId,
                                                     @Param("uniformId") Long uniformId,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    @Select("""
            <script>
            SELECT
                DATE_FORMAT(rl.create_time, '%Y-%m-%d') AS day,
                COUNT(*) AS total_recommendations,
                COUNT(DISTINCT CASE WHEN rl.order_item_id IS NOT NULL THEN rl.order_item_id END) AS linked_order_count,
                COUNT(DISTINCT sf.feedback_id) AS total_feedbacks,
                COALESCE(SUM(CASE WHEN sf.satisfaction = 'FIT' THEN 1 ELSE 0 END), 0) AS fit_count,
                COALESCE(SUM(CASE WHEN sf.satisfaction = 'TOO_LARGE' THEN 1 ELSE 0 END), 0) AS too_large_count,
                COALESCE(SUM(CASE WHEN sf.satisfaction = 'TOO_SMALL' THEN 1 ELSE 0 END), 0) AS too_small_count,
                CASE
                    WHEN COUNT(DISTINCT sf.feedback_id) = 0 THEN 0
                    ELSE ROUND(SUM(CASE WHEN sf.satisfaction = 'FIT' THEN 1 ELSE 0 END) / COUNT(DISTINCT sf.feedback_id), 4)
                END AS fit_rate,
                CAST(COALESCE(ROUND(AVG(rl.confidence_score), 0), 0) AS SIGNED) AS avg_confidence_score
            FROM recommendation_log rl
            LEFT JOIN size_feedback sf ON sf.order_item_id = rl.order_item_id
            <where>
                rl.create_time <![CDATA[>=]]> #{startTime}
                AND rl.create_time <![CDATA[<=]]> #{endTime}
                <if test="schoolId != null">
                    AND rl.school_id = #{schoolId}
                </if>
                <if test="uniformId != null">
                    AND rl.uniform_id = #{uniformId}
                </if>
            </where>
            GROUP BY DATE(rl.create_time)
            ORDER BY DATE(rl.create_time)
            </script>
            """)
    List<RecommendationTrendPointDto> selectRecommendationTrend(@Param("schoolId") Long schoolId,
                                                                @Param("uniformId") Long uniformId,
                                                                @Param("startTime") LocalDateTime startTime,
                                                                @Param("endTime") LocalDateTime endTime);

    @Select("""
            <script>
            SELECT
                <choose>
                    <when test="groupBy == 'school'">
                        COALESCE(CAST(rl.school_id AS CHAR), 'unknown') AS group_key,
                        COALESCE(ss.school_name, CONCAT('学校#', rl.school_id), '未关联学校') AS group_name
                    </when>
                    <when test="groupBy == 'product'">
                        COALESCE(CAST(rl.uniform_id AS CHAR), 'unknown') AS group_key,
                        COALESCE(su.name, CONCAT('商品#', rl.uniform_id), '未关联商品') AS group_name
                    </when>
                    <otherwise>
                        COALESCE(CAST(rl.recommended_size_id AS CHAR), COALESCE(rl.recommended_size_name, 'unknown')) AS group_key,
                        COALESCE(sz.size_name, rl.recommended_size_name, CONCAT('尺码#', rl.recommended_size_id), '未记录推荐尺码') AS group_name
                    </otherwise>
                </choose>,
                COUNT(DISTINCT sf.feedback_id) AS total_feedback,
                COALESCE(SUM(CASE WHEN sf.satisfaction = 'FIT' THEN 1 ELSE 0 END), 0) AS fit,
                COALESCE(SUM(CASE WHEN sf.satisfaction = 'TOO_LARGE' THEN 1 ELSE 0 END), 0) AS too_large,
                COALESCE(SUM(CASE WHEN sf.satisfaction = 'TOO_SMALL' THEN 1 ELSE 0 END), 0) AS too_small,
                CASE
                    WHEN COUNT(DISTINCT sf.feedback_id) = 0 THEN 0
                    ELSE ROUND(SUM(CASE WHEN sf.satisfaction = 'FIT' THEN 1 ELSE 0 END) / COUNT(DISTINCT sf.feedback_id), 4)
                END AS fit_rate
            FROM size_feedback sf
            LEFT JOIN recommendation_log rl ON rl.order_item_id = sf.order_item_id
            LEFT JOIN s_schools ss ON ss.school_id = rl.school_id
            LEFT JOIN s_uniform su ON su.id = rl.uniform_id
            LEFT JOIN s_sizes sz ON sz.id = rl.recommended_size_id
            <where>
                COALESCE(sf.update_time, sf.create_time) <![CDATA[>=]]> #{startTime}
                AND COALESCE(sf.update_time, sf.create_time) <![CDATA[<=]]> #{endTime}
                <if test="schoolId != null">
                    AND rl.school_id = #{schoolId}
                </if>
                <if test="uniformId != null">
                    AND rl.uniform_id = #{uniformId}
                </if>
            </where>
            GROUP BY
                <choose>
                    <when test="groupBy == 'school'">
                        COALESCE(CAST(rl.school_id AS CHAR), 'unknown'),
                        COALESCE(ss.school_name, CONCAT('学校#', rl.school_id), '未关联学校')
                    </when>
                    <when test="groupBy == 'product'">
                        COALESCE(CAST(rl.uniform_id AS CHAR), 'unknown'),
                        COALESCE(su.name, CONCAT('商品#', rl.uniform_id), '未关联商品')
                    </when>
                    <otherwise>
                        COALESCE(CAST(rl.recommended_size_id AS CHAR), COALESCE(rl.recommended_size_name, 'unknown')),
                        COALESCE(sz.size_name, rl.recommended_size_name, CONCAT('尺码#', rl.recommended_size_id), '未记录推荐尺码')
                    </otherwise>
                </choose>
            ORDER BY
                <choose>
                    <when test="sortBy == 'totalFeedback'">
                        total_feedback
                    </when>
                    <when test="sortBy == 'fit'">
                        fit
                    </when>
                    <when test="sortBy == 'tooLarge'">
                        too_large
                    </when>
                    <when test="sortBy == 'tooSmall'">
                        too_small
                    </when>
                    <when test="sortBy == 'tooLargeRate'">
                        CASE
                            WHEN COUNT(DISTINCT sf.feedback_id) = 0 THEN 0
                            ELSE SUM(CASE WHEN sf.satisfaction = 'TOO_LARGE' THEN 1 ELSE 0 END) / COUNT(DISTINCT sf.feedback_id)
                        END
                    </when>
                    <when test="sortBy == 'tooSmallRate'">
                        CASE
                            WHEN COUNT(DISTINCT sf.feedback_id) = 0 THEN 0
                            ELSE SUM(CASE WHEN sf.satisfaction = 'TOO_SMALL' THEN 1 ELSE 0 END) / COUNT(DISTINCT sf.feedback_id)
                        END
                    </when>
                    <otherwise>
                        fit_rate
                    </otherwise>
                </choose>
                <choose>
                    <when test="sortOrder == 'desc'">DESC</when>
                    <otherwise>ASC</otherwise>
                </choose>,
                total_feedback DESC,
                group_name ASC
            </script>
            """)
    IPage<FeedbackDistributionDto> selectFeedbackDistribution(Page<FeedbackDistributionDto> page,
                                                              @Param("groupBy") String groupBy,
                                                              @Param("schoolId") Long schoolId,
                                                              @Param("uniformId") Long uniformId,
                                                              @Param("startTime") LocalDateTime startTime,
                                                              @Param("endTime") LocalDateTime endTime,
                                                              @Param("sortBy") String sortBy,
                                                              @Param("sortOrder") String sortOrder);

    @Select("""
            <script>
            SELECT
                rl.uniform_id,
                COALESCE(su.name, CONCAT('商品#', rl.uniform_id), '未关联商品') AS uniform_name,
                rl.recommended_size_id AS size_id,
                COALESCE(sz.size_name, rl.recommended_size_name, CONCAT('尺码#', rl.recommended_size_id), '未记录推荐尺码') AS size_name,
                COALESCE(SUM(CASE WHEN COALESCE(rl.confidence_score, 0) &lt; #{threshold} THEN 1 ELSE 0 END), 0) AS low_confidence_count,
                COUNT(*) AS total_recommendations,
                CASE
                    WHEN COUNT(*) = 0 THEN 0
                    ELSE ROUND(SUM(CASE WHEN COALESCE(rl.confidence_score, 0) &lt; #{threshold} THEN 1 ELSE 0 END) / COUNT(*), 4)
                END AS low_confidence_rate,
                CAST(COALESCE(ROUND(AVG(rl.confidence_score), 0), 0) AS SIGNED) AS avg_confidence
            FROM recommendation_log rl
            LEFT JOIN s_uniform su ON su.id = rl.uniform_id
            LEFT JOIN s_sizes sz ON sz.id = rl.recommended_size_id
            <where>
                <if test="startTime != null">
                    rl.create_time <![CDATA[>=]]> #{startTime}
                </if>
                <if test="endTime != null">
                    AND rl.create_time <![CDATA[<=]]> #{endTime}
                </if>
                <if test="schoolId != null">
                    AND rl.school_id = #{schoolId}
                </if>
                <if test="uniformId != null">
                    AND rl.uniform_id = #{uniformId}
                </if>
                AND rl.uniform_id IS NOT NULL
            </where>
            GROUP BY rl.uniform_id,
                     COALESCE(su.name, CONCAT('商品#', rl.uniform_id), '未关联商品'),
                     rl.recommended_size_id,
                     COALESCE(sz.size_name, rl.recommended_size_name, CONCAT('尺码#', rl.recommended_size_id), '未记录推荐尺码')
            HAVING low_confidence_count &gt; 0
            ORDER BY low_confidence_rate DESC, low_confidence_count DESC, avg_confidence ASC
            LIMIT #{limit}
            </script>
            """)
    List<LowConfidenceHotspotDto> selectLowConfidenceHotspots(@Param("schoolId") Long schoolId,
                                                              @Param("uniformId") Long uniformId,
                                                              @Param("threshold") Integer threshold,
                                                              @Param("limit") Integer limit,
                                                              @Param("startTime") LocalDateTime startTime,
                                                              @Param("endTime") LocalDateTime endTime);

    @Select("""
            <script>
            SELECT
                rl.experiment_key,
                rl.experiment_variant,
                COUNT(*) AS total_recommendations,
                COUNT(DISTINCT CASE WHEN rl.order_item_id IS NOT NULL THEN rl.order_item_id END) AS linked_order_count,
                COUNT(DISTINCT sf.feedback_id) AS total_feedbacks,
                COALESCE(SUM(CASE WHEN sf.satisfaction = 'FIT' THEN 1 ELSE 0 END), 0) AS fit_count,
                COALESCE(SUM(CASE WHEN sf.satisfaction = 'TOO_LARGE' THEN 1 ELSE 0 END), 0) AS too_large_count,
                COALESCE(SUM(CASE WHEN sf.satisfaction = 'TOO_SMALL' THEN 1 ELSE 0 END), 0) AS too_small_count,
                COALESCE(SUM(CASE WHEN COALESCE(rl.confidence_score, 0) &lt; 60 THEN 1 ELSE 0 END), 0) AS low_confidence_count,
                CAST(COALESCE(ROUND(AVG(rl.confidence_score), 0), 0) AS SIGNED) AS avg_confidence_score,
                COALESCE(SUM(CASE WHEN JSON_VALID(rl.calibration_details)
                    AND JSON_UNQUOTE(JSON_EXTRACT(rl.calibration_details, '$.parameterHit')) = 'true' THEN 1 ELSE 0 END), 0) AS calibration_hit_count,
                COALESCE(SUM(CASE WHEN rl.calibration_applied = 1 THEN 1 ELSE 0 END), 0) AS calibration_applied_count,
                COALESCE(SUM(CASE WHEN JSON_VALID(rl.calibration_details)
                    AND JSON_UNQUOTE(JSON_EXTRACT(rl.calibration_details, '$.recommendationChanged')) = 'true' THEN 1 ELSE 0 END), 0) AS recommendation_changed_count,
                COALESCE(SUM(CASE WHEN JSON_VALID(rl.calibration_details)
                    THEN COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(rl.calibration_details, '$.safetyGateSkipCount')) AS UNSIGNED), 0)
                    ELSE 0 END), 0) AS safety_gate_skip_count
            FROM recommendation_log rl
            LEFT JOIN size_feedback sf ON sf.order_item_id = rl.order_item_id
            <where>
                rl.create_time <![CDATA[>=]]> #{startTime}
                AND rl.create_time <![CDATA[<=]]> #{endTime}
                AND rl.experiment_key IS NOT NULL
                AND rl.experiment_variant IN ('A', 'B')
                <if test="schoolId != null">
                    AND rl.school_id = #{schoolId}
                </if>
                <if test="uniformId != null">
                    AND rl.uniform_id = #{uniformId}
                </if>
            </where>
            GROUP BY rl.experiment_key, rl.experiment_variant
            ORDER BY rl.experiment_key, rl.experiment_variant
            </script>
            """)
    List<RecommendationExperimentVariantMetricsDto> selectExperimentMetrics(@Param("schoolId") Long schoolId,
                                                                            @Param("uniformId") Long uniformId,
                                                                            @Param("startTime") LocalDateTime startTime,
                                                                            @Param("endTime") LocalDateTime endTime);
}
