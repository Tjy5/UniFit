package com.authguard.usersystem.controller;

import com.authguard.usersystem.dto.ApiResponse;
import com.authguard.usersystem.dto.SWishlistDetailDto;
import com.authguard.usersystem.exception.BizException;
import com.authguard.usersystem.exception.NotFoundException;
import com.authguard.usersystem.security.CurrentUserResolver;
import com.authguard.usersystem.service.SWishlistService;

// ⭐ 1. Import
import com.authguard.usersystem.service.ISUserActivityLogService;
// End Import

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/s-wishlist")
public class SWishlistController {

    @Autowired
    private SWishlistService wishlistService;

    @Autowired
    private CurrentUserResolver currentUserResolver;

    // ⭐ 2. Inject
    @Autowired
    private ISUserActivityLogService activityLogService;

    // ObjectMapper not used here, but kept for consistency if needed later for complex request DTOs
    // @Autowired(required = false)
    // private ObjectMapper objectMapper;


    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Void>> addToWishlist(
            @RequestParam("uniformId") @Min(value = 1, message = "uniformId必须大于0") Long uniformId,
            HttpServletRequest request // ⭐ 3.
    ) {
        Long userId = currentUserResolver.requireUserId(request);

        boolean added = wishlistService.addToWishlist(userId, uniformId);
        if (added) {
            activityLogService.recordActivityAsync(userId, "ADD_TO_WISHLIST_SUCCESS", "UNIFORM", uniformId.toString(), null, request);
            return ResponseEntity.status(201).body(ApiResponse.success("商品已成功加入收藏夹！", null));
        } else {
            activityLogService.recordActivityAsync(userId, "ADD_TO_WISHLIST_FAILED", "UNIFORM", uniformId.toString(), "Already exists or failed", request);
            throw new BizException("商品已在收藏夹中或添加失败。");
        }
    }

    @DeleteMapping("/remove/{uniformId}")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @PathVariable("uniformId") @Min(value = 1, message = "uniformId必须大于0") Long uniformId,
            HttpServletRequest request // ⭐ 3.
    ) {
        Long userId = currentUserResolver.requireUserId(request);

        boolean removed = wishlistService.removeFromWishlist(userId, uniformId);
        if (removed) {
            activityLogService.recordActivityAsync(userId, "REMOVE_FROM_WISHLIST_SUCCESS", "UNIFORM", uniformId.toString(), null, request);
            return ResponseEntity.ok(ApiResponse.success("已从收藏夹移除", null));
        } else {
            activityLogService.recordActivityAsync(userId, "REMOVE_FROM_WISHLIST_FAILED", "UNIFORM", uniformId.toString(), "Not found", request);
            throw new NotFoundException("Item not found in wishlist to remove.");
        }
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<SWishlistDetailDto>>> getWishlist(
            HttpServletRequest request // ⭐ 3.
    ) {
        Long userId = currentUserResolver.requireUserId(request);
        List<SWishlistDetailDto> list = wishlistService.getWishlistByUserId(userId);
        activityLogService.recordActivityAsync(userId, "VIEW_WISHLIST", "WISHLIST", null, "count:" + list.size(), request);
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
