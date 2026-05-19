package com.suios.admin.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.review.entity.SReview;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SReviewMapper extends BaseMapper<SReview> {

    @Select("""
            <script>
            SELECT r.*,
                   ua.user_account AS user_account,
                   u.name AS uniform_name
            FROM s_reviews r
            LEFT JOIN user_account ua ON r.user_id = ua.user_id
            LEFT JOIN s_uniform u ON r.uniform_id = u.id
            <where>
                <if test="uniformId != null">
                    AND r.uniform_id = #{uniformId}
                </if>
                <if test="status != null">
                    AND r.status = #{status}
                </if>
            </where>
            ORDER BY r.create_time DESC, r.review_id DESC
            </script>
            """)
    IPage<SReview> selectPageWithDetails(Page<SReview> page,
                                         @Param("uniformId") Long uniformId,
                                         @Param("status") Integer status);

    @Select("""
            <script>
            SELECT r.*,
                   ua.user_account AS user_account,
                   u.name AS uniform_name
            FROM s_reviews r
            LEFT JOIN user_account ua ON r.user_id = ua.user_id
            LEFT JOIN s_uniform u ON r.uniform_id = u.id
            <where>
                <if test="uniformId != null">
                    AND r.uniform_id = #{uniformId}
                </if>
                <if test="status != null">
                    AND r.status = #{status}
                </if>
            </where>
            ORDER BY r.create_time DESC, r.review_id DESC
            </script>
            """)
    List<SReview> selectListWithDetails(@Param("uniformId") Long uniformId, @Param("status") Integer status);

    @Select("""
            SELECT r.*,
                   ua.user_account AS user_account,
                   u.name AS uniform_name
            FROM s_reviews r
            LEFT JOIN user_account ua ON r.user_id = ua.user_id
            LEFT JOIN s_uniform u ON r.uniform_id = u.id
            WHERE r.review_id = #{reviewId}
            """)
    SReview selectDetailById(@Param("reviewId") Long reviewId);
}
