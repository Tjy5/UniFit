package com.suios.admin.school.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.suios.admin.common.entity.AuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("s_schools")
@EqualsAndHashCode(callSuper = true)
public class SSchool extends AuditEntity {

    @TableId(type = IdType.AUTO)
    private Long schoolId;

    private String schoolName;

    private String schoolAddress;

    private String contactPerson;

    private String contactPhone;
}
