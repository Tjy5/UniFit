package com.authguard.usersystem.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SSchool {
    private Long schoolId;
    private String schoolName;
    // 如果下拉框还需要其他信息，可以添加，但通常只需要ID和名称
}
