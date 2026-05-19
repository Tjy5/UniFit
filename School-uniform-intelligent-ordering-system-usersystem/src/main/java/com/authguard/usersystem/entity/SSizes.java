package com.authguard.usersystem.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SSizes {
    private Long id;            // ID
    private String sizeName;    // 名称
    private Long minHeight;     // 最低身高（cm）
    private Long maxHeight;     // 最高身高（cm）
    private BigDecimal minWeight; // 最小体重（kg）
    private BigDecimal maxWeight; // 最大体重（kg）
    private BigDecimal minChest; // 最小胸围（cm）
    private BigDecimal maxChest; // 最大胸围（cm）
    private BigDecimal minWaist; // 最小腰围（cm）
    private BigDecimal maxWaist; // 最大腰围（cm）
    private BigDecimal minHip; // 最小臀围（cm）
    private BigDecimal maxHip; // 最大臀围（cm）
    private BigDecimal minShoulder; // 最小肩宽（cm）
    private BigDecimal maxShoulder; // 最大肩宽（cm）
    private String createBy;    // 创建者
    private Date createTime;    // 创建时间
    private String updateBy;    // 更新者
    private Date updateTime;    // 更新时间
    private String remark;      // 备注
}
