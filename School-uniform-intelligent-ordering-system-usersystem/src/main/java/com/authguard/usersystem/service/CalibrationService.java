package com.authguard.usersystem.service;

import com.authguard.usersystem.dto.CalibrationBundle;
import com.authguard.usersystem.dto.FeedbackAggregation;
import com.authguard.usersystem.entity.CalibrationParams;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CalibrationService {
    CalibrationBundle loadCalibrationSet(Long schoolId, Long uniformId, Collection<Long> candidateSizeIds);

    Map<Long, BigDecimal> applyScoreOffsets(Map<Long, BigDecimal> baseScores, CalibrationBundle bundle);

    default Map<Long, BigDecimal> applyScoreOffsets(Map<Long, BigDecimal> baseScores,
                                                    CalibrationBundle bundle,
                                                    Map<Long, Boolean> requiredDimensionsSafeBySizeId) {
        return applyScoreOffsets(baseScores, bundle);
    }

    List<FeedbackAggregation> aggregateFeedback(LocalDate startDate, LocalDate endDate);

    List<CalibrationParams> generateCalibrationParams(List<FeedbackAggregation> aggregations);

    void saveGeneratedParams(List<CalibrationParams> params, String operator, String reason);

    void clearCache();
}
