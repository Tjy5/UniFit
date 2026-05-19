package com.authguard.usersystem.entity;

import lombok.Data;

@Data
public class SAddress {
    private Long id;              // 地址主键ID
    private Long userId;          // 用户ID (关联 user_account 表)
    private String recipientName; // 收货人姓名
    private String phoneNumber;   // 收货人手机号
    private String province;      // 省份
    private String city;          // 城市
    private String district;      // 区/县
    private String streetAddress; // 详细街道地址
    private Boolean isDefault;    // 是否为默认地址 (使用 Boolean, MyBatis 会处理 0/1)
}
