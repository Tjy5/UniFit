package com.authguard.usersystem.mapper;

import com.authguard.usersystem.dto.CalibrationFeedbackRecord;
import com.authguard.usersystem.entity.SizeFeedback;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SizeFeedbackMapper {
    SizeFeedback selectByOrderItemId(@Param("orderItemId") Long orderItemId);

    int insert(SizeFeedback feedback);

    int update(SizeFeedback feedback);

    List<CalibrationFeedbackRecord> selectCalibrationFeedbackWindow(@Param("startTime") Date startTime,
                                                                    @Param("endTime") Date endTime);
}
