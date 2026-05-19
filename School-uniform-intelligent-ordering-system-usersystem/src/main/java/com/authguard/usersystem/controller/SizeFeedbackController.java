package com.authguard.usersystem.controller;

import com.authguard.usersystem.dto.ApiResponse;
import com.authguard.usersystem.dto.SizeFeedbackRequest;
import com.authguard.usersystem.dto.SizeFeedbackResponse;
import com.authguard.usersystem.dto.SizeRecommendationResponse;
import com.authguard.usersystem.exception.NotFoundException;
import com.authguard.usersystem.security.CurrentUserResolver;
import com.authguard.usersystem.service.ISUserActivityLogService;
import com.authguard.usersystem.service.SizeFeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/size-feedback")
public class SizeFeedbackController {

    private final SizeFeedbackService sizeFeedbackService;
    private final CurrentUserResolver currentUserResolver;
    private final ISUserActivityLogService activityLogService;

    public SizeFeedbackController(SizeFeedbackService sizeFeedbackService,
                                  CurrentUserResolver currentUserResolver,
                                  ISUserActivityLogService activityLogService) {
        this.sizeFeedbackService = sizeFeedbackService;
        this.currentUserResolver = currentUserResolver;
        this.activityLogService = activityLogService;
    }

    @GetMapping("/order-items/{orderItemId}")
    public ResponseEntity<ApiResponse<SizeFeedbackResponse>> getFeedback(@PathVariable Long orderItemId,
                                                                         HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        SizeFeedbackResponse response = sizeFeedbackService.getFeedback(userId, orderItemId);
        if (response == null) {
            throw new NotFoundException("尺码反馈不存在");
        }
        activityLogService.recordActivityAsync(userId, "VIEW_SIZE_FEEDBACK", "ORDER_ITEM", orderItemId.toString(), null, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/order-items/{orderItemId}/recommendation")
    public ResponseEntity<ApiResponse<SizeRecommendationResponse>> getRecommendationSnapshot(@PathVariable Long orderItemId,
                                                                                             HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        SizeRecommendationResponse response = sizeFeedbackService.getRecommendationSnapshot(userId, orderItemId);
        if (response == null) {
            throw new NotFoundException("推荐快照不存在");
        }
        activityLogService.recordActivityAsync(userId, "VIEW_SIZE_RECOMMENDATION_SNAPSHOT", "ORDER_ITEM", orderItemId.toString(), null, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/order-items/{orderItemId}")
    public ResponseEntity<ApiResponse<SizeFeedbackResponse>> saveFeedback(@PathVariable Long orderItemId,
                                                                          @Valid @RequestBody SizeFeedbackRequest feedbackRequest,
                                                                          HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        SizeFeedbackResponse response = sizeFeedbackService.saveFeedback(userId, orderItemId, feedbackRequest);
        activityLogService.recordActivityAsync(userId, "SAVE_SIZE_FEEDBACK_SUCCESS", "ORDER_ITEM", orderItemId.toString(),
                response.getSatisfaction(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
