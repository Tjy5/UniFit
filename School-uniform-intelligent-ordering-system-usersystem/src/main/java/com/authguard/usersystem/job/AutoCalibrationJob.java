package com.authguard.usersystem.job;

import com.authguard.usersystem.config.CalibrationProperties;
import com.authguard.usersystem.dto.FeedbackAggregation;
import com.authguard.usersystem.entity.CalibrationParams;
import com.authguard.usersystem.service.CalibrationService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoCalibrationJob {

    private final CalibrationService calibrationService;
    private final CalibrationProperties calibrationProperties;

    @Scheduled(cron = "${calibration.auto-job.cron:0 0 2 * * ?}")
    public void run() {
        if (!calibrationProperties.isEnabled() || !calibrationProperties.getAutoJob().isEnabled()) {
            log.debug("Auto calibration job skipped because calibration is disabled");
            return;
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(Math.max(1, calibrationProperties.getWindowDays()));
        log.info("Starting auto calibration job for window {} -> {}", startDate, endDate);

        try {
            List<FeedbackAggregation> aggregations = calibrationService.aggregateFeedback(startDate, endDate);
            long totalGenerated = 0;
            long activeCount = 0;
            long observingCount = 0;
            for (String scopeType : List.of("GLOBAL", "SCHOOL", "PRODUCT")) {
                List<FeedbackAggregation> scopedAggregations = aggregations.stream()
                        .filter(aggregation -> scopeType.equals(aggregation.getScopeType()))
                        .toList();
                if (scopedAggregations.isEmpty()) {
                    continue;
                }
                try {
                    List<CalibrationParams> generatedParams = calibrationService.generateCalibrationParams(scopedAggregations);
                    calibrationService.saveGeneratedParams(generatedParams, "auto-job", "Scheduled calibration refresh for " + scopeType);
                    totalGenerated += generatedParams.size();
                    activeCount += generatedParams.stream().filter(param -> "ACTIVE".equals(param.getStatus())).count();
                    observingCount += generatedParams.stream().filter(param -> "OBSERVING".equals(param.getStatus())).count();
                    log.info("Auto calibration scope {} completed: aggregations={}, generated={}", scopeType, scopedAggregations.size(), generatedParams.size());
                } catch (Exception scopeEx) {
                    log.error("Auto calibration scope {} failed", scopeType, scopeEx);
                }
            }
            log.info(
                    "Auto calibration job completed: aggregations={}, generated={}, active={}, observing={}",
                    aggregations.size(),
                    totalGenerated,
                    activeCount,
                    observingCount
            );
        } catch (Exception ex) {
            log.error("Auto calibration job failed", ex);
        }
    }
}
