package com.suios.admin.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("s_reviews")
public class SReview {

    @TableId(value = "review_id", type = IdType.AUTO)
    private Long reviewId;

    private Long userId;

    private Long uniformId;

    private Long orderItemId;

    private Integer rating;

    private String content;

    private String images;

    private Integer status;

    private Boolean isAnonymous;

    private LocalDateTime createTime;

    @TableField(exist = false)
    private String userAccount;

    @TableField(exist = false)
    private String uniformName;
}
