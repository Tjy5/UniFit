package com.authguard.usersystem.service.impl;

import com.authguard.usersystem.entity.SGrade;
import com.authguard.usersystem.mapper.SGradeMapper;
import com.authguard.usersystem.service.ISGradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList; // 用于学校ID为空时返回空列表

@Service
public class SGradeServiceImpl implements ISGradeService {

    @Autowired
    private SGradeMapper sGradeMapper;

    @Override
    public List<SGrade> getGradeOptionsBySchoolId(Long schoolId) {
        if (schoolId == null || schoolId <= 0) {
            // 如果学校ID无效，可以返回空列表，或者调用 getAllGradeOptions()
            // return getAllGradeOptions(); // 或者
            return new ArrayList<>();
        }
        return sGradeMapper.selectGradeOptionsBySchoolId(schoolId);
    }

    @Override
    public List<SGrade> getAllGradeOptions() {
        return sGradeMapper.selectAllGradeOptions();
    }
}
