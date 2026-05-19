package com.suios.admin.size.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.page.PageResult;
import com.suios.admin.size.entity.SSizes;
import com.suios.admin.size.mapper.SSizesMapper;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SSizesService {

    private final SSizesMapper sizesMapper;

    public SSizesService(SSizesMapper sizesMapper) {
        this.sizesMapper = sizesMapper;
    }

    public PageResult<SSizes> list(long pageNum, long pageSize, String sizeName) {
        LambdaQueryWrapper<SSizes> queryWrapper = new LambdaQueryWrapper<SSizes>()
                .like(StringUtils.hasText(sizeName), SSizes::getSizeName, sizeName)
                .orderByDesc(SSizes::getId);
        Page<SSizes> page = sizesMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<SSizes> listAll(String sizeName) {
        return sizesMapper.selectList(
                new LambdaQueryWrapper<SSizes>()
                        .like(StringUtils.hasText(sizeName), SSizes::getSizeName, sizeName)
                        .orderByDesc(SSizes::getId)
        );
    }

    public SSizes getById(Long id) {
        return ensureExists(id);
    }

    public void create(SSizes sizes) {
        validateRanges(sizes);
        sizesMapper.insert(sizes);
    }

    public void update(SSizes sizes) {
        ensureExists(sizes.getId());
        validateRanges(sizes);
        sizesMapper.updateById(sizes);
    }

    public void deleteByIds(String ids) {
        sizesMapper.deleteByIds(parseIds(ids));
    }

    private SSizes ensureExists(Long id) {
        SSizes sizes = sizesMapper.selectById(id);
        if (sizes == null) {
            throw new BizException("尺码不存在");
        }
        return sizes;
    }

    private List<Long> parseIds(String ids) {
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .toList();
    }

    private void validateRanges(SSizes sizes) {
        validateRange(toDecimal(sizes.getMinHeight()), toDecimal(sizes.getMaxHeight()), "身高");
        validateRange(sizes.getMinWeight(), sizes.getMaxWeight(), "体重");
        validateRange(sizes.getMinChest(), sizes.getMaxChest(), "胸围");
        validateRange(sizes.getMinWaist(), sizes.getMaxWaist(), "腰围");
        validateRange(sizes.getMinHip(), sizes.getMaxHip(), "臀围");
        validateRange(sizes.getMinShoulder(), sizes.getMaxShoulder(), "肩宽");
    }

    private void validateRange(BigDecimal min, BigDecimal max, String label) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BizException(label + "最小值不能大于最大值");
        }
    }

    private BigDecimal toDecimal(Long value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
