package com.authguard.usersystem.dto; // 确保包名与你的项目结构一致

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data // 使用 Lombok 自动生成 getter, setter, toString, equals, hashCode
public class SWishlistDetailDto {

    // 来自 s_wishlists 表的字段
    private Long wishlistId;    // 收藏记录的ID
    private Long userId;        // 用户ID
    private Long uniformId;     // 收藏的校服ID
    private Date addedTime;     // 添加收藏的时间

    // 来自 s_uniform 表 (以及通过 s_uniform 关联的 s_schools, s_grades 表) 的字段
    private String uniformName;     // 校服名称
    private BigDecimal uniformPrice;  // 校服价格 (使用 BigDecimal 更精确)
    private String uniformImage;    // 校服图片路径 (通常是主图或逗号分隔的路径字符串)
    private String schoolName;      // 校服所属学校名称
    private String gradeName;       // 校服所属年级名称

    // private String uniformIntro; // 校服介绍
    // private Integer uniformStatus; // 校服状态
}
