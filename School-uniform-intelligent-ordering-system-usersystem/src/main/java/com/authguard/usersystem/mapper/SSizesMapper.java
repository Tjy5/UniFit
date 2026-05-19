package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.SSizes; // 你的 SSizes 实体类
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SSizesMapper {
    // 查询所有尺码信息
    List<SSizes> selectAllSizes();

    // 新增：根据 ID 查询单个尺码的方法 (你可能已经有这个或类似的方法)
    SSizes selectSSizesById(Long id);

    // 新增：根据 ID 列表查询多个尺码的方法
    List<SSizes> selectSSizesByIds(@Param("ids") List<Long> ids);
}

