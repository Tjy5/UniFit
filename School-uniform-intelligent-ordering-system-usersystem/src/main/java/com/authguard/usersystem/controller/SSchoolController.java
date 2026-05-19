package com.authguard.usersystem.controller;

import com.authguard.usersystem.entity.SSchool;
import com.authguard.usersystem.service.ISSchoolService;
// ⭐ 1. Import
import com.authguard.usersystem.service.ISUserActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
// End Import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/s-schools")
public class SSchoolController {
    @Autowired
    private ISSchoolService sSchoolService;

    // ⭐ 2. Inject
    @Autowired
    private ISUserActivityLogService activityLogService;

    @GetMapping("/selectList")
    public ResponseEntity<List<SSchool>> getAllSchoolOptions(
            HttpServletRequest request // ⭐ 3.
    ) {
        activityLogService.recordActivityAsync(null, "VIEW_ALL_SCHOOLS", null, null, null, request);
        List<SSchool> schools = sSchoolService.getAllSchoolOptions();
        return ResponseEntity.ok(schools);
    }
}
