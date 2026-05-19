package com.authguard.usersystem.service.impl;

import com.authguard.usersystem.dto.ReviewCreationRequestDto;
import com.authguard.usersystem.dto.ReviewDisplayDto;
import com.authguard.usersystem.entity.SOrderItem;
import com.authguard.usersystem.entity.SOrders;
import com.authguard.usersystem.entity.SReview;
import com.authguard.usersystem.entity.SUniform; // 新增导入 SUniform
import com.authguard.usersystem.entity.UserAccount;
import com.authguard.usersystem.mapper.ISReviewMapper;
import com.authguard.usersystem.mapper.SOrderItemMapper;
import com.authguard.usersystem.mapper.SOrdersMapper;
import com.authguard.usersystem.mapper.UserAccountMapper;
import com.authguard.usersystem.mapper.SUniformMapper;
import com.authguard.usersystem.service.ISReviewService;
import com.authguard.usersystem.util.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SReviewServiceImpl implements ISReviewService {

    private static final Logger log = LoggerFactory.getLogger(SReviewServiceImpl.class);

    @Autowired
    private ISReviewMapper reviewMapper;

    @Autowired
    private SOrderItemMapper orderItemsMapper;

    @Autowired
    private SOrdersMapper ordersMapper;

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private SUniformMapper uniformMapper; // 确保已注入

    @Override
    @Transactional
    public ReviewDisplayDto createReview(Long userId, ReviewCreationRequestDto dto) {
        if (!canUserReviewOrderItem(userId, dto.getOrderItemId())) {
            throw new IllegalArgumentException("您没有权限评价此商品、订单项不存在或已评价过。");
        }

        UserAccount user = userAccountMapper.selectUserById(userId);
        if (user == null) {
            log.error("createReview: UserAccount not found for userId: {}", userId);
            throw new IllegalArgumentException("评论用户信息不存在。");
        }

        SReview review = SReview.builder()
                .userId(userId)
                .uniformId(dto.getUniformId())
                .orderItemId(dto.getOrderItemId())
                .rating(dto.getRating())
                .content(dto.getContent())
                .isAnonymous(dto.getIsAnonymous() != null ? dto.getIsAnonymous() : false)
                .build();

        int inserted = reviewMapper.insertReview(review);
        if (inserted == 0 || review.getReviewId() == null) {
            log.error("createReview: Failed to insert review into database for userId: {}, orderItemId: {}", userId, dto.getOrderItemId());
            throw new RuntimeException("创建评论失败，请稍后再试。");
        }
        log.info("Review created successfully with reviewId: {} by userId: {}", review.getReviewId(), userId);

        int updatedOrderItems = orderItemsMapper.updateOrderItemReviewId(dto.getOrderItemId(), review.getReviewId());
        if (updatedOrderItems == 0) {
            log.error("createReview: Failed to update order item's reviewId. OrderItemId: {}, ReviewId: {}", dto.getOrderItemId(), review.getReviewId());
            throw new RuntimeException("更新订单评价状态失败。");
        }
        log.info("Order item {} reviewId updated to {}.", dto.getOrderItemId(), review.getReviewId());

        try {
            updateUniformRatingAndCount(dto.getUniformId());
        } catch (Exception e) {
            log.error("Error updating uniform rating and count for uniformId {}: {}", dto.getUniformId(), e.getMessage(), e);
        }

        SReview createdReview = reviewMapper.selectReviewById(review.getReviewId());
        return convertToReviewDisplayDto(createdReview, user);
    }

    @Override
    public PageResult<ReviewDisplayDto> getReviewsByUniformId(Long uniformId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        if (offset < 0) offset = 0;

        log.debug("Fetching reviews for uniformId: {}, pageNum: {}, pageSize: {}, offset: {}", uniformId, pageNum, pageSize, offset);
        List<SReview> reviews = reviewMapper.selectReviewsByUniformId(uniformId, offset, pageSize);
        long total = reviewMapper.countReviewsByUniformId(uniformId);
        log.debug("Found {} reviews in DB for uniformId: {}. Total count: {}", reviews.size(), uniformId, total);

        List<ReviewDisplayDto> dtoList = reviews.stream()
                .map(review -> {
                    UserAccount userAssociatedWithReview = null;
                    if (review.getUserId() != null && (review.getIsAnonymous() == null || !review.getIsAnonymous())) {
                        userAssociatedWithReview = userAccountMapper.selectUserById(review.getUserId());
                    }
                    return convertToReviewDisplayDto(review, userAssociatedWithReview);
                })
                .collect(Collectors.toList());

        log.debug("Converted to {} ReviewDisplayDto objects for uniformId: {}", dtoList.size(), uniformId);
        return new PageResult<>(dtoList, total, pageNum, pageSize);
    }

    @Override
    public PageResult<ReviewDisplayDto> getReviewsByUserId(Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        if (offset < 0) offset = 0;

        log.debug("Fetching reviews for userId: {}, pageNum: {}, pageSize: {}, offset: {}", userId, pageNum, pageSize, offset);
        List<SReview> reviews = reviewMapper.selectReviewsByUserId(userId, offset, pageSize);
        long total = reviewMapper.countReviewsByUserId(userId);
        log.debug("Found {} reviews in DB for userId: {}. Total count: {}", reviews.size(), userId, total);

        UserAccount currentUser = null;
        if (userId != null) {
            currentUser = userAccountMapper.selectUserById(userId);
            if (currentUser == null) {
                log.warn("Could not find UserAccount for userId: {} when fetching their reviews.", userId);
            }
        }
        final UserAccount finalCurrentUser = currentUser;

        List<ReviewDisplayDto> dtoList = reviews.stream()
                .map(review -> convertToReviewDisplayDto(review, finalCurrentUser))
                .collect(Collectors.toList());

        log.debug("Converted to {} ReviewDisplayDto objects for userId: {}", dtoList.size(), userId);
        return new PageResult<>(dtoList, total, pageNum, pageSize);
    }

    @Override
    @Transactional
    public ReviewDisplayDto updateReview(Long userId, Long reviewId, ReviewCreationRequestDto dto) {
        SReview existingReview = reviewMapper.selectReviewById(reviewId);
        if (existingReview == null) {
            throw new IllegalArgumentException("评论不存在。");
        }
        if (!existingReview.getUserId().equals(userId)) {
            throw new IllegalArgumentException("您没有权限修改此评论。");
        }

        existingReview.setRating(dto.getRating());
        existingReview.setContent(dto.getContent());
        existingReview.setIsAnonymous(dto.getIsAnonymous() != null ? dto.getIsAnonymous() : false);

        int updated = reviewMapper.updateReview(existingReview);
        if (updated == 0) {
            log.error("updateReview: Failed to update review with reviewId: {}", reviewId);
            throw new RuntimeException("更新评论失败，请稍后再试。");
        }
        log.info("Review updated successfully with reviewId: {}", reviewId);

        try {
            updateUniformRatingAndCount(existingReview.getUniformId());
        } catch (Exception e) {
            log.error("Error updating uniform rating and count for uniformId {}: {}", existingReview.getUniformId(), e.getMessage(), e);
        }

        UserAccount user = userAccountMapper.selectUserById(userId);
        // 确保更新后的 review 对象被传递，它包含了最新的 createTime
        SReview updatedReviewEntity = reviewMapper.selectReviewById(reviewId);
        return convertToReviewDisplayDto(updatedReviewEntity, user);
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        SReview existingReview = reviewMapper.selectReviewById(reviewId);
        if (existingReview == null) {
            log.warn("Attempted to delete a non-existent review with reviewId: {}", reviewId);
            throw new IllegalArgumentException("评论不存在。");
        }
        if (!existingReview.getUserId().equals(userId)) {
            throw new IllegalArgumentException("您没有权限删除此评论。");
        }

        Long uniformIdBeforeDelete = existingReview.getUniformId();

        int deleted = reviewMapper.deleteReviewById(reviewId);
        if (deleted == 0) {
            log.error("deleteReview: Failed to delete review with reviewId: {}", reviewId);
            throw new RuntimeException("删除评论失败，请稍后再试。");
        }
        log.info("Review deleted successfully with reviewId: {}", reviewId);

        if (uniformIdBeforeDelete != null) {
            try {
                updateUniformRatingAndCount(uniformIdBeforeDelete);
            } catch (Exception e) {
                log.error("Error updating uniform rating and count for uniformId {}: {}", uniformIdBeforeDelete, e.getMessage(), e);
            }
        }
    }

    @Override
    @Transactional
    public void updateUniformRatingAndCount(Long uniformId) {
        if (uniformId == null) {
            log.warn("updateUniformRatingAndCount called with null uniformId.");
            return;
        }
        List<SReview> reviews = reviewMapper.selectReviewsByUniformId(uniformId, 0, Integer.MAX_VALUE);
        double averageRating = 0.0;
        int reviewCount = 0;

        if (reviews != null && !reviews.isEmpty()) {
            reviewCount = reviews.size();
            double totalRatingSum = 0;
            for (SReview review : reviews) {
                if (review.getRating() != null) {
                    totalRatingSum += review.getRating();
                }
            }
            if (reviewCount > 0) {
                averageRating = totalRatingSum / reviewCount;
                averageRating = Math.round(averageRating * 100.0) / 100.0;
            }
        }

        int updatedRows = uniformMapper.updateUniformRatingAndCount(uniformId, averageRating, reviewCount);
        if (updatedRows > 0) {
            log.info("Successfully updated uniform {} rating to {} with {} reviews.", uniformId, averageRating, reviewCount);
        } else {
            log.warn("Failed to update uniform rating and count for uniformId {} (or no change needed/uniform not found).", uniformId);
        }
    }

    private ReviewDisplayDto convertToReviewDisplayDto(SReview review, UserAccount userAssociatedWithReview) {
        if (review == null) {
            return null;
        }

        ReviewDisplayDto dto = new ReviewDisplayDto();
        BeanUtils.copyProperties(review, dto); // reviewId, userId, uniformId, orderItemId, rating, content, isAnonymous, createTime

        // 填充 uniformName
        if (review.getUniformId() != null) {
            SUniform uniform = uniformMapper.selectSUniformById(review.getUniformId()); // 使用 SUniformMapper 中的方法
            if (uniform != null) {
                dto.setUniformName(uniform.getName()); // SUniform 实体有 getName()
            } else {
                dto.setUniformName("未知校服");
                log.warn("Could not find uniform with ID: {}", review.getUniformId());
            }
        } else {
            dto.setUniformName("N/A");
        }

        // 设置显示名称 (displayName)
        if (dto.getIsAnonymous() != null && dto.getIsAnonymous()) {
            dto.setDisplayName("匿名用户");
            dto.setUserId(null); // 匿名评价不应暴露实际用户ID给前端
        } else {
            // 非匿名评价
            if (userAssociatedWithReview != null && userAssociatedWithReview.getUserId().equals(review.getUserId())) {
                dto.setDisplayName(userAssociatedWithReview.getUserAccount());
            } else if (review.getUserId() != null) {
                UserAccount author = userAccountMapper.selectUserById(review.getUserId());
                if (author != null) {
                    dto.setDisplayName(author.getUserAccount());
                } else {
                    log.warn("Could not load author (userId: {}) for reviewId: {}", review.getUserId(), review.getReviewId());
                    dto.setDisplayName("用户 (ID: " + review.getUserId() + ")");
                }
            } else {
                log.error("Non-anonymous review (reviewId: {}) has null userId.", review.getReviewId());
                dto.setDisplayName("用户信息错误");
                dto.setUserId(null);
            }
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserReviewOrderItem(Long userId, Long orderItemId) {
        if (userId == null || orderItemId == null) {
            log.warn("canUserReviewOrderItem: userId or orderItemId is null.");
            return false;
        }
        SOrderItem orderItem = orderItemsMapper.selectOrderItemById(orderItemId);
        if (orderItem == null) {
            log.warn("canUserReviewOrderItem: OrderItem not found for id {}", orderItemId);
            return false;
        }
        SOrders order = ordersMapper.selectById(orderItem.getOrderId());
        if (order == null) {
            log.warn("canUserReviewOrderItem: Order (ID: {}) not found for orderItemId {}", orderItem.getOrderId(), orderItemId);
            return false;
        }
        if (!order.getUserId().equals(userId)) {
            log.warn("canUserReviewOrderItem: Order (ID: {}) does not belong to user {} for orderItemId {}",
                    orderItem.getOrderId(), userId, orderItemId);
            return false;
        }
        if (orderItem.getReviewId() != null && orderItem.getReviewId() > 0) {
            log.info("canUserReviewOrderItem: OrderItemId {} already has a review (review_id: {} in s_order_items).", orderItemId, orderItem.getReviewId());
            return false;
        }
        int reviewCountInReviewsTable = reviewMapper.checkIfUserReviewedOrderItem(userId, orderItemId);
        if (reviewCountInReviewsTable > 0) {
            log.warn("canUserReviewOrderItem: User {} has already an existing review in s_reviews for orderItemId {} (count: {}), though orderItem.reviewId might be null.",
                    userId, orderItemId, reviewCountInReviewsTable);
            return false;
        }
        log.info("User {} CAN review orderItem {}.", userId, orderItemId);
        return true;
    }
}
