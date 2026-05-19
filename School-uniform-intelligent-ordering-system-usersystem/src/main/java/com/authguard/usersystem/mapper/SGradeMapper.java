package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.SGrade;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SGradeMapper {
    /**
     * 根据学校ID查询年级，用于下拉框选项
     * @param schoolId 学校ID
     * @return 该学校下的年级列表
     */
    List<SGrade> selectGradeOptionsBySchoolId(@Param("schoolId") Long schoolId);

    /**
     * 查询所有年级，用于在未选择学校时的下拉框选项 (可选)
     * @return 所有年级列表
     */
    List<SGrade> selectAllGradeOptions();
}
