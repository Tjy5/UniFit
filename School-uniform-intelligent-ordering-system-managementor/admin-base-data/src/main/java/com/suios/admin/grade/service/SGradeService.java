package com.suios.admin.grade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.page.PageResult;
import com.suios.admin.grade.entity.SGrade;
import com.suios.admin.grade.mapper.SGradeMapper;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SGradeService {

    private final SGradeMapper gradeMapper;

    public SGradeService(SGradeMapper gradeMapper) {
        this.gradeMapper = gradeMapper;
    }

    public PageResult<SGrade> list(long pageNum, long pageSize, String gradeName, Long schoolId) {
        IPage<SGrade> page = gradeMapper.selectPageWithSchool(new Page<>(pageNum, pageSize), gradeName, schoolId);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<SGrade> selectList(Long schoolId) {
        return gradeMapper.selectList(
                new LambdaQueryWrapper<SGrade>()
                        .select(SGrade::getGradeId, SGrade::getGradeName, SGrade::getSchoolId)
                        .eq(schoolId != null, SGrade::getSchoolId, schoolId)
                        .orderByAsc(SGrade::getGradeName)
        );
    }

    public SGrade getById(Long gradeId) {
        return ensureExists(gradeId);
    }

    public void create(SGrade grade) {
        gradeMapper.insert(grade);
    }

    public void update(SGrade grade) {
        ensureExists(grade.getGradeId());
        gradeMapper.updateById(grade);
    }

    public void deleteByIds(String ids) {
        gradeMapper.deleteByIds(parseIds(ids));
    }

    private SGrade ensureExists(Long gradeId) {
        SGrade grade = gradeMapper.selectById(gradeId);
        if (grade == null) {
            throw new BizException("年级不存在");
        }
        return grade;
    }

    private List<Long> parseIds(String ids) {
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .toList();
    }
}
