package com.suios.admin.grade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.suios.admin.common.entity.AuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("s_grades")
@EqualsAndHashCode(callSuper = true)
public class SGrade extends AuditEntity {

    @TableId(type = IdType.AUTO)
    private Long gradeId;

    private String gradeName;

    private Long schoolId;

    @TableField(exist = false)
    private String schoolName;
}
