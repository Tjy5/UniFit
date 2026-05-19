package com.suios.admin.analytics.controller;

import com.suios.admin.analytics.dto.FeedbackDistributionResponse;
import com.suios.admin.analytics.dto.LowConfidenceHotspotsResponse;
import com.suios.admin.analytics.dto.RecommendationExperimentMetricsResponse;
import com.suios.admin.analytics.dto.RecommendationStatsDto;
import com.suios.admin.analytics.service.RecommendationAnalyticsService;
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
@Tag(name = "Recommendation Analytics", description = "推荐效果分析、反馈分布与低置信度热点接口")
public class RecommendationAnalyticsController {

    private final RecommendationAnalyticsService recommendationAnalyticsService;

    @GetMapping("/recommendation-stats")
    @Operation(summary = "获取推荐效果统计", description = "按学校、商品和日期范围聚合推荐量、关联订单覆盖率、反馈覆盖率和趋势数据。")
    public R<RecommendationStatsDto> recommendationStats(
                                                         @Parameter(description = "学校ID，留空表示全部学校")
                                                         @RequestParam(required = false) Long schoolId,
                                                         @Parameter(description = "商品ID，留空表示全部商品")
                                                         @RequestParam(required = false) Long uniformId,
                                                         @Parameter(description = "开始日期，格式 YYYY-MM-DD", required = true)
                                                         @RequestParam
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                         @Parameter(description = "结束日期，格式 YYYY-MM-DD", required = true)
                                                         @RequestParam
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.success(recommendationAnalyticsService.getRecommendationStats(schoolId, uniformId, startDate, endDate));
    }

    @GetMapping("/recommendation-experiment")
    @Operation(summary = "获取尺码推荐实验对比", description = "按学校、商品和日期范围聚合 A/B 实验核心指标与 B 组校准诊断指标。")
    public R<RecommendationExperimentMetricsResponse> recommendationExperiment(
                                                         @Parameter(description = "学校ID，留空表示全部学校")
                                                         @RequestParam(required = false) Long schoolId,
                                                         @Parameter(description = "商品ID，留空表示全部商品")
                                                         @RequestParam(required = false) Long uniformId,
                                                         @Parameter(description = "开始日期，格式 YYYY-MM-DD", required = true)
                                                         @RequestParam
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                         @Parameter(description = "结束日期，格式 YYYY-MM-DD", required = true)
                                                         @RequestParam
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.success(recommendationAnalyticsService.getExperimentMetrics(schoolId, uniformId, startDate, endDate));
    }

    @GetMapping("/feedback-distribution")
    @Operation(summary = "获取反馈分布", description = "按学校、商品或尺码分组统计反馈，并支持排序与分页。")
    public R<FeedbackDistributionResponse> feedbackDistribution(
                                                                @Parameter(description = "分组维度：school、product、size", required = true)
                                                                @RequestParam String groupBy,
                                                                @Parameter(description = "学校ID，留空表示全部学校")
                                                                @RequestParam(required = false) Long schoolId,
                                                                @Parameter(description = "商品ID，留空表示全部商品")
                                                                @RequestParam(required = false) Long uniformId,
                                                                @Parameter(description = "开始日期，格式 YYYY-MM-DD", required = true)
                                                                @RequestParam
                                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                @Parameter(description = "结束日期，格式 YYYY-MM-DD", required = true)
                                                                @RequestParam
                                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                @Parameter(description = "分页大小，默认 20，最大受 analytics.max-page-size 限制")
                                                                @RequestParam(required = false) Integer limit,
                                                                @Parameter(description = "结果偏移量，默认 0")
                                                                @RequestParam(required = false) Integer offset,
                                                                @Parameter(description = "排序字段：fitRate、totalFeedback、fit、tooLarge、tooSmall、tooLargeRate、tooSmallRate")
                                                                @RequestParam(required = false) String sortBy,
                                                                @Parameter(description = "排序方向：asc 或 desc")
                                                                @RequestParam(required = false) String sortOrder) {
        return R.success(recommendationAnalyticsService.getFeedbackDistribution(
                groupBy,
                schoolId,
                uniformId,
                startDate,
                endDate,
                limit,
                offset,
                sortBy,
                sortOrder
        ));
    }

    @GetMapping("/low-confidence-hotspots")
    @Operation(summary = "获取低置信度热点", description = "返回低置信度占比最高的商品与尺码热点，支持按学校、商品、阈值和时间范围过滤。")
    public R<LowConfidenceHotspotsResponse> lowConfidenceHotspots(
                                                                  @Parameter(description = "学校ID，留空表示全部学校")
                                                                  @RequestParam(required = false) Long schoolId,
                                                                  @Parameter(description = "商品ID，留空表示全部商品")
                                                                  @RequestParam(required = false) Long uniformId,
                                                                  @Parameter(description = "低置信度阈值，默认 60，范围会被夹紧到 0-100")
                                                                  @RequestParam(required = false) Integer threshold,
                                                                  @Parameter(description = "返回条数，默认 20，最大受 analytics.max-page-size 限制")
                                                                  @RequestParam(required = false) Integer limit,
                                                                  @Parameter(description = "开始日期，格式 YYYY-MM-DD")
                                                                  @RequestParam(required = false)
                                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                  @Parameter(description = "结束日期，格式 YYYY-MM-DD")
                                                                  @RequestParam(required = false)
                                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.success(recommendationAnalyticsService.getLowConfidenceHotspots(
                schoolId,
                uniformId,
                threshold,
                limit,
                startDate,
                endDate
        ));
    }
}
