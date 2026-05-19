package com.authguard.usersystem.controller;

import com.authguard.usersystem.dto.ApiResponse;
import com.authguard.usersystem.dto.SizeRecommendationRequest;
import com.authguard.usersystem.dto.SizeRecommendationResponse;
import com.authguard.usersystem.security.CurrentUserResolver;
import com.authguard.usersystem.service.ISUserActivityLogService;
import com.authguard.usersystem.service.SizeRecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/size-recommendations")
public class SizeRecommendationController {

    private final SizeRecommendationService sizeRecommendationService;
    private final CurrentUserResolver currentUserResolver;
    private final ISUserActivityLogService activityLogService;

    public SizeRecommendationController(SizeRecommendationService sizeRecommendationService,
                                        CurrentUserResolver currentUserResolver,
                                        ISUserActivityLogService activityLogService) {
        this.sizeRecommendationService = sizeRecommendationService;
        this.currentUserResolver = currentUserResolver;
        this.activityLogService = activityLogService;
    }

    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse<SizeRecommendationResponse>> recommend(@Valid @RequestBody(required = false) SizeRecommendationRequest request,
                                                                             HttpServletRequest httpServletRequest) {
        Long userId = currentUserResolver.requireUserId(httpServletRequest);
        SizeRecommendationResponse response = sizeRecommendationService.recommend(userId, request);
        activityLogService.recordActivityAsync(
                userId,
                "SIZE_RECOMMENDATION_SUCCESS",
                "SIZE_RECOMMENDATION",
                response.getRecommended() == null ? null : response.getRecommended().getSizeName(),
                response.getMessage(),
                httpServletRequest
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
