package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.SUserActivityLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ISUserActivityLogMapper {
    /**
     * 插入用户行为日志
     * @param log 日志对象
     * @return 影响行数
     */
    int insertLog(SUserActivityLog log);
}
