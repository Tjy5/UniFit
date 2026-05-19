package com.authguard.usersystem.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date; // 假设你确实需要这些审计字段在实体中

@Data
public class SUniform {
    private Long id;            // ID
    private String name;        // 名称
    private Long schoolId;      // 学校ID (对应数据库 s_uniform.school_id)
    private Long gradeId;       // 年级ID (对应数据库 s_uniform.grade_id)
    private String categoryKey;  // 校服品类键

    private transient String schoolName; // 学校名称 (用于展示, 来自JOIN)
    private transient String gradeName;  // 年级名称 (用于展示, 来自JOIN)

    private String intro;       // 介绍
    private String image;       // 图片
    private BigDecimal price;   // 价格
    private Long status;        // 状态
    private String createBy;    // 创建者
    private Date createTime;    // 创建时间
    private String updateBy;    // 更新者
    private Date updateTime;    // 更新时间
    private String remark;      // 备注
    private Double averageRating; // DECIMAL(3,2) 对应 Double
    private Integer reviewCount;  // INT UNSIGNED 对应 Integer
}
