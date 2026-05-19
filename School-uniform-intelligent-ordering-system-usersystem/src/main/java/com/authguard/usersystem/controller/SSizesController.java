package com.authguard.usersystem.controller;

import com.authguard.usersystem.entity.SSizes;
import com.authguard.usersystem.service.SSizesService;
// ⭐ 1. Import
import com.authguard.usersystem.service.ISUserActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
// End Import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/s-sizes")
public class SSizesController {
    @Autowired
    private SSizesService sSizesService;

    // ⭐ 2. Inject
    @Autowired
    private ISUserActivityLogService activityLogService;

    @GetMapping("/all")
    public List<SSizes> getAllSizes(
            HttpServletRequest request // ⭐ 3.
    ) {
        activityLogService.recordActivityAsync(null, "VIEW_ALL_SIZES", null, null, null, request);
        return sSizesService.getAllSizes();
    }
}
