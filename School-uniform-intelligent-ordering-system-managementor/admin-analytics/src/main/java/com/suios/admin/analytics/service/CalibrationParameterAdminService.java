package com.suios.admin.analytics.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suios.admin.analytics.dto.CalibrationAuditDto;
import com.suios.admin.analytics.dto.CalibrationImpactLogRow;
import com.suios.admin.analytics.dto.CalibrationImpactParamHitDto;
import com.suios.admin.analytics.dto.CalibrationImpactRecordDto;
import com.suios.admin.analytics.dto.CalibrationImpactResponseDto;
import com.suios.admin.analytics.dto.CalibrationImpactSummaryDto;
import com.suios.admin.analytics.dto.CalibrationParamDetailDto;
import com.suios.admin.analytics.dto.CalibrationParamListItemDto;
import com.suios.admin.analytics.dto.CalibrationParamVersionDto;
import com.suios.admin.analytics.dto.CalibrationRollbackRequest;
import com.suios.admin.analytics.entity.CalibrationAuditRecord;
import com.suios.admin.analytics.entity.CalibrationParamRecord;
import com.suios.admin.analytics.mapper.CalibrationParameterAdminMapper;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.page.PageResult;
import com.suios.admin.common.security.AuthContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalibrationParameterAdminService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final CalibrationParameterAdminMapper calibrationMapper;
    private final AuthContext authContext;
    private final ObjectMapper objectMapper;

    @Value("${analytics.max-page-size:200}")
    private int maxPageSize;

    public CalibrationParameterAdminService(CalibrationParameterAdminMapper calibrationMapper,
                                            AuthContext authContext,
                                            ObjectMapper objectMapper) {
        this.calibrationMapper = calibrationMapper;
        this.authContext = authContext;
        this.objectMapper = objectMapper;
    }

    public PageResult<CalibrationParamListItemDto> list(long pageNum,
                                                        long pageSize,
                                                        String scopeType,
                                                        String scopeId,
                                                        Long targetSizeId,
                                                        Boolean enabled,
                                                        String status,
                                                        String confidenceLevel,
                                                        Integer version) {
        requireAdminAuthenticated();
        IPage<CalibrationParamListItemDto> page = calibrationMapper.selectParamPage(
                new Page<>(Math.max(1L, pageNum), clampPageSize(pageSize)),
                blankToNull(scopeType),
                blankToNull(scopeId),
                targetSizeId,
                enabled,
                blankToNull(status),
                blankToNull(confidenceLevel),
                version
        );
        List<CalibrationParamListItemDto> rows = page.getRecords() == null
                ? List.of()
                : page.getRecords().stream().map(this::normalizeParamDto).toList();
        return new PageResult<>(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public CalibrationParamDetailDto detail(Long paramId) {
        requireAdminAuthenticated();
        CalibrationParamDetailDto detail = calibrationMapper.selectParamDetail(requireParamId(paramId));
        if (detail == null) {
            throw new BizException("校准参数不存在");
        }
        return normalizeParamDto(detail);
    }

    public List<CalibrationParamVersionDto> versions(Long paramId) {
        requireAdminAuthenticated();
        ensureParamExists(paramId);
        return calibrationMapper.selectVersionsByIdentity(paramId)
                .stream()
                .map(this::normalizeParamDto)
                .toList();
    }

    public List<CalibrationAuditDto> audits(Long paramId) {
        requireAdminAuthenticated();
        ensureParamExists(paramId);
        return calibrationMapper.selectAuditHistoryByIdentity(paramId)
                .stream()
                .map(this::normalizeAuditDto)
                .toList();
    }

    public CalibrationImpactResponseDto impact(Long paramId,
                                               Integer version,
                                               Long schoolId,
                                               Long uniformId,
                                               Long sizeId,
                                               LocalDate startDate,
                                               LocalDate endDate,
                                               Integer limit,
                                               Integer offset) {
        requireAdminAuthenticated();
        LocalDateRange range = optionalDateRange(startDate, endDate);
        int safeLimit = (int) clampPageSize(limit == null ? 20 : limit);
        int safeOffset = Math.max(0, offset == null ? 0 : offset);
        int fetchLimit = Math.min(Math.max(safeLimit + safeOffset, safeLimit), Math.max(maxPageSize, safeLimit));

        List<CalibrationImpactRecordDto> matched = calibrationMapper.selectImpactLogs(
                        schoolId,
                        uniformId,
                        range == null ? null : range.startTime(),
                        range == null ? null : range.endTime(),
                        paramId,
                        version,
                        fetchLimit
                )
                .stream()
                .map(row -> toImpactRecord(row, paramId, version, sizeId))
                .filter(Objects::nonNull)
                .toList();

        CalibrationImpactResponseDto response = new CalibrationImpactResponseDto();
        response.setSummary(summarizeImpact(matched));
        response.setTotal(matched.size());
        response.setLimit(safeLimit);
        response.setOffset(safeOffset);
        response.setRecords(matched.stream().skip(safeOffset).limit(safeLimit).toList());
        return response;
    }

    @Transactional
    public CalibrationParamDetailDto enable(Long paramId, String reason) {
        requireAdminAuthenticated();
        String safeReason = requireReason(reason);
        CalibrationParamRecord record = loadRecord(paramId);
        if ("OBSERVING".equalsIgnoreCase(record.getStatus())) {
            throw new BizException("观察状态参数样本不足，不能直接启用");
        }
        String oldValue = writeJson(record);
        Date now = new Date();
        record.setEnabled(true);
        record.setStatus("ACTIVE");
        if (record.getEffectiveFrom() == null || record.getEffectiveFrom().after(now)) {
            record.setEffectiveFrom(now);
        }
        record.setEffectiveUntil(null);
        record.setUpdateBy(operator());
        record.setUpdateTime(now);
        calibrationMapper.updateParamRecord(record);
        writeAudit(record.getParamId(), "ENABLE", oldValue, writeJson(record), safeReason, now);
        return detail(record.getParamId());
    }

    @Transactional
    public CalibrationParamDetailDto disable(Long paramId, String reason) {
        requireAdminAuthenticated();
        String safeReason = requireReason(reason);
        CalibrationParamRecord record = loadRecord(paramId);
        String oldValue = writeJson(record);
        Date now = new Date();
        record.setEnabled(false);
        record.setStatus("DISABLED");
        record.setEffectiveUntil(now);
        record.setUpdateBy(operator());
        record.setUpdateTime(now);
        calibrationMapper.updateParamRecord(record);
        writeAudit(record.getParamId(), "DISABLE", oldValue, writeJson(record), safeReason, now);
        return detail(record.getParamId());
    }

    @Transactional
    public CalibrationParamDetailDto rollback(Long paramId, CalibrationRollbackRequest request) {
        requireAdminAuthenticated();
        String safeReason = requireReason(request == null ? null : request.getReason());
        CalibrationParamRecord reference = loadRecord(paramId);
        Long sourceParamId = request == null || request.getSourceParamId() == null ? paramId : request.getSourceParamId();
        CalibrationParamRecord source = loadRecord(sourceParamId);
        if (!sameIdentity(reference, source)) {
            throw new BizException("只能回滚到同一参数身份下的历史版本");
        }

        Date now = new Date();
        List<CalibrationParamRecord> activeRecords = calibrationMapper.selectEffectiveRecordsByIdentity(
                reference.getScopeType(),
                reference.getScopeId(),
                reference.getTargetSizeId(),
                reference.getCalibrationType(),
                now
        );
        List<Map<String, Object>> deactivated = new ArrayList<>();
        for (CalibrationParamRecord activeRecord : activeRecords) {
            String before = writeJson(activeRecord);
            activeRecord.setEnabled(false);
            activeRecord.setStatus("DISABLED");
            activeRecord.setEffectiveUntil(now);
            activeRecord.setUpdateBy(operator());
            activeRecord.setUpdateTime(now);
            calibrationMapper.updateParamRecord(activeRecord);
            deactivated.add(Map.of(
                    "old", readJson(before),
                    "new", readJson(writeJson(activeRecord))
            ));
        }

        Integer maxVersion = calibrationMapper.selectLatestVersionByIdentity(
                reference.getScopeType(),
                reference.getScopeId(),
                reference.getTargetSizeId(),
                reference.getCalibrationType()
        );
        CalibrationParamRecord newRecord = copyForRollback(source, (maxVersion == null ? 0 : maxVersion) + 1, now);
        calibrationMapper.insertParamRecord(newRecord);

        Map<String, Object> oldSnapshot = new LinkedHashMap<>();
        oldSnapshot.put("sourceParam", readJson(writeJson(source)));
        oldSnapshot.put("deactivated", deactivated);
        Map<String, Object> newSnapshot = new LinkedHashMap<>();
        newSnapshot.put("newParam", readJson(writeJson(newRecord)));
        newSnapshot.put("sourceParamId", source.getParamId());
        newSnapshot.put("sourceVersion", source.getVersion());
        writeAudit(newRecord.getParamId(), "ROLLBACK", writeJson(oldSnapshot), writeJson(newSnapshot), safeReason, now);
        return detail(newRecord.getParamId());
    }

    private CalibrationParamRecord copyForRollback(CalibrationParamRecord source, int nextVersion, Date now) {
        CalibrationParamRecord copy = new CalibrationParamRecord();
        copy.setScopeType(source.getScopeType());
        copy.setScopeId(source.getScopeId());
        copy.setTargetSizeId(source.getTargetSizeId());
        copy.setCalibrationType(source.getCalibrationType());
        copy.setAdjustmentValue(source.getAdjustmentValue());
        copy.setIsManual(true);
        copy.setEnabled(true);
        copy.setStatus("ACTIVE");
        copy.setSampleSize(source.getSampleSize());
        copy.setUniqueUserCount(source.getUniqueUserCount());
        copy.setFeedbackDistribution(source.getFeedbackDistribution());
        copy.setConfidenceLevel(source.getConfidenceLevel());
        copy.setEffectiveFrom(now);
        copy.setEffectiveUntil(null);
        copy.setVersion(nextVersion);
        copy.setCreateBy(operator());
        copy.setCreateTime(now);
        copy.setUpdateBy(operator());
        copy.setUpdateTime(now);
        return copy;
    }

    private CalibrationParamRecord loadRecord(Long paramId) {
        CalibrationParamRecord record = calibrationMapper.selectParamRecord(requireParamId(paramId));
        if (record == null) {
            throw new BizException("校准参数不存在");
        }
        return record;
    }

    private void ensureParamExists(Long paramId) {
        loadRecord(paramId);
    }

    private Long requireParamId(Long paramId) {
        if (paramId == null) {
            throw new BizException("校准参数ID不能为空");
        }
        return paramId;
    }

    private void writeAudit(Long paramId, String action, String oldValue, String newValue, String reason, Date now) {
        CalibrationAuditRecord audit = new CalibrationAuditRecord();
        audit.setParamId(paramId);
        audit.setAction(action);
        audit.setOldValue(oldValue);
        audit.setNewValue(newValue);
        audit.setReason(reason);
        audit.setOperator(operator());
        audit.setCreateTime(now);
        calibrationMapper.insertAuditRecord(audit);
    }

    private CalibrationImpactRecordDto toImpactRecord(CalibrationImpactLogRow row,
                                                      Long paramId,
                                                      Integer version,
                                                      Long sizeId) {
        Map<String, Object> details = readMap(row.getCalibrationDetails());
        if (details.isEmpty()) {
            return null;
        }
        List<CalibrationImpactParamHitDto> hits = extractParamHits(details);
        boolean hasNewMetadata = hits.stream().anyMatch(hit -> hit.getParamId() != null || hit.getVersion() != null);
        if (paramId != null && hits.stream().noneMatch(hit -> Objects.equals(hit.getParamId(), paramId))) {
            return null;
        }
        if (version != null && hits.stream().noneMatch(hit -> Objects.equals(hit.getVersion(), version))) {
            return null;
        }
        if (sizeId != null && hits.stream().noneMatch(hit -> Objects.equals(hit.getSizeId(), sizeId))) {
            return null;
        }

        CalibrationImpactRecordDto dto = new CalibrationImpactRecordDto();
        dto.setLogId(row.getLogId());
        dto.setCreateTime(row.getCreateTime());
        dto.setSchoolId(row.getSchoolId());
        dto.setSchoolName(row.getSchoolName());
        dto.setUniformId(row.getUniformId());
        dto.setUniformName(row.getUniformName());
        dto.setRecommendedSizeId(row.getRecommendedSizeId());
        dto.setRecommendedSizeName(row.getRecommendedSizeName());
        dto.setBaseBestSizeId(asLong(details.get("baseBestSizeId")));
        dto.setBaseBestSizeName(asString(details.get("baseBestSizeName")));
        dto.setBaseBestScore(asDecimal(details.get("baseBestScore")));
        dto.setCalibratedBestSizeId(asLong(details.get("calibratedBestSizeId")));
        dto.setCalibratedBestSizeName(asString(details.get("calibratedBestSizeName")));
        dto.setCalibratedBestScore(asDecimal(details.get("calibratedBestScore")));
        dto.setFinalBestSizeId(asLong(details.get("finalBestSizeId")));
        dto.setFinalBestSizeName(asString(details.get("finalBestSizeName")));
        dto.setFinalBestScore(asDecimal(details.get("finalBestScore")));
        dto.setScoreDelta(delta(dto.getCalibratedBestScore(), dto.getBaseBestScore()));
        dto.setAttributionStatus(hasNewMetadata ? "TRACEABLE" : "UNKNOWN");
        dto.setMatchedParams(hits);
        return dto;
    }

    private List<CalibrationImpactParamHitDto> extractParamHits(Map<String, Object> details) {
        Object rawDetailsBySize = details.get("detailsBySizeId");
        if (!(rawDetailsBySize instanceof Map<?, ?> detailsBySize)) {
            return List.of();
        }
        List<CalibrationImpactParamHitDto> hits = new ArrayList<>();
        detailsBySize.forEach((sizeKey, rawDetail) -> {
            if (!(rawDetail instanceof Map<?, ?> detail) || detail.isEmpty()) {
                return;
            }
            CalibrationImpactParamHitDto hit = new CalibrationImpactParamHitDto();
            hit.setSizeId(asLong(sizeKey));
            hit.setParamId(asLong(detail.get("paramId")));
            hit.setVersion(asInteger(detail.get("version")));
            hit.setCalibrationType(asString(detail.get("calibrationType")));
            hit.setSource(asString(detail.get("source")));
            hit.setOffset(asDecimal(detail.get("offset")));
            hit.setEffectiveOffset(asDecimal(detail.get("effectiveOffset")));
            hit.setSampleSize(asInteger(detail.get("sampleSize")));
            hit.setUniqueUserCount(asInteger(detail.get("uniqueUserCount")));
            hit.setConfidenceLevel(asString(detail.get("confidenceLevel")));
            hit.setStatus(asString(detail.get("status")));
            hits.add(hit);
        });
        hits.sort(Comparator.comparing(CalibrationImpactParamHitDto::getSizeId, Comparator.nullsLast(Long::compareTo)));
        return hits;
    }

    private CalibrationImpactSummaryDto summarizeImpact(List<CalibrationImpactRecordDto> rows) {
        CalibrationImpactSummaryDto summary = new CalibrationImpactSummaryDto();
        summary.setAppliedCount(rows.size());
        summary.setBestSizeChangedCount(rows.stream()
                .filter(row -> row.getBaseBestSizeId() != null
                        && row.getCalibratedBestSizeId() != null
                        && !Objects.equals(row.getBaseBestSizeId(), row.getCalibratedBestSizeId()))
                .count());
        summary.setAvgBaseBestScore(avg(rows.stream().map(CalibrationImpactRecordDto::getBaseBestScore).toList()));
        summary.setAvgCalibratedBestScore(avg(rows.stream().map(CalibrationImpactRecordDto::getCalibratedBestScore).toList()));
        summary.setAvgScoreDelta(avg(rows.stream().map(CalibrationImpactRecordDto::getScoreDelta).toList()));
        return summary;
    }

    private BigDecimal avg(List<BigDecimal> values) {
        List<BigDecimal> present = values.stream().filter(Objects::nonNull).toList();
        if (present.isEmpty()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal total = present.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(present.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal delta(BigDecimal after, BigDecimal before) {
        if (after == null || before == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return after.subtract(before).setScale(4, RoundingMode.HALF_UP);
    }

    private <T extends CalibrationParamListItemDto> T normalizeParamDto(T dto) {
        if (dto == null) {
            return null;
        }
        if (dto.getFeedbackDistribution() instanceof String raw) {
            dto.setFeedbackDistribution(readJson(raw));
        }
        dto.setLatestVersion(Boolean.TRUE.equals(dto.getLatestVersion()));
        dto.setEffectiveNow(Boolean.TRUE.equals(dto.getEffectiveNow()));
        dto.setShadowedByHigherVersion(Boolean.TRUE.equals(dto.getShadowedByHigherVersion()));
        return dto;
    }

    private CalibrationAuditDto normalizeAuditDto(CalibrationAuditDto dto) {
        if (dto.getOldValue() instanceof String rawOld) {
            dto.setOldValue(readJson(rawOld));
        }
        if (dto.getNewValue() instanceof String rawNew) {
            dto.setNewValue(readJson(rawNew));
        }
        return dto;
    }

    private Map<String, Object> readMap(String raw) {
        Object value = readJson(raw);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            map.forEach((key, item) -> normalized.put(String.valueOf(key), item));
            return normalized;
        }
        return Map.of();
    }

    private Object readJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, MAP_TYPE);
        } catch (Exception ignored) {
            return raw;
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private boolean sameIdentity(CalibrationParamRecord left, CalibrationParamRecord right) {
        return Objects.equals(left.getScopeType(), right.getScopeType())
                && Objects.equals(left.getScopeId(), right.getScopeId())
                && Objects.equals(left.getTargetSizeId(), right.getTargetSizeId())
                && Objects.equals(left.getCalibrationType(), right.getCalibrationType());
    }

    private void requireAdminAuthenticated() {
        if (authContext.getCurrentUserId() == null) {
            throw new BizException("登录状态无效");
        }
    }

    private String operator() {
        String username = authContext.getCurrentUsername();
        return username == null || username.isBlank() ? "system" : username;
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BizException("操作原因不能为空");
        }
        return reason.trim();
    }

    private long clampPageSize(long pageSize) {
        long safe = pageSize <= 0 ? 10L : pageSize;
        return Math.min(safe, Math.max(1, maxPageSize));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private LocalDateRange optionalDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }
        LocalDate safeStart = startDate == null ? endDate : startDate;
        LocalDate safeEnd = endDate == null ? startDate : endDate;
        if (safeEnd.isBefore(safeStart)) {
            throw new BizException("结束日期不能早于开始日期");
        }
        return new LocalDateRange(safeStart.atStartOfDay(), safeEnd.atTime(LocalTime.MAX));
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private BigDecimal asDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(4, RoundingMode.HALF_UP);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(4, RoundingMode.HALF_UP);
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value)).setScale(4, RoundingMode.HALF_UP);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record LocalDateRange(LocalDateTime startTime, LocalDateTime endTime) {
    }
}
