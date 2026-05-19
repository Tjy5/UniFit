package com.suios.admin.analytics.controller;

import com.suios.admin.analytics.dto.BehaviorFunnelResponse;
import com.suios.admin.analytics.dto.DropOffDistributionResponse;
import com.suios.admin.analytics.dto.RecommendationAdoptionStatsDto;
import com.suios.admin.analytics.dto.RecommendationEntryComparisonResponse;
import com.suios.admin.analytics.dto.StalledCartsResponse;
import com.suios.admin.analytics.service.BehaviorFunnelService;
import com.suios.admin.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Tag(name = "Behavior Funnel Analytics", description = "用户行为漏斗、入口对比、采纳率、流失与滞留购物车分析接口")
public class BehaviorFunnelController {

    private final BehaviorFunnelService behaviorFunnelService;

    @GetMapping("/behavior-funnel")
    @Operation(summary = "获取用户行为漏斗", description = "按会话统计浏览、详情、推荐、加购、下单五个阶段达到数与相邻转化率。")
    public R<BehaviorFunnelResponse> behaviorFunnel(
            @Parameter(description = "学校ID，留空表示全部学校")
            @RequestParam(required = false) Long schoolId,
            @Parameter(description = "年级ID，留空表示全部年级")
            @RequestParam(required = false) Long gradeId,
            @Parameter(description = "商品ID，留空表示全部商品")
            @RequestParam(required = false) Long uniformId,
            @Parameter(description = "推荐入口：mall-home-dialog、wishlist、order-feedback、unknown")
            @RequestParam(required = false) String source,
            @Parameter(description = "开始日期，格式 YYYY-MM-DD", required = true)
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期，格式 YYYY-MM-DD", required = true)
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.success(behaviorFunnelService.getBehaviorFunnel(schoolId, gradeId, uniformId, source, startDate, endDate));
    }

    @GetMapping("/recommendation-entry-comparison")
    @Operation(summary = "获取推荐入口对比", description = "按推荐入口统计曝光、加购、下单、采纳数量和转化率。")
    public R<RecommendationEntryComparisonResponse> recommendationEntryComparison(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long gradeId,
            @RequestParam(required = false) Long uniformId,
            @RequestParam(required = false) String source,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.success(behaviorFunnelService.getEntryComparison(schoolId, gradeId, uniformId, source, startDate, endDate));
    }

    @GetMapping("/recommendation-adoption")
    @Operation(summary = "获取推荐采纳指标", description = "统计推荐尺码与实际下单尺码一致率、尺码修改率和反馈覆盖样本数。")
    public R<RecommendationAdoptionStatsDto> recommendationAdoption(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long gradeId,
            @RequestParam(required = false) Long uniformId,
            @RequestParam(required = false) String source,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.success(behaviorFunnelService.getRecommendationAdoption(schoolId, gradeId, uniformId, source, startDate, endDate));
    }

    @GetMapping("/behavior-drop-off")
    @Operation(summary = "获取行为流失分布", description = "按未下单会话的最后行为统计流失分布，并合并 OTHER 桶。")
    public R<DropOffDistributionResponse> behaviorDropOff(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long gradeId,
            @RequestParam(required = false) Long uniformId,
            @RequestParam(required = false) String source,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer limit) {
        return R.success(behaviorFunnelService.getDropOffDistribution(schoolId, gradeId, uniformId, source, startDate, endDate, limit));
    }

    @GetMapping("/stalled-carts")
    @Operation(summary = "获取滞留购物车候选清单", description = "返回超过配置阈值且未转化为同商品订单的购物车项。")
    public R<StalledCartsResponse> stalledCarts(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Long gradeId,
            @RequestParam(required = false) Long uniformId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        return R.success(behaviorFunnelService.getStalledCarts(schoolId, gradeId, uniformId, startDate, endDate, limit, offset));
    }
}
