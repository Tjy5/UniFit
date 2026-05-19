package com.suios.admin.analytics.controller;

import com.suios.admin.analytics.dto.ReplenishmentSuggestionDto;
import com.suios.admin.analytics.dto.SupplyForecastDto;
import com.suios.admin.analytics.dto.SupplyLowStockAlertDto;
import com.suios.admin.analytics.dto.SupplySalesAnalyticsDto;
import com.suios.admin.analytics.dto.SupplyTrendPointDto;
import com.suios.admin.analytics.service.SupplyDemandAnalyticsService;
import com.suios.admin.common.annotation.OperLog;
import com.suios.admin.common.result.R;
import com.suios.admin.common.util.ExcelUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics/supply")
@RequiredArgsConstructor
public class SupplyDemandAnalyticsController {

    private final SupplyDemandAnalyticsService service;

    @GetMapping("/sales")
    public R<List<SupplySalesAnalyticsDto>> sales(@RequestParam(defaultValue = "uniform") String groupBy,
                                                  @RequestParam(required = false) Long schoolId,
                                                  @RequestParam(required = false) Long gradeId,
                                                  @RequestParam(required = false) Long uniformId,
                                                  @RequestParam(required = false) Long sizeId,
                                                  @RequestParam(required = false) String categoryKey,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.success(service.salesAnalytics(groupBy, schoolId, gradeId, uniformId, sizeId, categoryKey, startDate, endDate));
    }

    @GetMapping("/trend")
    public R<List<SupplyTrendPointDto>> trend(@RequestParam(defaultValue = "day") String bucketType,
                                              @RequestParam(required = false) Long schoolId,
                                              @RequestParam(required = false) Long gradeId,
                                              @RequestParam(required = false) Long uniformId,
                                              @RequestParam(required = false) Long sizeId,
                                              @RequestParam(required = false) String categoryKey,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.success(service.trend(bucketType, schoolId, gradeId, uniformId, sizeId, categoryKey, startDate, endDate));
    }

    @GetMapping("/low-stock")
    public R<List<SupplyLowStockAlertDto>> lowStock(@RequestParam(required = false) Long schoolId,
                                                    @RequestParam(required = false) Long gradeId,
                                                    @RequestParam(required = false) Long uniformId,
                                                    @RequestParam(required = false) Long sizeId,
                                                    @RequestParam(required = false) String categoryKey,
                                                    @RequestParam(required = false) String severity) {
        return R.success(service.lowStockAlerts(schoolId, gradeId, uniformId, sizeId, categoryKey, severity));
    }

    @GetMapping("/forecast")
    public R<List<SupplyForecastDto>> forecast(@RequestParam(required = false) Long schoolId,
                                               @RequestParam(required = false) Long gradeId,
                                               @RequestParam(required = false) Long uniformId,
                                               @RequestParam(required = false) Long sizeId,
                                               @RequestParam(required = false) String categoryKey,
                                               @RequestParam(required = false) Integer lookbackDays,
                                               @RequestParam(required = false) Integer horizonDays) {
        return R.success(service.forecasts(schoolId, gradeId, uniformId, sizeId, categoryKey, lookbackDays, horizonDays));
    }

    @GetMapping("/replenishment")
    public R<List<ReplenishmentSuggestionDto>> replenishment(@RequestParam(required = false) Long schoolId,
                                                             @RequestParam(required = false) Long gradeId,
                                                             @RequestParam(required = false) Long uniformId,
                                                             @RequestParam(required = false) Long sizeId,
                                                             @RequestParam(required = false) String categoryKey,
                                                             @RequestParam(required = false) Integer lookbackDays,
                                                             @RequestParam(required = false) Integer horizonDays,
                                                             @RequestParam(required = false) Boolean includeAll) {
        return R.success(service.replenishmentSuggestions(schoolId, gradeId, uniformId, sizeId, categoryKey, lookbackDays, horizonDays, includeAll));
    }

