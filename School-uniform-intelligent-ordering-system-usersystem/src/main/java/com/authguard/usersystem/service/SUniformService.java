package com.authguard.usersystem.service;

import com.authguard.usersystem.entity.SUniform;

import java.util.List;

public interface SUniformService {
    // 查询所有状态为0的校服
    List<SUniform> getAllActiveUniforms();

    // 根据 ID 查询单个校服详情
    SUniform getUniformById(Long uniformId);
}