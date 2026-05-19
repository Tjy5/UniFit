package com.suios.admin.inventory.controller;

import com.suios.admin.common.annotation.OperLog;
import com.suios.admin.common.page.PageResult;
import com.suios.admin.common.result.R;
import com.suios.admin.common.util.ExcelUtils;
import com.suios.admin.inventory.dto.InventoryMovementRequest;
import com.suios.admin.inventory.dto.LowStockAlertDto;
import com.suios.admin.inventory.entity.InventoryMovement;
import com.suios.admin.inventory.entity.InventorySku;
import com.suios.admin.inventory.service.InventoryMovementService;
import com.suios.admin.inventory.service.InventorySkuService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventorySkuService skuService;
    private final InventoryMovementService movementService;

    @GetMapping("/sku/list")
    public R<PageResult<InventorySku>> listSkus(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) Long schoolId,
                                                @RequestParam(required = false) Long gradeId,
                                                @RequestParam(required = false) Long uniformId,
                                                @RequestParam(required = false) Long sizeId,
                                                @RequestParam(required = false) String categoryKey,
                                                @RequestParam(required = false) String status) {
        return R.success(skuService.list(pageNum, pageSize, schoolId, gradeId, uniformId, sizeId, categoryKey, status));
    }

    @GetMapping("/sku/{skuId}")
    public R<InventorySku> getSku(@PathVariable Long skuId) {
        return R.success(skuService.getById(skuId));
    }

    @PostMapping("/sku")
    @OperLog(module = "库存SKU管理", operation = "INSERT")
    public R<InventorySku> createSku(@Valid @RequestBody InventorySku sku) {
        return R.success("库存SKU创建成功", skuService.create(sku));
    }

    @PutMapping("/sku")
    @OperLog(module = "库存SKU管理", operation = "UPDATE")
    public R<InventorySku> updateSku(@Valid @RequestBody InventorySku sku) {
        return R.success("库存SKU更新成功", skuService.update(sku));
    }

    @DeleteMapping("/sku/{ids}")
    @OperLog(module = "库存SKU管理", operation = "DELETE")
    public R<Void> deleteSkus(@PathVariable String ids) {
        skuService.deleteByIds(ids);
        return R.success("库存SKU删除成功");
    }

    @GetMapping("/movement/list")
    public R<List<InventoryMovement>> listMovements(@RequestParam(required = false) Long skuId,
                                                    @RequestParam(required = false) String movementType,
                                                    @RequestParam(required = false) Long relatedOrderId,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.success(movementService.list(skuId, movementType, relatedOrderId, startTime, endTime));
    }

    @PostMapping("/movement")
    @OperLog(module = "库存流水管理", operation = "INSERT")
    public R<InventoryMovement> applyMovement(@Valid @RequestBody InventoryMovementRequest request) {
        return R.success("库存流水处理成功", movementService.apply(request));
    }

    @GetMapping("/alerts/low-stock")
    public R<List<LowStockAlertDto>> lowStockAlerts(@RequestParam(required = false) Long schoolId,
                                                    @RequestParam(required = false) Long gradeId,
                                                    @RequestParam(required = false) Long uniformId,
                                                    @RequestParam(required = false) Long sizeId,
                                                    @RequestParam(required = false) String categoryKey,
                                                    @RequestParam(required = false) String severity) {
        return R.success(skuService.lowStockAlerts(schoolId, gradeId, uniformId, sizeId, categoryKey, severity));
    }

    @PostMapping("/sku/export")
    @OperLog(module = "库存SKU管理", operation = "EXPORT")
    public void exportSkus(@RequestParam(required = false) Long schoolId,
                           @RequestParam(required = false) Long gradeId,
                           @RequestParam(required = false) Long uniformId,
                           @RequestParam(required = false) Long sizeId,
                           @RequestParam(required = false) String categoryKey,
                           @RequestParam(required = false) String status,
                           HttpServletResponse response) throws IOException {
        List<List<?>> rows = new ArrayList<>();
        for (InventorySku item : skuService.listAll(schoolId, gradeId, uniformId, sizeId, categoryKey, status)) {
            rows.add(Arrays.asList(
                    item.getSkuId(),
                    item.getUniformName(),
                    item.getSchoolName(),
                    item.getGradeName(),
                    item.getCategoryKey(),
                    item.getSizeName(),
                    item.getStockQuantity(),
                    item.getReservedQuantity(),
                    item.getAvailableQuantity(),
                    item.getSafetyStock(),
                    item.getReorderPoint(),
                    item.getLeadTimeDays(),
                    item.getStatus()
            ));
        }
        ExcelUtils.export(
                "inventory-skus",
                List.of("SKU ID", "校服", "学校", "年级", "品类", "尺码", "库存", "预留", "可用", "安全库存", "补货点", "提前期", "状态"),
                rows,
                response
        );
    }

    @PostMapping("/alerts/low-stock/export")
    @OperLog(module = "低库存告警", operation = "EXPORT")
    public void exportLowStockAlerts(@RequestParam(required = false) Long schoolId,
                                     @RequestParam(required = false) Long gradeId,
                                     @RequestParam(required = false) Long uniformId,
                                     @RequestParam(required = false) Long sizeId,
                                     @RequestParam(required = false) String categoryKey,
                                     @RequestParam(required = false) String severity,
                                     HttpServletResponse response) throws IOException {
        List<List<?>> rows = new ArrayList<>();
        for (LowStockAlertDto item : skuService.lowStockAlerts(schoolId, gradeId, uniformId, sizeId, categoryKey, severity)) {
            rows.add(Arrays.asList(
                    item.getSkuId(),
                    item.getUniformName(),
                    item.getSchoolName(),
                    item.getGradeName(),
                    item.getCategoryKey(),
                    item.getSizeName(),
                    item.getAvailableQuantity(),
                    item.getSafetyStock(),
                    item.getReorderPoint(),
                    item.getShortageQuantity(),
                    item.getSeverity()
            ));
        }
        ExcelUtils.export(
                "low-stock-alerts",
                List.of("SKU ID", "校服", "学校", "年级", "品类", "尺码", "可用库存", "安全库存", "补货点", "缺口", "严重程度"),
                rows,
                response
        );
    }
}
