package com.authguard.usersystem.controller;

import com.authguard.usersystem.entity.SGrade;
import com.authguard.usersystem.service.ISGradeService;
// ⭐ 1. Import
import com.authguard.usersystem.service.ISUserActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
// End Import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/s-grades")
public class SGradeController {
    @Autowired
    private ISGradeService sGradeService;

    // ⭐ 2. Inject
    @Autowired
    private ISUserActivityLogService activityLogService;

    @GetMapping("/listBySchool")
    public ResponseEntity<List<SGrade>> getGradeOptionsBySchoolId(
            @RequestParam(required = true) Long schoolId,
            HttpServletRequest request // ⭐ 3.
    ) {
        // No specific user ID here, log the general API call
        // userId can be null for anonymous/system actions
        activityLogService.recordActivityAsync(null, "VIEW_GRADES_BY_SCHOOL", "SCHOOL", schoolId.toString(), null, request);
        List<SGrade> grades = sGradeService.getGradeOptionsBySchoolId(schoolId);
        return ResponseEntity.ok(grades);
    }

    @GetMapping("/listAll")
    public ResponseEntity<List<SGrade>> getAllGradeOptions(
            HttpServletRequest request // ⭐ 3.
    ) {
        activityLogService.recordActivityAsync(null, "VIEW_ALL_GRADES", null, null, null, request);
        List<SGrade> grades = sGradeService.getAllGradeOptions();
        return ResponseEntity.ok(grades);
    }
}
