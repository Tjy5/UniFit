package com.authguard.usersystem.service;

import com.authguard.usersystem.dto.ReviewCreationRequestDto;
import com.authguard.usersystem.dto.ReviewDisplayDto;
import com.authguard.usersystem.util.PageResult;

public interface ISReviewService {

    /**
     * 用户提交新的评论
     * @param userId 用户ID (从token中获取)
     * @param requestDto 包含评论所需信息的DTO (uniformId, orderItemId, rating, content, isAnonymous)
     * @return 创建的评论信息 (例如 ReviewDisplayDto)
     * @throws IllegalArgumentException 如果校验失败 (例如用户未购买该商品，或已评论过)
     */
    ReviewDisplayDto createReview(Long userId, ReviewCreationRequestDto requestDto);

    /**
     * 根据校服ID获取已显示的评论列表 (分页)
     * @param uniformId 校服ID
     * @param pageNum 当前页码
     * @param pageSize 每页数量
     * @return 分页的评论列表 (包含评论者信息)
     */
    PageResult<ReviewDisplayDto> getReviewsByUniformId(Long uniformId, int pageNum, int pageSize);

    /**
     * 根据用户ID获取该用户发表的评论列表 (分页)
     * @param userId 用户ID
     * @param pageNum 当前页码
     * @param pageSize 每页数量
     * @return 分页的评论列表
     */
    PageResult<ReviewDisplayDto> getReviewsByUserId(Long userId, int pageNum, int pageSize);

    /**
     * 更新评论
     * @param userId 用户ID (从token中获取)
     * @param reviewId 评论ID
     * @param requestDto 包含评论更新信息的DTO (rating, content, isAnonymous)
     * @return 更新后的评论信息 (例如 ReviewDisplayDto)
     * @throws IllegalArgumentException 如果校验失败 (例如评论不属于该用户)
     */
    ReviewDisplayDto updateReview(Long userId, Long reviewId, ReviewCreationRequestDto requestDto);

    /**
     * 删除评论
     * @param userId 用户ID (从token中获取)
     * @param reviewId 评论ID
     * @throws IllegalArgumentException 如果校验失败 (例如评论不属于该用户)
     */
    void deleteReview(Long userId, Long reviewId);

    // --- 辅助方法，可能在Service内部使用或暴露给其他Service ---

    /**
     * 检查用户是否有资格评论指定的订单项
     * @param userId 用户ID
     * @param orderItemId 订单项ID
     * @return 如果有资格返回true，否则false (例如，需要检查订单是否完成，是否已评论过)
     */
    boolean canUserReviewOrderItem(Long userId, Long orderItemId);

    /**
     * 更新校服的平均评分和评论总数 (通常在评论创建或状态改变后调用)
     * @param uniformId 校服ID
     */
    void updateUniformRatingAndCount(Long uniformId);
}
