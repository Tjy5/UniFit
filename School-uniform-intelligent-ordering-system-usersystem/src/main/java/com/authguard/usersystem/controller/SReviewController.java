package com.authguard.usersystem.controller;

import com.authguard.usersystem.dto.ApiResponse;
import com.authguard.usersystem.dto.ReviewCreationRequestDto;
import com.authguard.usersystem.dto.ReviewDisplayDto;
import com.authguard.usersystem.exception.BizException;
import com.authguard.usersystem.security.CurrentUserResolver;
import com.authguard.usersystem.service.ISReviewService;
import com.authguard.usersystem.service.ISUserActivityLogService;
import com.authguard.usersystem.util.PageResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/s-reviews")
@RequiredArgsConstructor
public class SReviewController {

    private final ISReviewService reviewService;
    private final CurrentUserResolver currentUserResolver;
    private final ISUserActivityLogService activityLogService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewDisplayDto>> submitReview(@Valid @RequestBody ReviewCreationRequestDto reviewDto,
                                                                      HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        ReviewDisplayDto createdReview = reviewService.createReview(userId, reviewDto);
        activityLogService.recordActivityAsync(userId, "CREATE_REVIEW_SUCCESS", "ORDER_ITEM",
                reviewDto.getOrderItemId().toString(), "rating:" + reviewDto.getRating(), request);
        return ResponseEntity.status(201).body(ApiResponse.success(createdReview));
    }

    @GetMapping("/uniform/{uniformId}")
    public ResponseEntity<ApiResponse<PageResult<ReviewDisplayDto>>> getReviewsForUniform(@PathVariable Long uniformId,
                                                                                           @RequestParam(defaultValue = "1") int pageNum,
                                                                                           @RequestParam(defaultValue = "10") int pageSize) {
        if (uniformId == null || uniformId <= 0) {
            throw new BizException("无效的校服ID");
        }
        return ResponseEntity.ok(ApiResponse.success(reviewService.getReviewsByUniformId(uniformId, pageNum, pageSize)));
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<PageResult<ReviewDisplayDto>>> getMyReviews(@RequestParam(defaultValue = "1") int pageNum,
                                                                                   @RequestParam(defaultValue = "10") int pageSize,
                                                                                   HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        return ResponseEntity.ok(ApiResponse.success(reviewService.getReviewsByUserId(userId, pageNum, pageSize)));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewDisplayDto>> updateReview(@PathVariable Long reviewId,
                                                                       @Valid @RequestBody ReviewCreationRequestDto reviewDto,
                                                                       HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        ReviewDisplayDto updatedReview = reviewService.updateReview(userId, reviewId, reviewDto);
        activityLogService.recordActivityAsync(userId, "UPDATE_REVIEW_SUCCESS", "REVIEW",
                reviewId.toString(), "rating:" + reviewDto.getRating(), request);
        return ResponseEntity.ok(ApiResponse.success(updatedReview));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId, HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        reviewService.deleteReview(userId, reviewId);
        activityLogService.recordActivityAsync(userId, "DELETE_REVIEW_SUCCESS", "REVIEW", reviewId.toString(), null, request);
        return ResponseEntity.ok(ApiResponse.success("评论删除成功", null));
    }
}
