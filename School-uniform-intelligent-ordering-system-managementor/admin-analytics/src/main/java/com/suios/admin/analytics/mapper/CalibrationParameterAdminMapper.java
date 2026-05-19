package com.suios.admin.analytics.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.analytics.dto.CalibrationAuditDto;
import com.suios.admin.analytics.dto.CalibrationImpactLogRow;
import com.suios.admin.analytics.dto.CalibrationParamDetailDto;
import com.suios.admin.analytics.dto.CalibrationParamListItemDto;
import com.suios.admin.analytics.dto.CalibrationParamVersionDto;
import com.suios.admin.analytics.entity.CalibrationAuditRecord;
import com.suios.admin.analytics.entity.CalibrationParamRecord;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CalibrationParameterAdminMapper {

    IPage<CalibrationParamListItemDto> selectParamPage(Page<CalibrationParamListItemDto> page,
                                                       @Param("scopeType") String scopeType,
                                                       @Param("scopeId") String scopeId,
                                                       @Param("targetSizeId") Long targetSizeId,
                                                       @Param("enabled") Boolean enabled,
                                                       @Param("status") String status,
                                                       @Param("confidenceLevel") String confidenceLevel,
                                                       @Param("version") Integer version);

    CalibrationParamDetailDto selectParamDetail(@Param("paramId") Long paramId);

    CalibrationParamRecord selectParamRecord(@Param("paramId") Long paramId);

    List<CalibrationParamVersionDto> selectVersionsByIdentity(@Param("paramId") Long paramId);

    List<CalibrationAuditDto> selectAuditHistoryByIdentity(@Param("paramId") Long paramId);

    Long selectLatestAuditRevision();

    Integer selectLatestVersionByIdentity(@Param("scopeType") String scopeType,
                                          @Param("scopeId") String scopeId,
                                          @Param("targetSizeId") Long targetSizeId,
                                          @Param("calibrationType") String calibrationType);

    List<CalibrationParamRecord> selectEffectiveRecordsByIdentity(@Param("scopeType") String scopeType,
                                                                  @Param("scopeId") String scopeId,
                                                                  @Param("targetSizeId") Long targetSizeId,
                                                                  @Param("calibrationType") String calibrationType,
                                                                  @Param("now") Date now);

    int updateParamRecord(CalibrationParamRecord record);

    int insertParamRecord(CalibrationParamRecord record);

    int insertAuditRecord(CalibrationAuditRecord record);

    List<CalibrationImpactLogRow> selectImpactLogs(@Param("schoolId") Long schoolId,
                                                   @Param("uniformId") Long uniformId,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime,
                                                   @Param("paramId") Long paramId,
                                                   @Param("version") Integer version,
                                                   @Param("limit") Integer limit);
}
