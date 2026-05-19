package com.suios.admin.review.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.common.enums.ReviewStatus;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.page.PageResult;
import com.suios.admin.review.entity.SReview;
import com.suios.admin.review.mapper.SReviewMapper;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SReviewService {

    private final SReviewMapper reviewMapper;

    public SReviewService(SReviewMapper reviewMapper) {
        this.reviewMapper = reviewMapper;
    }

    public PageResult<SReview> list(long pageNum, long pageSize, Long uniformId, Integer status) {
        IPage<SReview> page = reviewMapper.selectPageWithDetails(new Page<>(pageNum, pageSize), uniformId, status);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<SReview> listAll(Long uniformId, Integer status) {
        return reviewMapper.selectListWithDetails(uniformId, status);
    }

    public SReview getById(Long reviewId) {
        SReview review = reviewMapper.selectDetailById(reviewId);
        if (review == null) {
            throw new BizException("评论不存在");
        }
        return review;
    }

    public void audit(Long reviewId, Integer status) {
        if (!isValidStatus(status) || ReviewStatus.PENDING.getCode().equals(status)) {
            throw new BizException("评论审核状态不合法");
        }
        SReview review = getById(reviewId);
        review.setStatus(status);
        reviewMapper.updateById(review);
    }

    private boolean isValidStatus(Integer status) {
        return Arrays.stream(ReviewStatus.values())
                .anyMatch(item -> item.getCode().equals(status));
    }
}
