package com.authguard.usersystem.service;

import com.authguard.usersystem.entity.SSizes;

import java.util.List;

public interface SSizesService {
    // 查询所有尺码信息
    List<SSizes> getAllSizes();
}