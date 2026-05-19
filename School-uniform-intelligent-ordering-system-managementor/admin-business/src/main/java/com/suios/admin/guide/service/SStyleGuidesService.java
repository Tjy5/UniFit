package com.suios.admin.guide.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.page.PageResult;
import com.suios.admin.guide.entity.SStyleGuides;
import com.suios.admin.guide.mapper.SStyleGuidesMapper;
import com.suios.admin.uniform.mapper.SUniformMapper;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SStyleGuidesService {

    private final SStyleGuidesMapper guidesMapper;
    private final SUniformMapper uniformMapper;

    public SStyleGuidesService(SStyleGuidesMapper guidesMapper, SUniformMapper uniformMapper) {
        this.guidesMapper = guidesMapper;
        this.uniformMapper = uniformMapper;
    }

    public PageResult<SStyleGuides> list(long pageNum, long pageSize, String title, Long uniformId, Long status) {
        IPage<SStyleGuides> page = guidesMapper.selectPageWithUniform(new Page<>(pageNum, pageSize), title, uniformId, status);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public SStyleGuides getById(Long id) {
        SStyleGuides guide = guidesMapper.selectDetailById(id);
        if (guide == null) {
            throw new BizException("穿搭指南不存在");
        }
        return guide;
    }

    public void create(SStyleGuides guide) {
        validateGuide(guide);
        if (guide.getStatus() == null) {
            guide.setStatus(0L);
        }
        guidesMapper.insert(guide);
    }

    public void update(SStyleGuides guide) {
        ensureExists(guide.getId());
        validateGuide(guide);
        guidesMapper.updateById(guide);
    }

    public void deleteByIds(String ids) {
        guidesMapper.deleteByIds(parseIds(ids));
    }

    public List<SStyleGuides> listActive() {
        return guidesMapper.selectList(new LambdaQueryWrapper<SStyleGuides>()
                .eq(SStyleGuides::getStatus, 0L)
                .orderByDesc(SStyleGuides::getUpdateTime));
    }

    private void validateGuide(SStyleGuides guide) {
        if (guide.getUniformId() != null && uniformMapper.selectById(guide.getUniformId()) == null) {
            throw new BizException("关联校服不存在");
        }
    }

    private void ensureExists(Long id) {
        if (id == null || guidesMapper.selectById(id) == null) {
            throw new BizException("穿搭指南不存在");
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
