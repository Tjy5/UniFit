package com.suios.admin.school.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.page.PageResult;
import com.suios.admin.school.entity.SSchool;
import com.suios.admin.school.mapper.SSchoolMapper;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SSchoolService {

    private final SSchoolMapper schoolMapper;

    public SSchoolService(SSchoolMapper schoolMapper) {
        this.schoolMapper = schoolMapper;
    }

    public PageResult<SSchool> list(long pageNum, long pageSize, String schoolName) {
        LambdaQueryWrapper<SSchool> queryWrapper = new LambdaQueryWrapper<SSchool>()
                .like(StringUtils.hasText(schoolName), SSchool::getSchoolName, schoolName)
                .orderByAsc(SSchool::getSchoolName);
        Page<SSchool> page = schoolMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<SSchool> selectList() {
        return schoolMapper.selectList(
                new LambdaQueryWrapper<SSchool>()
                        .select(SSchool::getSchoolId, SSchool::getSchoolName)
                        .orderByAsc(SSchool::getSchoolName)
        );
    }

    public SSchool getById(Long schoolId) {
        return ensureExists(schoolId);
    }

    public void create(SSchool school) {
        schoolMapper.insert(school);
    }

    public void update(SSchool school) {
        ensureExists(school.getSchoolId());
        schoolMapper.updateById(school);
    }

    public void deleteByIds(String ids) {
        schoolMapper.deleteByIds(parseIds(ids));
    }

    private SSchool ensureExists(Long schoolId) {
        SSchool school = schoolMapper.selectById(schoolId);
        if (school == null) {
            throw new BizException("学校不存在");
        }
        return school;
    }

    private List<Long> parseIds(String ids) {
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .toList();
    }
}