    @PostMapping("/sales/export")
    @OperLog(module = "供需销量分析", operation = "EXPORT")
    public void exportSales(@RequestParam(defaultValue = "uniform") String groupBy,
                            @RequestParam(required = false) Long schoolId,
                            @RequestParam(required = false) Long gradeId,
                            @RequestParam(required = false) Long uniformId,
                            @RequestParam(required = false) Long sizeId,
                            @RequestParam(required = false) String categoryKey,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                            HttpServletResponse response) throws IOException {
        List<List<?>> rows = new ArrayList<>();
        for (SupplySalesAnalyticsDto item : service.salesAnalytics(groupBy, schoolId, gradeId, uniformId, sizeId, categoryKey, startDate, endDate)) {
            rows.add(Arrays.asList(item.getGroupKey(), item.getGroupName(), item.getTotalQuantity(), item.getTotalAmount(), item.getOrderCount()));
        }
        ExcelUtils.export("supply-sales", List.of("分组键", "分组名称", "销量", "销售额", "订单数"), rows, response);
    }

    @PostMapping("/low-stock/export")
    @OperLog(module = "供需低库存告警", operation = "EXPORT")
    public void exportLowStock(@RequestParam(required = false) Long schoolId,
                               @RequestParam(required = false) Long gradeId,
                               @RequestParam(required = false) Long uniformId,
                               @RequestParam(required = false) Long sizeId,
                               @RequestParam(required = false) String categoryKey,
                               @RequestParam(required = false) String severity,
                               HttpServletResponse response) throws IOException {
        List<List<?>> rows = new ArrayList<>();
        for (SupplyLowStockAlertDto item : service.lowStockAlerts(schoolId, gradeId, uniformId, sizeId, categoryKey, severity)) {
            rows.add(Arrays.asList(item.getSkuId(), item.getUniformName(), item.getSchoolName(), item.getGradeName(), item.getSizeName(), item.getAvailableQuantity(), item.getReorderPoint(), item.getSafetyStock(), item.getShortageQuantity(), item.getSeverity()));
        }
        ExcelUtils.export("supply-low-stock", List.of("SKU ID", "校服", "学校", "年级", "尺码", "可用库存", "补货点", "安全库存", "缺口", "严重程度"), rows, response);
    }

    @PostMapping("/forecast/export")
    @OperLog(module = "供需预测", operation = "EXPORT")
    public void exportForecast(@RequestParam(required = false) Long schoolId,
                               @RequestParam(required = false) Long gradeId,
                               @RequestParam(required = false) Long uniformId,
                               @RequestParam(required = false) Long sizeId,
                               @RequestParam(required = false) String categoryKey,
                               @RequestParam(required = false) Integer lookbackDays,
                               @RequestParam(required = false) Integer horizonDays,
                               HttpServletResponse response) throws IOException {
        List<List<?>> rows = new ArrayList<>();
        for (SupplyForecastDto item : service.forecasts(schoolId, gradeId, uniformId, sizeId, categoryKey, lookbackDays, horizonDays)) {
            rows.add(Arrays.asList(item.getSkuId(), item.getUniformName(), item.getSizeName(), item.getHistoricalQuantity(), item.getDailyAverage(), item.getForecastDemand(), item.getAvailableQuantity(), item.getDataCompleteness(), String.join("|", item.getReasonCodes())));
        }
        ExcelUtils.export("supply-forecast", List.of("SKU ID", "校服", "尺码", "历史销量", "日均销量", "预测需求", "可用库存", "数据完整性", "原因码"), rows, response);
    }

    @PostMapping("/replenishment/export")
    @OperLog(module = "补货建议", operation = "EXPORT")
    public void exportReplenishment(@RequestParam(required = false) Long schoolId,
                                    @RequestParam(required = false) Long gradeId,
                                    @RequestParam(required = false) Long uniformId,
                                    @RequestParam(required = false) Long sizeId,
                                    @RequestParam(required = false) String categoryKey,
                                    @RequestParam(required = false) Integer lookbackDays,
                                    @RequestParam(required = false) Integer horizonDays,
                                    @RequestParam(required = false) Boolean includeAll,
                                    HttpServletResponse response) throws IOException {
        List<List<?>> rows = new ArrayList<>();
        for (ReplenishmentSuggestionDto item : service.replenishmentSuggestions(schoolId, gradeId, uniformId, sizeId, categoryKey, lookbackDays, horizonDays, includeAll)) {
            rows.add(Arrays.asList(item.getSkuId(), item.getUniformName(), item.getSizeName(), item.getAvailableQuantity(), item.getForecastDemand(), item.getSafetyStock(), item.getLeadTimeDays(), item.getSuggestedQuantity(), item.getReasonText()));
        }
        ExcelUtils.export("supply-replenishment", List.of("SKU ID", "校服", "尺码", "可用库存", "预测需求", "安全库存", "提前期", "建议补货", "原因"), rows, response);
    }
}
