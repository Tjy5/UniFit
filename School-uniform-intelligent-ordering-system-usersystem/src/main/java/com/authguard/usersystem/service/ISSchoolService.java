package com.authguard.usersystem.service;

import com.authguard.usersystem.entity.SSchool;
import java.util.List;

public interface ISSchoolService {
    /**
     * 获取所有学校选项列表
     * @return 学校列表
     */
    List<SSchool> getAllSchoolOptions();
}
