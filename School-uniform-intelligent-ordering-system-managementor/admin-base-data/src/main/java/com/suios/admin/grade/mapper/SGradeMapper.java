package com.suios.admin.grade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.grade.entity.SGrade;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SGradeMapper extends BaseMapper<SGrade> {

    @Select("""
            <script>
            SELECT g.grade_id,
                   g.grade_name,
                   g.school_id,
                   s.school_name AS school_name
            FROM s_grades g
            LEFT JOIN s_schools s ON g.school_id = s.school_id
            <where>
                <if test="gradeName != null and gradeName != ''">
                    AND g.grade_name LIKE CONCAT('%', #{gradeName}, '%')
                </if>
                <if test="schoolId != null">
                    AND g.school_id = #{schoolId}
                </if>
            </where>
            ORDER BY g.grade_id DESC
            </script>
            """)
    IPage<SGrade> selectPageWithSchool(Page<SGrade> page,
                                       @Param("gradeName") String gradeName,
                                       @Param("schoolId") Long schoolId);
}
