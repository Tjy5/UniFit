package com.suios.admin.review.controller;

import com.suios.admin.common.page.PageResult;
import com.suios.admin.common.result.R;
import com.suios.admin.common.util.ExcelUtils;
import com.suios.admin.common.annotation.OperLog;
import com.suios.admin.review.dto.ReviewAuditRequest;
import com.suios.admin.review.entity.SReview;
import com.suios.admin.review.service.SReviewService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class SReviewController {

    private final SReviewService reviewService;

    @GetMapping("/list")
    public R<PageResult<SReview>> list(@RequestParam(defaultValue = "1") long pageNum,
                                       @RequestParam(defaultValue = "10") long pageSize,
                                       @RequestParam(required = false) Long uniformId,
                                       @RequestParam(required = false) Integer status) {
        return R.success(reviewService.list(pageNum, pageSize, uniformId, status));
    }

    @GetMapping("/{reviewId}")
    public R<SReview> getInfo(@PathVariable Long reviewId) {
        return R.success(reviewService.getById(reviewId));
    }

    @PutMapping("/{reviewId}/audit")
    @OperLog(module = "评论审核", operation = "AUDIT")
    public R<Void> audit(@PathVariable Long reviewId, @RequestBody ReviewAuditRequest request) {
        reviewService.audit(reviewId, request.getStatus());
        return R.success("评论审核成功");
    }

    @PostMapping("/export")
    @OperLog(module = "评论审核", operation = "EXPORT")
    public void export(@RequestParam(required = false) Long uniformId,
                       @RequestParam(required = false) Integer status,
                       HttpServletResponse response) throws IOException {
        List<SReview> rows = reviewService.listAll(uniformId, status);
        List<List<?>> dataRows = new ArrayList<>();
        for (SReview item : rows) {
            dataRows.add(List.of(
                    item.getReviewId(),
                    item.getUserAccount(),
                    item.getUniformName(),
                    item.getRating(),
                    item.getStatus(),
                    item.getCreateTime()
            ));
        }
        ExcelUtils.export(
                "reviews",
                List.of("评论ID", "用户账号", "校服名称", "评分", "审核状态", "评论时间"),
                dataRows,
                response
        );
    }
}
