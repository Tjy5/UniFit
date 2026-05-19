package com.authguard.usersystem.service.impl;

import com.authguard.usersystem.dto.SizePreferenceEventInput;
import com.authguard.usersystem.dto.SizeFeedbackRequest;
import com.authguard.usersystem.dto.SizeFeedbackResponse;
import com.authguard.usersystem.dto.SizeRecommendationCandidateDto;
import com.authguard.usersystem.dto.SizeRecommendationResponse;
import com.authguard.usersystem.entity.RecommendationLog;
import com.authguard.usersystem.entity.SOrderItem;
import com.authguard.usersystem.entity.SOrders;
import com.authguard.usersystem.entity.SizeFeedback;
import com.authguard.usersystem.mapper.RecommendationLogMapper;
import com.authguard.usersystem.mapper.SOrderItemMapper;
import com.authguard.usersystem.mapper.SizeFeedbackMapper;
import com.authguard.usersystem.service.SOrdersService;
import com.authguard.usersystem.service.SizeFeedbackService;
import com.authguard.usersystem.service.SizePreferenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SizeFeedbackServiceImpl implements SizeFeedbackService {

    private static final List<String> VALID_SATISFACTIONS = List.of("FIT", "TOO_LARGE", "TOO_SMALL");

    private final SizeFeedbackMapper sizeFeedbackMapper;
    private final SOrderItemMapper sOrderItemMapper;
    private final SOrdersService sOrdersService;
    private final RecommendationLogMapper recommendationLogMapper;
    private final SizePreferenceService sizePreferenceService;
    private final ObjectMapper objectMapper;

    public SizeFeedbackServiceImpl(SizeFeedbackMapper sizeFeedbackMapper,
                                   SOrderItemMapper sOrderItemMapper,
                                   SOrdersService sOrdersService,
                                   RecommendationLogMapper recommendationLogMapper,
                                   @Autowired(required = false) SizePreferenceService sizePreferenceService,
                                   @Autowired(required = false) ObjectMapper objectMapper) {
        this.sizeFeedbackMapper = sizeFeedbackMapper;
        this.sOrderItemMapper = sOrderItemMapper;
        this.sOrdersService = sOrdersService;
        this.recommendationLogMapper = recommendationLogMapper;
        this.sizePreferenceService = sizePreferenceService;
        this.objectMapper = objectMapper;
    }

    @Override
    public SizeFeedbackResponse getFeedback(Long userId, Long orderItemId) {
        SOrderItem orderItem = requireOrderItem(orderItemId);
        SOrders order = requireCompletedOrder(orderItem, userId);
        SizeFeedback feedback = sizeFeedbackMapper.selectByOrderItemId(orderItemId);
        RecommendationLog linkedRecommendation = recommendationLogMapper.selectByOrderItemId(orderItemId);
        return feedback == null ? null : toResponse(feedback, orderItem, order.getId(), linkedRecommendation);
    }

    @Override
    @Transactional
    public SizeFeedbackResponse saveFeedback(Long userId, Long orderItemId, SizeFeedbackRequest request) {
        if (request == null || !StringUtils.hasText(request.getSatisfaction())) {
            throw new IllegalArgumentException("请选择尺码反馈结果");
        }
        if (!VALID_SATISFACTIONS.contains(request.getSatisfaction())) {
            throw new IllegalArgumentException("不支持的尺码反馈结果");
        }

        SOrderItem orderItem = requireOrderItem(orderItemId);
        SOrders order = requireCompletedOrder(orderItem, userId);
        SizeFeedback existing = sizeFeedbackMapper.selectByOrderItemId(orderItemId);
        RecommendationLog linkedRecommendation = recommendationLogMapper.selectByOrderItemId(orderItemId);
        Date now = new Date();

        SizeFeedback feedback = existing == null ? new SizeFeedback() : existing;
        feedback.setUserId(userId);
        feedback.setOrderId(order.getId());
        feedback.setOrderItemId(orderItemId);
        feedback.setRecommendedSize(resolveRecommendedSize(request, existing, linkedRecommendation));
        feedback.setPurchasedSize(StringUtils.hasText(request.getPurchasedSize()) ? request.getPurchasedSize() : orderItem.getSizeNameSnapshot());
        feedback.setSatisfaction(request.getSatisfaction());
        feedback.setIssueParts(String.join(",", request.getIssueParts() == null ? List.of() : request.getIssueParts()));
        feedback.setNote(request.getNote());
        feedback.setUpdateBy(String.valueOf(userId));
        feedback.setUpdateTime(now);

        if (existing == null) {
            feedback.setCreateBy(String.valueOf(userId));
            feedback.setCreateTime(now);
            sizeFeedbackMapper.insert(feedback);
        } else {
            sizeFeedbackMapper.update(feedback);
        }
        if (sizePreferenceService != null) {
            sizePreferenceService.learnFromFeedback(new SizePreferenceEventInput(userId, feedback, orderItem, order, linkedRecommendation));
        }
        return toResponse(feedback, orderItem, order.getId(), linkedRecommendation);
    }

    @Override
    public SizeRecommendationResponse getRecommendationSnapshot(Long userId, Long orderItemId) {
        SOrderItem orderItem = requireOrderItem(orderItemId);
        requireCompletedOrder(orderItem, userId);
        RecommendationLog log = recommendationLogMapper.selectByOrderItemId(orderItemId);
        if (log == null) {
            return null;
        }
        return toRecommendationSnapshot(log);
    }

    private SOrderItem requireOrderItem(Long orderItemId) {
        SOrderItem orderItem = sOrderItemMapper.selectOrderItemById(orderItemId);
        if (orderItem == null) {
            throw new IllegalArgumentException("订单项不存在");
        }
        return orderItem;
    }

    private SOrders requireCompletedOrder(SOrderItem orderItem, Long userId) {
        SOrders order = sOrdersService.getOrderById(orderItem.getOrderId());
        if (order == null || order.getUserId() == null || !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("您无权操作该订单项");
        }
        if (order.getStatus() == null || order.getStatus() != 3L) {
            throw new IllegalArgumentException("仅已完成订单可提交尺码反馈");
        }
        return order;
    }

    private SizeFeedbackResponse toResponse(SizeFeedback feedback,
                                            SOrderItem orderItem,
                                            Long orderId,
                                            RecommendationLog linkedRecommendation) {
        SizeFeedbackResponse response = new SizeFeedbackResponse();
        response.setFeedbackId(feedback.getFeedbackId());
        response.setOrderId(orderId);
        response.setOrderItemId(feedback.getOrderItemId());
        response.setRecommendedSize(StringUtils.hasText(feedback.getRecommendedSize())
                ? feedback.getRecommendedSize()
                : linkedRecommendation == null ? null : linkedRecommendation.getRecommendedSizeName());
        response.setPurchasedSize(StringUtils.hasText(feedback.getPurchasedSize())
                ? feedback.getPurchasedSize()
                : orderItem.getSizeNameSnapshot());
        response.setSatisfaction(feedback.getSatisfaction());
        response.setIssueParts(parseIssueParts(feedback.getIssueParts()));
        response.setNote(feedback.getNote());
        response.setSubmitted(true);
        response.setUpdatedAt(feedback.getUpdateTime() != null ? feedback.getUpdateTime() : feedback.getCreateTime());
        return response;
    }

    private SizeRecommendationResponse toRecommendationSnapshot(RecommendationLog log) {
        SizeRecommendationResponse response = readRecommendationSummary(log.getResultSummary());
        if (response == null) {
            response = new SizeRecommendationResponse();
        }

        response.setRecommendationLogId(log.getLogId());
        response.setCalibrationApplied(Boolean.TRUE.equals(log.getCalibrationApplied()));
        response.setPersonalizationApplied(response.isPersonalizationApplied() || Boolean.TRUE.equals(log.getPersonalizationApplied()));
        response.setPersonalizationDetails(response.getPersonalizationDetails() == null ? new LinkedHashMap<>() : response.getPersonalizationDetails());
        response.setConfidence(log.getConfidenceScore());
        response.setConfidenceLevel(log.getConfidenceLevel());
        response.setReasons(response.getReasons() == null ? new ArrayList<>() : new ArrayList<>(response.getReasons()));
        response.setAlternatives(response.getAlternatives() == null ? new ArrayList<>() : new ArrayList<>(response.getAlternatives()));
        response.setUsedDimensions(response.getUsedDimensions() == null ? new ArrayList<>() : new ArrayList<>(response.getUsedDimensions()));

        if (response.getRecommended() == null && StringUtils.hasText(log.getRecommendedSizeName())) {
            SizeRecommendationCandidateDto candidate = new SizeRecommendationCandidateDto();
            candidate.setSizeId(log.getRecommendedSizeId());
            candidate.setSizeName(log.getRecommendedSizeName());
            response.setRecommended(candidate);
        }

        if (response.getRecommended() != null) {
            if (response.getRecommended().getSizeId() == null) {
                response.getRecommended().setSizeId(log.getRecommendedSizeId());
            }
            if (!StringUtils.hasText(response.getRecommended().getSizeName())) {
                response.getRecommended().setSizeName(log.getRecommendedSizeName());
            }
        }

        if (!StringUtils.hasText(response.getMessage())) {
            response.setMessage("以下展示的是您下单时关联的推荐快照");
        }
        if (!StringUtils.hasText(response.getConfidenceMessage()) && response.getConfidence() != null) {
            response.setConfidenceMessage("该结果来自下单时记录的推荐快照");
        }
        restoreCalibrationFallback(response, log);
        restorePersonalizationFallback(response, log);
        if (response.isCalibrationApplied() && response.getReasons().stream().noneMatch(reason -> reason.contains("群体尺码反馈"))) {
            response.getReasons().add(0, "该推荐已参考群体尺码反馈微调");
        }
        if (response.isPersonalizationApplied() && response.getReasons().stream().noneMatch(this::isPersonalizationReason)) {
            response.getReasons().add(0, personalizationSnapshotReason(response));
        } else if (response.getReasons().isEmpty()) {
            response.getReasons().add("该推荐为下单时保存的历史快照");
        }

        response.setAvailable(response.getRecommended() != null);
        return response;
    }

    private void restoreCalibrationFallback(SizeRecommendationResponse response, RecommendationLog log) {
        if (response.getCalibrationDetails() == null) {
            response.setCalibrationDetails(new LinkedHashMap<>());
        }
        Map<String, Object> storedDetails = readDetailsMap(log.getCalibrationDetails());
        if (storedDetails != null) {
            storedDetails.forEach(response.getCalibrationDetails()::putIfAbsent);
        }
        response.getCalibrationDetails().putIfAbsent("applied", Boolean.TRUE.equals(log.getCalibrationApplied()));
    }

    private void restorePersonalizationFallback(SizeRecommendationResponse response, RecommendationLog log) {
        if (response.getPersonalizationDetails() == null) {
            response.setPersonalizationDetails(new LinkedHashMap<>());
        }
        Map<String, Object> storedDetails = readDetailsMap(log.getPersonalizationDetails());
        if (storedDetails != null) {
            storedDetails.forEach(response.getPersonalizationDetails()::putIfAbsent);
        }
        response.getPersonalizationDetails().putIfAbsent("categoryKey", log.getCategoryKey());
        response.getPersonalizationDetails().putIfAbsent("applied", Boolean.TRUE.equals(log.getPersonalizationApplied()));
        if (!StringUtils.hasText(response.getPreferenceSummary()) && StringUtils.hasText(log.getPersonalizationDetails())) {
            response.setPreferenceSummary("该推荐已保存个人偏好快照");
        }
    }

    private boolean isPersonalizationReason(String reason) {
        return reason != null && (reason.contains("主动设置") || reason.contains("历史穿着反馈") || reason.contains("个人尺码偏好"));
    }

    private String personalizationSnapshotReason(SizeRecommendationResponse response) {
        Object source = response.getPersonalizationDetails() == null ? null : response.getPersonalizationDetails().get("preferenceSource");
        return "EXPLICIT".equals(source)
                ? "该推荐已参考您主动设置的穿着偏好"
                : "该推荐已参考历史穿着反馈";
    }

    private Map<String, Object> readDetailsMap(String details) {
        if (!StringUtils.hasText(details) || objectMapper == null) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(details, Map.class);
            return parsed;
        } catch (Exception ex) {
            return null;
        }
    }

    private SizeRecommendationResponse readRecommendationSummary(String resultSummary) {
        if (!StringUtils.hasText(resultSummary) || objectMapper == null) {
            return null;
        }
        try {
            return objectMapper.readValue(resultSummary, SizeRecommendationResponse.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveRecommendedSize(SizeFeedbackRequest request,
                                          SizeFeedback existing,
                                          RecommendationLog linkedRecommendation) {
        if (linkedRecommendation != null && StringUtils.hasText(linkedRecommendation.getRecommendedSizeName())) {
            return linkedRecommendation.getRecommendedSizeName();
        }
        if (existing != null && StringUtils.hasText(existing.getRecommendedSize())) {
            return existing.getRecommendedSize();
        }
        return request.getRecommendedSize();
    }

    private List<String> parseIssueParts(String issueParts) {
        if (!StringUtils.hasText(issueParts)) {
            return List.of();
        }
        return Arrays.stream(issueParts.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
