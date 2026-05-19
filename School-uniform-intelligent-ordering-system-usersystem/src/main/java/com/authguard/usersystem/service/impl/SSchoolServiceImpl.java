package com.authguard.usersystem.service.impl;

import com.authguard.usersystem.entity.SSchool;
import com.authguard.usersystem.mapper.SSchoolMapper;
import com.authguard.usersystem.service.ISSchoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SSchoolServiceImpl implements ISSchoolService {

    @Autowired
    private SSchoolMapper sSchoolMapper;

    @Override
    public List<SSchool> getAllSchoolOptions() {
        return sSchoolMapper.selectAllSchoolOptions();
    }
}
