package com.authguard.usersystem.service.impl;

import com.authguard.usersystem.entity.SSizes;
import com.authguard.usersystem.mapper.SSizesMapper;
import com.authguard.usersystem.service.SSizesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SSizesServiceImpl implements SSizesService {

    @Autowired
    private SSizesMapper sSizesMapper;

    @Override
    public List<SSizes> getAllSizes() {
        return sSizesMapper.selectAllSizes();
    }
}