package com.authguard.usersystem.service.impl;

import com.authguard.usersystem.entity.SStyleGuides;
import com.authguard.usersystem.mapper.SStyleGuidesMapper;
import com.authguard.usersystem.service.SStyleGuidesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SStyleGuidesServiceImpl implements SStyleGuidesService {

    @Autowired
    private SStyleGuidesMapper sStyleGuidesMapper;

    @Override
    public List<SStyleGuides> getAllActiveStyleGuides() {
        return sStyleGuidesMapper.selectAllActiveStyleGuides();
    }

    @Override
    public SStyleGuides getStyleGuideById(Long id) {
        return sStyleGuidesMapper.selectById(id);
    }
}