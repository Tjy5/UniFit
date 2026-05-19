package com.authguard.usersystem.service.impl;

import com.authguard.usersystem.entity.SUniform;
import com.authguard.usersystem.mapper.SUniformMapper;
import com.authguard.usersystem.service.SUniformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SUniformServiceImpl implements SUniformService {

    @Autowired
    private SUniformMapper sUniformMapper;

    @Override
    public List<SUniform> getAllActiveUniforms() {
        return sUniformMapper.selectAllActiveUniforms();
    }

    @Override
    public SUniform getUniformById(Long uniformId) {
        if (uniformId == null) {
            return null;
        }
        return sUniformMapper.selectSUniformById(uniformId);
    }
}