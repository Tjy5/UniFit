package com.authguard.usersystem.service;

import com.authguard.usersystem.dto.ConfidenceResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class ConfidenceCalculator {

    public ConfidenceResult calculate(int usedDimensionCount,
                                      int matchedDimensionCount,
                                      BigDecimal topScore,
                                      BigDecimal scoreGap,
                                      boolean requiredDimensionsSafe,
                                      int boundaryTouches) {
        double normalizedTopScore = topScore == null ? 0D : topScore.doubleValue();
        double normalizedGap = scoreGap == null ? 0D : scoreGap.doubleValue();
        double completeness = usedDimensionCount <= 0 ? 0D : Math.min(usedDimensionCount, 6) / 6D * 20D;
        double matchRatio = usedDimensionCount <= 0 ? 0D : (double) matchedDimensionCount / usedDimensionCount;

        double score = normalizedTopScore * 0.6D
                + completeness
                + (matchRatio * 10D)
                + Math.min(normalizedGap, 20D) * 0.6D
                - (boundaryTouches * 4D)
                - (requiredDimensionsSafe ? 0D : 15D);

        if (usedDimensionCount <= 2) {
            score -= 8D;
        }

        int finalScore = BigDecimal.valueOf(Math.max(0D, Math.min(100D, score)))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        String level = finalScore >= 90 ? "HIGH" : finalScore >= 60 ? "MEDIUM" : "LOW";
        boolean lowConfidence = finalScore < 60;
        String message;
        if (lowConfidence) {
            message = "推荐把握度较低，建议参考尺码表手动选择";
        } else if ("HIGH".equals(level)) {
            message = "关键维度匹配稳定，推荐把握度较高";
        } else {
            message = "基础维度可支撑推荐，建议结合尺码表进一步确认";
        }
        return new ConfidenceResult(finalScore, level, message, lowConfidence);
    }
}
