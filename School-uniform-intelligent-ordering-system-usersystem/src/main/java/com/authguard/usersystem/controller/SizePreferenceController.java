package com.authguard.usersystem.controller;

import com.authguard.usersystem.dto.ApiResponse;
import com.authguard.usersystem.dto.SizePreferenceProfileDto;
import com.authguard.usersystem.dto.SizePreferenceRequest;
import com.authguard.usersystem.security.CurrentUserResolver;
import com.authguard.usersystem.service.ISUserActivityLogService;
import com.authguard.usersystem.service.SizePreferenceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/size-preferences")
public class SizePreferenceController {

    private final SizePreferenceService sizePreferenceService;
    private final CurrentUserResolver currentUserResolver;
    private final ISUserActivityLogService activityLogService;

    public SizePreferenceController(SizePreferenceService sizePreferenceService,
                                    CurrentUserResolver currentUserResolver,
                                    ISUserActivityLogService activityLogService) {
        this.sizePreferenceService = sizePreferenceService;
        this.currentUserResolver = currentUserResolver;
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<SizePreferenceProfileDto>> getPreference(@RequestParam(required = false) Long uniformId,
                                                                               @RequestParam(required = false) String categoryKey,
                                                                               HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        SizePreferenceProfileDto response = sizePreferenceService.getPreference(userId, uniformId, categoryKey);
        activityLogService.recordActivityAsync(userId, "VIEW_SIZE_PREFERENCE", "SIZE_PREFERENCE", categoryKey, null, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<SizePreferenceProfileDto>> savePreference(@Valid @RequestBody SizePreferenceRequest preferenceRequest,
                                                                                HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        SizePreferenceProfileDto response = sizePreferenceService.saveExplicitPreference(userId, preferenceRequest);
        activityLogService.recordActivityAsync(userId, "SAVE_SIZE_PREFERENCE", "SIZE_PREFERENCE",
                response == null ? null : response.getCategoryKey(),
                response == null ? null : response.getEffectivePreference(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
