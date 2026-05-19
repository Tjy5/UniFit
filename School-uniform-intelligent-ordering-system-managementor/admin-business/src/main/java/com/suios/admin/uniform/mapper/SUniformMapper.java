package com.suios.admin.uniform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.uniform.entity.SUniform;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SUniformMapper extends BaseMapper<SUniform> {

    @Select("""
            <script>
            SELECT u.*,
                   s.school_name AS school_name,
                   g.grade_name AS grade_name
            FROM s_uniform u
            LEFT JOIN s_schools s ON u.school_id = s.school_id
            LEFT JOIN s_grades g ON u.grade_id = g.grade_id
            <where>
                <if test="name != null and name != ''">
                    AND u.name LIKE CONCAT('%', #{name}, '%')
                </if>
                <if test="schoolId != null">
                    AND u.school_id = #{schoolId}
                </if>
                <if test="gradeId != null">
                    AND u.grade_id = #{gradeId}
                </if>
                <if test="status != null">
                    AND u.status = #{status}
                </if>
            </where>
            ORDER BY u.id DESC
            </script>
            """)
    IPage<SUniform> selectPageWithRelations(Page<SUniform> page,
                                            @Param("name") String name,
                                            @Param("schoolId") Long schoolId,
                                            @Param("gradeId") Long gradeId,
                                            @Param("status") Long status);

    @Select("""
            <script>
            SELECT u.*,
                   s.school_name AS school_name,
                   g.grade_name AS grade_name
            FROM s_uniform u
            LEFT JOIN s_schools s ON u.school_id = s.school_id
            LEFT JOIN s_grades g ON u.grade_id = g.grade_id
            <where>
                <if test="name != null and name != ''">
                    AND u.name LIKE CONCAT('%', #{name}, '%')
                </if>
                <if test="schoolId != null">
                    AND u.school_id = #{schoolId}
                </if>
                <if test="gradeId != null">
                    AND u.grade_id = #{gradeId}
                </if>
                <if test="status != null">
                    AND u.status = #{status}
                </if>
            </where>
            ORDER BY u.id DESC
            </script>
            """)
    List<SUniform> selectListWithRelations(@Param("name") String name,
                                           @Param("schoolId") Long schoolId,
                                           @Param("gradeId") Long gradeId,
                                           @Param("status") Long status);
}
