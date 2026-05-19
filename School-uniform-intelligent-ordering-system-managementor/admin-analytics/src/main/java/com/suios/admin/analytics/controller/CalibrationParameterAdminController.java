package com.suios.admin.analytics.controller;

import com.suios.admin.analytics.dto.CalibrationAuditDto;
import com.suios.admin.analytics.dto.CalibrationImpactResponseDto;
import com.suios.admin.analytics.dto.CalibrationLifecycleRequest;
import com.suios.admin.analytics.dto.CalibrationParamDetailDto;
import com.suios.admin.analytics.dto.CalibrationParamListItemDto;
import com.suios.admin.analytics.dto.CalibrationParamVersionDto;
import com.suios.admin.analytics.dto.CalibrationRollbackRequest;
import com.suios.admin.analytics.service.CalibrationParameterAdminService;
import com.suios.admin.common.annotation.OperLog;
import com.suios.admin.common.page.PageResult;
import com.suios.admin.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/calibration/params")
@RequiredArgsConstructor
@Tag(name = "Calibration Parameter Admin", description = "校准参数列表、详情、版本、审计、生命周期和影响分析接口")
public class CalibrationParameterAdminController {

    private final CalibrationParameterAdminService calibrationParameterAdminService;

    @GetMapping("/list")
    @Operation(summary = "分页查询校准参数", description = "按作用域、尺码、启用状态、状态、匹配把握等级和版本筛选校准参数。")
    public R<PageResult<CalibrationParamListItemDto>> list(@RequestParam(defaultValue = "1") long pageNum,
                                                           @RequestParam(defaultValue = "10") long pageSize,
                                                           @RequestParam(required = false) String scopeType,
                                                           @RequestParam(required = false) String scopeId,
                                                           @RequestParam(required = false) Long targetSizeId,
                                                           @RequestParam(required = false) Boolean enabled,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(required = false) String confidenceLevel,
                                                           @RequestParam(required = false) Integer version) {
        return R.success(calibrationParameterAdminService.list(
                pageNum,
                pageSize,
                scopeType,
                scopeId,
                targetSizeId,
                enabled,
                status,
                confidenceLevel,
                version
        ));
    }

    @GetMapping("/{paramId}")
    @Operation(summary = "获取校准参数详情")
    public R<CalibrationParamDetailDto> detail(@Parameter(description = "校准参数ID", required = true)
                                               @PathVariable Long paramId) {
        return R.success(calibrationParameterAdminService.detail(paramId));
    }

    @GetMapping("/{paramId}/versions")
    @Operation(summary = "获取同一参数身份下的版本历史")
    public R<List<CalibrationParamVersionDto>> versions(@PathVariable Long paramId) {
        return R.success(calibrationParameterAdminService.versions(paramId));
    }

    @GetMapping("/{paramId}/audits")
    @Operation(summary = "获取同一参数身份下的审计历史")
    public R<List<CalibrationAuditDto>> audits(@PathVariable Long paramId) {
        return R.success(calibrationParameterAdminService.audits(paramId));
    }

    @GetMapping("/{paramId}/impact")
    @Operation(summary = "获取校准参数推荐分影响分析")
    public R<CalibrationImpactResponseDto> impact(@PathVariable Long paramId,
                                                  @RequestParam(required = false) Integer version,
                                                  @RequestParam(required = false) Long schoolId,
                                                  @RequestParam(required = false) Long uniformId,
                                                  @RequestParam(required = false) Long sizeId,
                                                  @RequestParam(required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                  @RequestParam(required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                  @RequestParam(required = false) Integer limit,
                                                  @RequestParam(required = false) Integer offset) {
        return R.success(calibrationParameterAdminService.impact(
                paramId,
                version,
                schoolId,
                uniformId,
                sizeId,
                startDate,
                endDate,
                limit,
                offset
        ));
    }

    @PostMapping("/{paramId}/enable")
    @OperLog(module = "校准参数管理", operation = "ENABLE")
    @Operation(summary = "启用校准参数")
    public R<CalibrationParamDetailDto> enable(@PathVariable Long paramId,
                                               @Valid @RequestBody CalibrationLifecycleRequest request) {
        return R.success("校准参数已启用", calibrationParameterAdminService.enable(paramId, request.getReason()));
    }

    @PostMapping("/{paramId}/disable")
    @OperLog(module = "校准参数管理", operation = "DISABLE")
    @Operation(summary = "停用校准参数")
    public R<CalibrationParamDetailDto> disable(@PathVariable Long paramId,
                                                @Valid @RequestBody CalibrationLifecycleRequest request) {
        return R.success("校准参数已停用", calibrationParameterAdminService.disable(paramId, request.getReason()));
    }

    @PostMapping("/{paramId}/rollback")
    @OperLog(module = "校准参数管理", operation = "ROLLBACK")
    @Operation(summary = "回滚到历史校准参数版本")
    public R<CalibrationParamDetailDto> rollback(@PathVariable Long paramId,
                                                 @Valid @RequestBody CalibrationRollbackRequest request) {
        return R.success("校准参数已回滚", calibrationParameterAdminService.rollback(paramId, request));
    }
}
