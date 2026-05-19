package com.authguard.usersystem.entity;

import lombok.Data;

import java.util.Date;

@Data
public class SWishlist {
    private Long wishlistId;        // 主键ID
    private Long userId;            // 用户ID
    private Long uniformId;         // 校服ID
    private Date addedTime;         // 添加时间
}
