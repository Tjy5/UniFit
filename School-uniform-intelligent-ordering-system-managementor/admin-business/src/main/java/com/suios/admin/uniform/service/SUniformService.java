package com.suios.admin.uniform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.common.enums.UniformStatus;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.page.PageResult;
import com.suios.admin.grade.entity.SGrade;
import com.suios.admin.grade.mapper.SGradeMapper;
import com.suios.admin.school.mapper.SSchoolMapper;
import com.suios.admin.uniform.entity.SUniform;
import com.suios.admin.uniform.mapper.SUniformMapper;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SUniformService {

    private final SUniformMapper uniformMapper;
    private final SSchoolMapper schoolMapper;
    private final SGradeMapper gradeMapper;

    public SUniformService(SUniformMapper uniformMapper,
                           SSchoolMapper schoolMapper,
                           SGradeMapper gradeMapper) {
        this.uniformMapper = uniformMapper;
        this.schoolMapper = schoolMapper;
        this.gradeMapper = gradeMapper;
    }

    public PageResult<SUniform> list(long pageNum, long pageSize, String name, Long schoolId, Long gradeId, Long status) {
        IPage<SUniform> page = uniformMapper.selectPageWithRelations(new Page<>(pageNum, pageSize), name, schoolId, gradeId, status);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<SUniform> listAll(String name, Long schoolId, Long gradeId, Long status) {
        return uniformMapper.selectListWithRelations(name, schoolId, gradeId, status);
    }

    public SUniform getById(Long id) {
        SUniform uniform = uniformMapper.selectById(id);
        if (uniform == null) {
            throw new BizException("校服不存在");
        }
        return uniform;
    }

    public void create(SUniform uniform) {
        validateRelations(uniform);
        if (uniform.getStatus() == null) {
            uniform.setStatus(0L);
        }
        uniformMapper.insert(uniform);
    }

    public void update(SUniform uniform) {
        ensureExists(uniform.getId());
        validateRelations(uniform);
        uniformMapper.updateById(uniform);
    }

    public void deleteByIds(String ids) {
        uniformMapper.deleteByIds(parseIds(ids));
    }

    private void validateRelations(SUniform uniform) {
        if (uniform.getSchoolId() != null && schoolMapper.selectById(uniform.getSchoolId()) == null) {
            throw new BizException("所属学校不存在");
        }

        if (uniform.getGradeId() != null) {
            SGrade grade = gradeMapper.selectById(uniform.getGradeId());
            if (grade == null) {
                throw new BizException("所属年级不存在");
            }
            if (uniform.getSchoolId() != null
                    && grade.getSchoolId() != null
                    && !grade.getSchoolId().equals(uniform.getSchoolId())) {
                throw new BizException("年级与学校不匹配");
            }
        }

        if (uniform.getStatus() != null && !isValidUniformStatus(uniform.getStatus())) {
            throw new BizException("校服状态不合法");
        }
    }

    private boolean isValidUniformStatus(Long status) {
        return Arrays.stream(UniformStatus.values())
                .anyMatch(item -> item.getCode().longValue() == status);
    }

    private void ensureExists(Long id) {
        if (id == null || uniformMapper.selectById(id) == null) {
            throw new BizException("校服不存在");
        }
    }

    private List<Long> parseIds(String ids) {
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .toList();
    }
}
