package com.authguard.usersystem.service;

import com.authguard.usersystem.entity.SGrade;
import java.util.List;

public interface ISGradeService {
    /**
     * 根据学校ID获取年级选项列表
     * @param schoolId 学校ID，如果为null或0，则可以考虑返回所有年级或空列表
     * @return 年级列表
     */
    List<SGrade> getGradeOptionsBySchoolId(Long schoolId);

    /**
     * 获取所有年级选项列表 (可选)
     * @return 所有年级列表
     */
    List<SGrade> getAllGradeOptions();
}
