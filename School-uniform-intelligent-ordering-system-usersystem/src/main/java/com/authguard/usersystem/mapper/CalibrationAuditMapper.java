package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.CalibrationAudit;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CalibrationAuditMapper {
    int insert(CalibrationAudit calibrationAudit);

    List<CalibrationAudit> selectByParamId(@Param("paramId") Long paramId);

    Long selectLatestAuditRevision();
}
