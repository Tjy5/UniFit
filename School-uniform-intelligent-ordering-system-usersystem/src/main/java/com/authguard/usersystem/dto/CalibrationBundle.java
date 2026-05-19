package com.authguard.usersystem.dto;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

@Data
public class CalibrationBundle {
    private Map<Long, BigDecimal> offsetsBySizeId = new LinkedHashMap<>();
    private Map<Long, String> provenanceBySizeId = new LinkedHashMap<>();
    private Map<Long, Map<String, Object>> detailsBySizeId = new LinkedHashMap<>();

    public static CalibrationBundle empty(Collection<Long> candidateSizeIds) {
        CalibrationBundle bundle = new CalibrationBundle();
        if (candidateSizeIds != null) {
            for (Long candidateSizeId : candidateSizeIds) {
                bundle.offsetsBySizeId.put(candidateSizeId, BigDecimal.ZERO);
                bundle.provenanceBySizeId.put(candidateSizeId, "NONE");
                bundle.detailsBySizeId.put(candidateSizeId, Map.of());
            }
        }
        return bundle;
    }

    public Map<Long, BigDecimal> immutableOffsets() {
        return Collections.unmodifiableMap(offsetsBySizeId);
    }

    public boolean hasNonZeroAdjustment() {
        return offsetsBySizeId.values().stream()
                .filter(value -> value != null)
                .anyMatch(value -> value.compareTo(BigDecimal.ZERO) != 0);
    }

    public boolean hasParameterHit() {
        return hasNonZeroAdjustment()
                || detailsBySizeId.values().stream().anyMatch(detail -> detail != null
                && (detail.containsKey("paramId") || Boolean.TRUE.equals(detail.get("parameterHit"))));
    }

    public long appliedCandidateCount() {
        return detailsBySizeId.values().stream()
                .filter(detail -> detail != null && Boolean.TRUE.equals(detail.get("applied")))
                .count();
    }

    public long safetyGateSkipCount() {
        return detailsBySizeId.values().stream()
                .filter(detail -> detail != null && Boolean.TRUE.equals(detail.get("safetyGateSkipped")))
                .count();
    }
}
