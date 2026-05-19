package com.authguard.usersystem.service.impl;

import com.authguard.usersystem.config.SizePreferenceProperties;
import com.authguard.usersystem.dto.SizePreferenceAdjustmentResult;
import com.authguard.usersystem.dto.SizePreferenceBackfillRecord;
import com.authguard.usersystem.dto.SizePreferenceEventInput;
import com.authguard.usersystem.dto.SizePreferenceProfileDto;
import com.authguard.usersystem.dto.SizePreferenceRequest;
import com.authguard.usersystem.entity.RecommendationLog;
import com.authguard.usersystem.entity.SOrderItem;
import com.authguard.usersystem.entity.SSizes;
import com.authguard.usersystem.entity.SUniform;
import com.authguard.usersystem.entity.UserSizePreferenceEvent;
import com.authguard.usersystem.entity.UserSizePreferenceProfile;
import com.authguard.usersystem.mapper.SUniformMapper;
import com.authguard.usersystem.mapper.UserSizePreferenceMapper;
import com.authguard.usersystem.service.SizePreferenceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SizePreferenceServiceImpl implements SizePreferenceService {

    private static final String LOOSE = "LOOSE";
    private static final String STANDARD = "STANDARD";
    private static final String SLIM = "SLIM";

    private final UserSizePreferenceMapper preferenceMapper;
    private final SUniformMapper sUniformMapper;
    private final SizePreferenceProperties properties;
    private final ObjectMapper objectMapper;

    public SizePreferenceServiceImpl(UserSizePreferenceMapper preferenceMapper,
                                     SUniformMapper sUniformMapper,
                                     SizePreferenceProperties properties,
                                     @Autowired(required = false) ObjectMapper objectMapper) {
        this.preferenceMapper = preferenceMapper;
        this.sUniformMapper = sUniformMapper;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String resolveCategoryKey(Long uniformId) {
        if (uniformId == null) {
            return GENERAL_CATEGORY;
        }
        SUniform uniform = sUniformMapper.selectSUniformById(uniformId);
        return normalizeCategoryKey(uniform == null ? null : uniform.getCategoryKey());
    }

    @Override
    public SizePreferenceProfileDto getPreference(Long userId, Long uniformId, String categoryKey) {
        if (userId == null) {
            return null;
        }
        String resolvedCategory = StringUtils.hasText(categoryKey) ? normalizeCategoryKey(categoryKey) : resolveCategoryKey(uniformId);
        return toDto(preferenceMapper.selectProfile(userId, resolvedCategory));
    }

    @Override
    @Transactional
    public SizePreferenceProfileDto saveExplicitPreference(Long userId, SizePreferenceRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        if (request == null || !StringUtils.hasText(request.getFitPreference())) {
            throw new IllegalArgumentException("请选择尺码偏好");
        }
        String preference = request.getFitPreference().trim().toUpperCase(Locale.ROOT);
        if (!List.of(LOOSE, STANDARD, SLIM).contains(preference)) {
            throw new IllegalArgumentException("不支持的尺码偏好");
        }
        String categoryKey = StringUtils.hasText(request.getCategoryKey())
                ? normalizeCategoryKey(request.getCategoryKey())
                : resolveCategoryKey(request.getUniformId());

        UserSizePreferenceProfile profile = preferenceMapper.selectProfile(userId, categoryKey);
        if (profile == null) {
            profile = emptyProfile(userId, categoryKey);
        }
        profile.setExplicitPreference(preference);
        profile.setConfidenceLevel("HIGH");
        profile.setConfidenceScore(Math.max(defaultInt(profile.getConfidenceScore()), 85));
        profile.setUpdateBy(String.valueOf(userId));
        profile.setUpdateTime(new Date());
        preferenceMapper.upsertProfile(profile);
        return toDto(preferenceMapper.selectProfile(userId, categoryKey));
    }

    @Override
    @Transactional
    public void learnFromFeedback(SizePreferenceEventInput input) {
        if (input == null || input.getUserId() == null || input.getFeedback() == null || input.getOrderItem() == null) {
            return;
        }
        String categoryKey = categoryFrom(input.getRecommendationLog(), input.getOrderItem());
        Date now = new Date();
        UserSizePreferenceEvent event = new UserSizePreferenceEvent();
        event.setUserId(input.getUserId());
        event.setOrderId(input.getFeedback().getOrderId());
        event.setOrderItemId(input.getFeedback().getOrderItemId());
        event.setFeedbackId(input.getFeedback().getFeedbackId());
        event.setUniformId(resolveUniformId(input.getRecommendationLog(), input.getOrderItem()));
        event.setCategoryKey(categoryKey);
        event.setRecommendedSize(resolveRecommendedSize(input.getRecommendationLog(), input.getFeedback().getRecommendedSize()));
        event.setPurchasedSize(StringUtils.hasText(input.getFeedback().getPurchasedSize())
                ? input.getFeedback().getPurchasedSize()
                : input.getOrderItem().getSizeNameSnapshot());
        event.setSatisfaction(input.getFeedback().getSatisfaction());
        event.setIssueParts(input.getFeedback().getIssueParts());
        event.setDirectionDelta(directionDelta(input.getFeedback().getSatisfaction()));
        event.setSourceType("FEEDBACK");
        event.setCreateBy(String.valueOf(input.getUserId()));
        event.setCreateTime(now);
        event.setUpdateBy(String.valueOf(input.getUserId()));
        event.setUpdateTime(now);

        preferenceMapper.upsertEvent(event);
        recomputeProfile(input.getUserId(), categoryKey);
    }

    @Override
    public SizePreferenceAdjustmentResult buildAdjustment(Long userId, String categoryKey, List<SSizes> sizeOrder) {
        SizePreferenceAdjustmentResult result = new SizePreferenceAdjustmentResult();
        String resolvedCategory = normalizeCategoryKey(categoryKey);
        result.setCategoryKey(resolvedCategory);
        if (!properties.isEnabled() || userId == null || sizeOrder == null || sizeOrder.isEmpty()) {
            return result;
        }

        UserSizePreferenceProfile profile = preferenceMapper.selectProfile(userId, resolvedCategory);
        if (profile == null) {
            return result;
        }
        String preference = effectivePreference(profile);
        result.setPreference(preference);
        result.setConfidenceLevel(profile.getConfidenceLevel());
        result.setConfidenceScore(profile.getConfidenceScore());
        result.setSampleCount(defaultInt(profile.getSampleCount()));
        result.setPreferenceSource(StringUtils.hasText(profile.getExplicitPreference()) ? "EXPLICIT" : "LEARNED");
        Map<String, Object> sourceSummary = readSummary(profile.getSourceSummary());
        Map<String, Integer> issuePartCounts = issuePartCounts(sourceSummary);
        result.setIssuePartCounts(issuePartCounts);
        result.setTopIssueParts(issuePartCounts.keySet().stream().limit(3).toList());
        result.setIssuePartSummary(issuePartSummary(issuePartCounts));
        if (!shouldApply(profile, preference)) {
            result.setSummary(preferenceSummary(profile, preference));
            return result;
        }
        if (STANDARD.equals(preference)) {
            result.setSummary(preferenceSummary(profile, preference));
            return result;
        }

        List<SSizes> orderedSizes = sizeOrder.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing((SSizes size) -> nullableDecimal(size.getMinHeight()))
                        .thenComparing(size -> nullableDecimal(size.getMinWeight()))
                        .thenComparing(SSizes::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        if (orderedSizes.size() < 2) {
            result.setSummary(preferenceSummary(profile, preference));
            return result;
        }

        BigDecimal maxOffset = properties.getMaxScoreOffset() == null ? BigDecimal.valueOf(8) : properties.getMaxScoreOffset().abs();
        int lastIndex = orderedSizes.size() - 1;
        Map<Long, BigDecimal> offsets = new LinkedHashMap<>();
        for (int index = 0; index < orderedSizes.size(); index++) {
            SSizes size = orderedSizes.get(index);
            if (size.getId() == null) {
                continue;
            }
            BigDecimal ratio = BigDecimal.valueOf(index)
                    .divide(BigDecimal.valueOf(lastIndex), 4, RoundingMode.HALF_UP);
            BigDecimal signedRatio = LOOSE.equals(preference)
                    ? ratio
                    : BigDecimal.ONE.subtract(ratio);
            BigDecimal offset = signedRatio.multiply(maxOffset).setScale(2, RoundingMode.HALF_UP);
            offsets.put(size.getId(), offset);
        }
        result.setApplied(offsets.values().stream().anyMatch(offset -> offset.compareTo(BigDecimal.ZERO) != 0));
        result.setOffsetsBySizeId(offsets);
        result.setSummary(preferenceSummary(profile, preference));
        return result;
    }

    @Override
    @Transactional
    public int backfillCompletedFeedback() {
        List<SizePreferenceBackfillRecord> records = preferenceMapper.selectCompletedFeedbackBackfillRecords();
        int count = 0;
        for (SizePreferenceBackfillRecord record : records) {
            if (record.getUserId() == null || record.getOrderItemId() == null) {
                continue;
            }
            UserSizePreferenceEvent event = new UserSizePreferenceEvent();
            event.setUserId(record.getUserId());
            event.setOrderId(record.getOrderId());
            event.setOrderItemId(record.getOrderItemId());
            event.setFeedbackId(record.getFeedbackId());
            event.setUniformId(record.getUniformId());
            event.setCategoryKey(normalizeCategoryKey(record.getCategoryKey()));
            event.setRecommendedSize(record.getRecommendedSize());
            event.setPurchasedSize(record.getPurchasedSize());
            event.setSatisfaction(record.getSatisfaction());
            event.setIssueParts(record.getIssueParts());
            event.setDirectionDelta(directionDelta(record.getSatisfaction()));
            event.setSourceType("BACKFILL");
            event.setCreateBy("backfill");
            event.setCreateTime(record.getFeedbackTime() == null ? new Date() : record.getFeedbackTime());
            event.setUpdateBy("backfill");
            event.setUpdateTime(new Date());
            preferenceMapper.upsertEvent(event);
            recomputeProfile(record.getUserId(), event.getCategoryKey());
            count++;
        }
        return count;
    }

    private void recomputeProfile(Long userId, String categoryKey) {
        List<UserSizePreferenceEvent> events = preferenceMapper.selectEventsByUserAndCategory(userId, categoryKey);
        UserSizePreferenceProfile existing = preferenceMapper.selectProfile(userId, categoryKey);
        UserSizePreferenceProfile profile = existing == null ? emptyProfile(userId, categoryKey) : existing;
        int fit = 0;
        int tooSmall = 0;
        int tooLarge = 0;
        int loose = 0;
        int slim = 0;
        int standard = 0;
        Map<String, Integer> issueParts = new LinkedHashMap<>();
        for (UserSizePreferenceEvent event : events) {
            if ("FIT".equals(event.getSatisfaction())) {
                fit++;
            } else if ("TOO_SMALL".equals(event.getSatisfaction())) {
                tooSmall++;
            } else if ("TOO_LARGE".equals(event.getSatisfaction())) {
                tooLarge++;
            }
            int delta = defaultInt(event.getDirectionDelta());
            if (delta > 0) {
                loose++;
            } else if (delta < 0) {
                slim++;
            } else {
                standard++;
            }
            mergeIssueParts(issueParts, event.getIssueParts());
        }
        int samples = events.size();
        int confidenceScore = confidenceScore(samples, loose, slim, standard, issueParts);
        profile.setSampleCount(samples);
        profile.setFitCount(fit);
        profile.setTooSmallCount(tooSmall);
        profile.setTooLargeCount(tooLarge);
        profile.setLooseSignalCount(loose);
        profile.setSlimSignalCount(slim);
        profile.setStandardSignalCount(standard);
        profile.setLearnedDirection(resolveLearnedDirection(loose, slim, standard));
        profile.setConfidenceScore(confidenceScore);
        profile.setConfidenceLevel(confidenceLevel(confidenceScore, samples));
        Map<String, Object> sourceSummary = new LinkedHashMap<>();
        sourceSummary.put("sampleCount", samples);
        sourceSummary.put("fit", fit);
        sourceSummary.put("tooSmall", tooSmall);
        sourceSummary.put("tooLarge", tooLarge);
        sourceSummary.put("looseSignals", loose);
        sourceSummary.put("slimSignals", slim);
        sourceSummary.put("standardSignals", standard);
        sourceSummary.put("issuePartCounts", issueParts);
        sourceSummary.put("topIssueParts", issueParts.keySet().stream().limit(3).toList());
        sourceSummary.put("learnedDirection", profile.getLearnedDirection());
        sourceSummary.put("confidenceScore", confidenceScore);
        sourceSummary.put("confidenceLevel", profile.getConfidenceLevel());
        sourceSummary.put("confidenceSignals", Map.of(
                "sampleCount", samples,
                "dominantDirectionSupport", dominantSupport(samples, loose, slim, standard),
                "issuePartSignalCount", issueParts.values().stream().mapToInt(Integer::intValue).sum()
        ));
        profile.setSourceSummary(writeJson(sourceSummary));
        profile.setUpdateBy(String.valueOf(userId));
        profile.setUpdateTime(new Date());
        preferenceMapper.upsertProfile(profile);
    }

    private UserSizePreferenceProfile emptyProfile(Long userId, String categoryKey) {
        Date now = new Date();
        UserSizePreferenceProfile profile = new UserSizePreferenceProfile();
        profile.setUserId(userId);
        profile.setCategoryKey(normalizeCategoryKey(categoryKey));
        profile.setConfidenceLevel("LOW");
        profile.setConfidenceScore(0);
        profile.setSampleCount(0);
        profile.setFitCount(0);
        profile.setTooSmallCount(0);
        profile.setTooLargeCount(0);
        profile.setLooseSignalCount(0);
        profile.setSlimSignalCount(0);
        profile.setStandardSignalCount(0);
        profile.setCreateBy(String.valueOf(userId));
        profile.setCreateTime(now);
        profile.setUpdateBy(String.valueOf(userId));
        profile.setUpdateTime(now);
        return profile;
    }

    private boolean shouldApply(UserSizePreferenceProfile profile, String preference) {
        if (!StringUtils.hasText(preference)) {
            return false;
        }
        if (StringUtils.hasText(profile.getExplicitPreference())) {
            return true;
        }
        return defaultInt(profile.getSampleCount()) >= properties.getMinFeedbackSamples()
                && !"LOW".equals(profile.getConfidenceLevel());
    }

    private String effectivePreference(UserSizePreferenceProfile profile) {
        if (profile == null) {
            return null;
        }
        if (StringUtils.hasText(profile.getExplicitPreference())) {
            return profile.getExplicitPreference();
        }
        return profile.getLearnedDirection();
    }

    private String resolveLearnedDirection(int loose, int slim, int standard) {
        if (loose > slim && loose >= standard) {
            return LOOSE;
        }
        if (slim > loose && slim >= standard) {
            return SLIM;
        }
        return STANDARD;
    }

    private int confidenceScore(int samples, int loose, int slim, int standard, Map<String, Integer> issueParts) {
        if (samples <= 0) {
            return 0;
        }
        int strongest = Math.max(Math.max(loose, slim), standard);
        BigDecimal support = BigDecimal.valueOf(strongest)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(samples), 0, RoundingMode.HALF_UP);
        int sampleBoost = Math.min(20, samples * 5);
        int issuePartBoost = Math.min(8, issueParts == null ? 0 : issueParts.values().stream().mapToInt(Integer::intValue).sum() * 2);
        return Math.min(100, support.intValue() + sampleBoost + issuePartBoost);
    }

    private int dominantSupport(int samples, int loose, int slim, int standard) {
        if (samples <= 0) {
            return 0;
        }
        int strongest = Math.max(Math.max(loose, slim), standard);
        return BigDecimal.valueOf(strongest)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(samples), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private String confidenceLevel(Integer score, int samples) {
        int safeScore = defaultInt(score);
        if (samples < properties.getMinFeedbackSamples()) {
            return "LOW";
        }
        if (safeScore >= 85) {
            return "HIGH";
        }
        if (safeScore >= 60) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private SizePreferenceProfileDto toDto(UserSizePreferenceProfile profile) {
        if (profile == null) {
            return null;
        }
        SizePreferenceProfileDto dto = new SizePreferenceProfileDto();
        dto.setUserId(profile.getUserId());
        dto.setCategoryKey(profile.getCategoryKey());
        dto.setExplicitPreference(profile.getExplicitPreference());
        dto.setLearnedDirection(profile.getLearnedDirection());
        dto.setEffectivePreference(effectivePreference(profile));
        dto.setConfidenceLevel(profile.getConfidenceLevel());
        dto.setConfidenceScore(profile.getConfidenceScore());
        dto.setSampleCount(profile.getSampleCount());
        dto.setFitCount(profile.getFitCount());
        dto.setTooSmallCount(profile.getTooSmallCount());
        dto.setTooLargeCount(profile.getTooLargeCount());
        dto.setUpdatedAt(profile.getUpdateTime());
        return dto;
    }

    private String preferenceSummary(UserSizePreferenceProfile profile, String preference) {
        if (!StringUtils.hasText(preference)) {
            return "";
        }
        String label = switch (preference) {
            case LOOSE -> "偏宽松";
            case SLIM -> "偏修身";
            default -> "标准合身";
        };
        String issueSummary = issuePartSummary(issuePartCounts(readSummary(profile.getSourceSummary())));
        String source = StringUtils.hasText(profile.getExplicitPreference())
                ? "已参考您主动设置的" + label + "穿着偏好"
                : "已参考历史穿着反馈学习出的" + label + "偏好";
        return StringUtils.hasText(issueSummary) ? source + "，" + issueSummary : source;
    }

    private String categoryFrom(RecommendationLog recommendationLog, SOrderItem orderItem) {
        if (recommendationLog != null && StringUtils.hasText(recommendationLog.getCategoryKey())) {
            return normalizeCategoryKey(recommendationLog.getCategoryKey());
        }
        return resolveCategoryKey(resolveUniformId(recommendationLog, orderItem));
    }

    private Long resolveUniformId(RecommendationLog recommendationLog, SOrderItem orderItem) {
        if (recommendationLog != null && recommendationLog.getUniformId() != null) {
            return recommendationLog.getUniformId();
        }
        return orderItem == null ? null : orderItem.getUniformId();
    }

    private String resolveRecommendedSize(RecommendationLog recommendationLog, String fallback) {
        if (recommendationLog != null && StringUtils.hasText(recommendationLog.getRecommendedSizeName())) {
            return recommendationLog.getRecommendedSizeName();
        }
        return fallback;
    }

    private int directionDelta(String satisfaction) {
        if ("TOO_SMALL".equals(satisfaction)) {
            return 1;
        }
        if ("TOO_LARGE".equals(satisfaction)) {
            return -1;
        }
        return 0;
    }

    private String normalizeCategoryKey(String categoryKey) {
        if (!StringUtils.hasText(categoryKey)) {
            return GENERAL_CATEGORY;
        }
        return categoryKey.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal nullableDecimal(Long value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private BigDecimal nullableDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private void mergeIssueParts(Map<String, Integer> issueParts, String rawIssueParts) {
        if (!StringUtils.hasText(rawIssueParts)) {
            return;
        }
        Arrays.stream(rawIssueParts.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(part -> issueParts.merge(part, 1, Integer::sum));
    }

    private Map<String, Object> readSummary(String sourceSummary) {
        if (!StringUtils.hasText(sourceSummary) || objectMapper == null) {
            return Map.of();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(sourceSummary, Map.class);
            return parsed == null ? Map.of() : parsed;
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private Map<String, Integer> issuePartCounts(Map<String, Object> sourceSummary) {
        Object rawCounts = sourceSummary == null ? null : sourceSummary.get("issuePartCounts");
        if (!(rawCounts instanceof Map<?, ?> counts)) {
            return new LinkedHashMap<>();
        }
        return counts.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() instanceof Number)
                .sorted((left, right) -> Integer.compare(((Number) right.getValue()).intValue(), ((Number) left.getValue()).intValue()))
                .collect(java.util.stream.Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        entry -> ((Number) entry.getValue()).intValue(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private String issuePartSummary(Map<String, Integer> issuePartCounts) {
        if (issuePartCounts == null || issuePartCounts.isEmpty()) {
            return "";
        }
        String labels = issuePartCounts.keySet().stream()
                .limit(2)
                .map(this::issuePartLabel)
                .collect(java.util.stream.Collectors.joining("、"));
        return "历史反馈中" + labels + "提及较多";
    }

    private String issuePartLabel(String issuePart) {
        return switch (issuePart) {
            case "shoulder" -> "肩宽";
            case "chest" -> "胸围";
            case "waist" -> "腰围";
            case "length" -> "衣长";
            case "sleeve" -> "袖长";
            default -> issuePart;
        };
    }

    private String writeJson(Object value) {
        if (objectMapper == null) {
            return String.valueOf(value);
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }
}
