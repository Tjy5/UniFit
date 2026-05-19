package com.suios.admin.guide.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.suios.admin.common.entity.AuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("s_style_guides")
@EqualsAndHashCode(callSuper = true)
public class SStyleGuides extends AuditEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long uniformId;

    private String title;

    private String content;

    private String image;

    private Long status;

    @TableField(exist = false)
    private String uniformName;
}
