package com.authguard.usersystem.service;

import com.authguard.usersystem.entity.SStyleGuides;

import java.util.List;

public interface SStyleGuidesService {
    // 查询所有状态为0的穿搭指南
    List<SStyleGuides> getAllActiveStyleGuides();

    // 根据ID查询穿搭指南
    SStyleGuides getStyleGuideById(Long id);
}