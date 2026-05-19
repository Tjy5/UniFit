package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.SSchool;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface SSchoolMapper {
    /**
     * 查询所有学校，用于下拉框选项
     * @return 学校列表
     */
    List<SSchool> selectAllSchoolOptions();
}
