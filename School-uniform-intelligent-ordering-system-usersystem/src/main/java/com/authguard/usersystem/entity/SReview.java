package com.authguard.usersystem.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SReview {

    private Long reviewId;
    private Long userId;
    private Long uniformId;
    private Long orderItemId;
    private Byte rating;
    private String content;
    private Boolean isAnonymous;
    private Date createTime;

    // 如果前端展示评论时需要用户信息和校服信息，
    // 通常会在Service层组装DTO，而不是直接在SReview实体中放这些对象。
    // 例如，可以创建一个 SReviewVo 或 SReviewDto，包含 SUser 和 SUniform 的部分信息。
}
