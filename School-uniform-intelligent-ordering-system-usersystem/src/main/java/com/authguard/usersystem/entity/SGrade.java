package com.authguard.usersystem.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SGrade {
    private Long gradeId;
    private String gradeName;
    private Long schoolId; // 对应 s_grades 表中的 school_id 列
    // 如果下拉框或列表中需要显示学校名称，可以添加 transient String schoolName;
    // 但对于纯粹的选项列表，通常不需要在年级实体中再带学校名称
}
