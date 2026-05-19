package com.suios.admin.inventory.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.page.PageResult;
import com.suios.admin.inventory.dto.LowStockAlertDto;
import com.suios.admin.inventory.entity.InventorySku;
import com.suios.admin.inventory.mapper.InventorySkuMapper;
import com.suios.admin.size.mapper.SSizesMapper;
import com.suios.admin.uniform.mapper.SUniformMapper;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InventorySkuService {

    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";

    private final InventorySkuMapper skuMapper;
    private final SUniformMapper uniformMapper;
    private final SSizesMapper sizesMapper;

    public InventorySkuService(InventorySkuMapper skuMapper,
                               SUniformMapper uniformMapper,
                               SSizesMapper sizesMapper) {
        this.skuMapper = skuMapper;
        this.uniformMapper = uniformMapper;
        this.sizesMapper = sizesMapper;
    }

    public PageResult<InventorySku> list(long pageNum,
                                         long pageSize,
                                         Long schoolId,
                                         Long gradeId,
                                         Long uniformId,
                                         Long sizeId,
                                         String categoryKey,
                                         String status) {
        IPage<InventorySku> page = skuMapper.selectPageWithContext(
                new Page<>(pageNum, pageSize),
                schoolId,
                gradeId,
                uniformId,
                sizeId,
                categoryKey,
                normalizeStatusOrNull(status)
        );
        page.getRecords().forEach(this::fillAvailableQuantity);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<InventorySku> listAll(Long schoolId,
                                      Long gradeId,
                                      Long uniformId,
                                      Long sizeId,
                                      String categoryKey,
                                      String status) {
        List<InventorySku> rows = skuMapper.selectListWithContext(
                schoolId,
                gradeId,
                uniformId,
                sizeId,
                categoryKey,
                normalizeStatusOrNull(status)
        );
        rows.forEach(this::fillAvailableQuantity);
        return rows;
    }

    public InventorySku getById(Long skuId) {
        InventorySku sku = skuMapper.selectByIdWithContext(skuId);
        if (sku == null) {
            throw new BizException("库存SKU不存在");
        }
        fillAvailableQuantity(sku);
        return sku;
    }

    @Transactional
    public InventorySku create(InventorySku sku) {
        normalizeForSave(sku);
        validateMasterData(sku);
        validateQuantities(sku);
        ensureNoDuplicateActiveSku(sku);
        int inserted = skuMapper.insert(sku);
        if (inserted == 0) {
            throw new BizException("库存SKU创建失败");
        }
        return getById(sku.getSkuId());
    }

    @Transactional
    public InventorySku update(InventorySku sku) {
        if (sku.getSkuId() == null || skuMapper.selectById(sku.getSkuId()) == null) {
            throw new BizException("库存SKU不存在");
        }
        normalizeForSave(sku);
        validateMasterData(sku);
        validateQuantities(sku);
        ensureNoDuplicateActiveSku(sku);
        int updated = skuMapper.updateById(sku);
        if (updated == 0) {
            throw new BizException("库存SKU更新失败");
        }
        return getById(sku.getSkuId());
    }

    @Transactional
    public void deleteByIds(String ids) {
        List<Long> skuIds = parseIds(ids);
        if (skuIds.isEmpty()) {
            throw new BizException("请选择库存SKU");
        }
        skuMapper.deleteByIds(skuIds);
    }

    public List<LowStockAlertDto> lowStockAlerts(Long schoolId,
                                                 Long gradeId,
                                                 Long uniformId,
                                                 Long sizeId,
                                                 String categoryKey,
                                                 String severity) {
        String normalizedSeverity = StringUtils.hasText(severity) ? severity.trim().toUpperCase() : null;
        return listAll(schoolId, gradeId, uniformId, sizeId, categoryKey, ACTIVE)
                .stream()
                .map(this::toLowStockAlert)
                .filter(alert -> alert.getAvailableQuantity() <= alert.getReorderPoint())
                .filter(alert -> normalizedSeverity == null || normalizedSeverity.equals(alert.getSeverity()))
                .sorted(Comparator.comparingInt(this::severityRank).reversed()
                        .thenComparing(Comparator.comparing(LowStockAlertDto::getShortageQuantity).reversed()))
                .toList();
    }

    public long availableQuantity(InventorySku sku) {
        return valueOrZero(sku.getStockQuantity()) - valueOrZero(sku.getReservedQuantity());
    }

    private LowStockAlertDto toLowStockAlert(InventorySku sku) {
        LowStockAlertDto alert = new LowStockAlertDto();
        alert.setSkuId(sku.getSkuId());
        alert.setUniformId(sku.getUniformId());
        alert.setSizeId(sku.getSizeId());
        alert.setUniformName(sku.getUniformName());
        alert.setSchoolName(sku.getSchoolName());
        alert.setGradeName(sku.getGradeName());
        alert.setCategoryKey(sku.getCategoryKey());
        alert.setSizeName(sku.getSizeName());
        alert.setStockQuantity(valueOrZero(sku.getStockQuantity()));
        alert.setReservedQuantity(valueOrZero(sku.getReservedQuantity()));
        alert.setAvailableQuantity(availableQuantity(sku));
        alert.setSafetyStock(valueOrZero(sku.getSafetyStock()));
        alert.setReorderPoint(valueOrZero(sku.getReorderPoint()));
        alert.setShortageQuantity(Math.max(0, alert.getReorderPoint() - alert.getAvailableQuantity()));
        alert.setSeverity(severity(alert.getAvailableQuantity(), alert.getSafetyStock(), alert.getReorderPoint()));
        return alert;
    }

    private String severity(long availableQuantity, long safetyStock, long reorderPoint) {
        if (availableQuantity < 0 || availableQuantity < safetyStock) {
            return "CRITICAL";
        }
        if (availableQuantity <= reorderPoint) {
            return "WARNING";
        }
        return "OK";
    }

    private int severityRank(LowStockAlertDto alert) {
        return switch (alert.getSeverity()) {
            case "CRITICAL" -> 3;
            case "WARNING" -> 2;
            default -> 1;
        };
    }

    private void normalizeForSave(InventorySku sku) {
        if (sku.getStockQuantity() == null) {
            sku.setStockQuantity(0L);
        }
        if (sku.getReservedQuantity() == null) {
            sku.setReservedQuantity(0L);
        }
        if (sku.getSafetyStock() == null) {
            sku.setSafetyStock(0L);
        }
        if (sku.getReorderPoint() == null) {
            sku.setReorderPoint(0L);
        }
        if (sku.getLeadTimeDays() == null) {
            sku.setLeadTimeDays(0);
        }
        sku.setStatus(normalizeStatusOrDefault(sku.getStatus()));
    }

    private void validateMasterData(InventorySku sku) {
        if (sku.getUniformId() == null || uniformMapper.selectById(sku.getUniformId()) == null) {
            throw new BizException("校服不存在");
        }
        if (sku.getSizeId() == null || sizesMapper.selectById(sku.getSizeId()) == null) {
            throw new BizException("尺码不存在");
        }
    }

    private void validateQuantities(InventorySku sku) {
        if (valueOrZero(sku.getStockQuantity()) < 0
                || valueOrZero(sku.getReservedQuantity()) < 0
                || valueOrZero(sku.getSafetyStock()) < 0
                || valueOrZero(sku.getReorderPoint()) < 0
                || sku.getLeadTimeDays() < 0) {
            throw new BizException("库存数量和提前期不能为负数");
        }
        if (valueOrZero(sku.getReservedQuantity()) > valueOrZero(sku.getStockQuantity())) {
            throw new BizException("预留库存不能大于库存数量");
        }
    }

    private void ensureNoDuplicateActiveSku(InventorySku sku) {
        if (!ACTIVE.equals(sku.getStatus())) {
            return;
        }
        InventorySku existing = skuMapper.selectActiveByUniformAndSize(sku.getUniformId(), sku.getSizeId());
        if (existing != null && !existing.getSkuId().equals(sku.getSkuId())) {
            throw new BizException("同一校服和尺码已存在启用库存SKU");
        }
    }

    private String normalizeStatusOrDefault(String status) {
        String normalizedStatus = normalizeStatusOrNull(status);
        return normalizedStatus == null ? ACTIVE : normalizedStatus;
    }

    private String normalizeStatusOrNull(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalizedStatus = status.trim().toUpperCase();
        if (!ACTIVE.equals(normalizedStatus) && !INACTIVE.equals(normalizedStatus)) {
            throw new BizException("库存SKU状态不合法");
        }
        return normalizedStatus;
    }

    private void fillAvailableQuantity(InventorySku sku) {
        sku.setAvailableQuantity(availableQuantity(sku));
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private List<Long> parseIds(String ids) {
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .toList();
    }
}
