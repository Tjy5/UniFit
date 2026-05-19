package com.authguard.usersystem.entity;

import lombok.Data;

import java.util.Date;

@Data
public class UserAccount {
    private Long userId;
    private String userAccount;
    private String userPassword;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}
