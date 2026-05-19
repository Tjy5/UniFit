package com.authguard.usersystem.entity;

import lombok.Data;

import java.util.Date;

@Data
public class SStyleGuides {
    private Long id;            // ID
    private Long uniformId;     // 校服ID
    private String title;       // 标题
    private String content;     // 内容
    private String image;       // 图片
    private Long status;        // 状态
    private String createBy;    // 创建者
    private Date createTime;    // 创建时间
    private String updateBy;    // 更新者
    private Date updateTime;    // 更新时间
}