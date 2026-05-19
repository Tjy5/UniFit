package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.SStyleGuides;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SStyleGuidesMapper {
    // 查询所有状态为0的穿搭指南
    List<SStyleGuides> selectAllActiveStyleGuides();

    // 根据ID查询穿搭指南
    SStyleGuides selectById(Long id);
}