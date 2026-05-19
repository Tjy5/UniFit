package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.CalibrationParams;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CalibrationParamsMapper {
    int insert(CalibrationParams calibrationParams);

    int update(CalibrationParams calibrationParams);

    CalibrationParams selectById(@Param("paramId") Long paramId);

    List<CalibrationParams> selectActiveByScope(@Param("scopeType") String scopeType,
                                                @Param("scopeId") String scopeId,
                                                @Param("targetSizeId") Long targetSizeId,
                                                @Param("now") Date now);

    Integer selectLatestVersion(@Param("scopeType") String scopeType,
                                @Param("scopeId") String scopeId,
                                @Param("targetSizeId") Long targetSizeId);
}
