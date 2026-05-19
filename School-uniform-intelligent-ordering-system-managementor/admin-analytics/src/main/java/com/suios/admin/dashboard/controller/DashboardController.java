package com.suios.admin.dashboard.controller;

import com.suios.admin.common.result.R;
import com.suios.admin.dashboard.dto.DashboardStatsResponse;
import com.suios.admin.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public R<DashboardStatsResponse> stats() {
        return R.success(dashboardService.getStats());
    }
}
