package com.authguard.usersystem.dto;

import lombok.Data;
import java.util.Date;

@Data
public class ReviewDisplayDto {
    private Long reviewId;
    private Long userId;            // 实际的用户ID
    private String displayName;     // 显示的用户名 (如果是匿名，则为"匿名用户")
    private Long uniformId;
    private String uniformName;
    private Long orderItemId;
    private Byte rating;
    private String content;
    private Boolean isAnonymous;
    private Date createTime;
}
