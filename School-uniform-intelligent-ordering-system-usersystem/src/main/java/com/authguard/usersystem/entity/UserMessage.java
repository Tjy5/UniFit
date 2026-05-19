package com.authguard.usersystem.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class UserMessage {
    private Long messageId;            // ID
    private Long messageUserId;        // 用户ID
    private String messageUserName;    // 用户姓名
    private Integer messageUserAge;    // 用户年龄
    private String messageUserSex;     // 用户性别
    private String messageUserAddress; // 用户地址
    private BigDecimal height;         // 身高 (decimal(10,2))
    private BigDecimal weight;         // 体重 (decimal(10,2))
    private BigDecimal chest;          // 胸围 (cm)
    private BigDecimal waist;          // 腰围 (cm)
    private BigDecimal hip;            // 臀围 (cm)
    private BigDecimal shoulder;       // 肩宽 (cm)

    private Long schoolId;             // 学校ID
    private Long gradeId;              // 年级ID

    private transient String schoolName;
    private transient String gradeName;

    private String createBy;           // 创建者
    private Date createTime;           // 创建时间
    private String updateBy;           // 更新者
    private Date updateTime;           // 更新时间
}
