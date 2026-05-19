package com.suios.admin.uniform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.suios.admin.common.entity.AuditEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("s_uniform")
@EqualsAndHashCode(callSuper = true)
public class SUniform extends AuditEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String intro;

    private String image;

    private BigDecimal price;

    private Long status;

    private BigDecimal averageRating;

    private Integer reviewCount;

    private Long schoolId;

    private Long gradeId;

    @TableField(exist = false)
    private String schoolName;

    @TableField(exist = false)
    private String gradeName;
}
