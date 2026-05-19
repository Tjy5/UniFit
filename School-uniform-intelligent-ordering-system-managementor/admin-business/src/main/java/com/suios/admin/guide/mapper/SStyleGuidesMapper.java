package com.suios.admin.guide.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.guide.entity.SStyleGuides;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SStyleGuidesMapper extends BaseMapper<SStyleGuides> {

    @Select("""
            <script>
            SELECT g.*,
                   u.name AS uniform_name
            FROM s_style_guides g
            LEFT JOIN s_uniform u ON g.uniform_id = u.id
            <where>
                <if test="title != null and title != ''">
                    AND g.title LIKE CONCAT('%', #{title}, '%')
                </if>
                <if test="uniformId != null">
                    AND g.uniform_id = #{uniformId}
                </if>
                <if test="status != null">
                    AND g.status = #{status}
                </if>
            </where>
            ORDER BY g.update_time DESC, g.id DESC
            </script>
            """)
    IPage<SStyleGuides> selectPageWithUniform(Page<SStyleGuides> page,
                                              @Param("title") String title,
                                              @Param("uniformId") Long uniformId,
                                              @Param("status") Long status);

    @Select("""
            SELECT g.*,
                   u.name AS uniform_name
            FROM s_style_guides g
            LEFT JOIN s_uniform u ON g.uniform_id = u.id
            WHERE g.id = #{id}
            """)
    SStyleGuides selectDetailById(@Param("id") Long id);
}
