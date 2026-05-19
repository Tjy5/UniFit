package com.authguard.usersystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.authguard.usersystem.config.SizePreferenceProperties;
import com.authguard.usersystem.dto.SizePreferenceAdjustmentResult;
import com.authguard.usersystem.dto.SizePreferenceEventInput;
import com.authguard.usersystem.dto.SizePreferenceProfileDto;
import com.authguard.usersystem.dto.SizePreferenceRequest;
import com.authguard.usersystem.entity.RecommendationLog;
import com.authguard.usersystem.entity.SOrderItem;
import com.authguard.usersystem.entity.SOrders;
import com.authguard.usersystem.entity.SSizes;
import com.authguard.usersystem.entity.SUniform;
import com.authguard.usersystem.entity.SizeFeedback;
import com.authguard.usersystem.entity.UserSizePreferenceEvent;
import com.authguard.usersystem.entity.UserSizePreferenceProfile;
import com.authguard.usersystem.mapper.SUniformMapper;
import com.authguard.usersystem.mapper.UserSizePreferenceMapper;
import com.authguard.usersystem.service.impl.SizePreferenceServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SizePreferenceServiceImplTest {

    @Mock
    private UserSizePreferenceMapper preferenceMapper;

    @Mock
    private SUniformMapper sUniformMapper;

    private SizePreferenceServiceImpl service;

    @BeforeEach
    void setUp() {
        SizePreferenceProperties properties = new SizePreferenceProperties();
        properties.setMinFeedbackSamples(2);
        properties.setMaxScoreOffset(BigDecimal.valueOf(8));
        service = new SizePreferenceServiceImpl(preferenceMapper, sUniformMapper, properties, new ObjectMapper());
    }

    @Test
    void shouldResolveMissingUniformCategoryToGeneralWithoutNameParsing() {
        SUniform uniform = new SUniform();
        uniform.setId(88L);
        uniform.setName("冬季外套");
        when(sUniformMapper.selectSUniformById(88L)).thenReturn(uniform);

        assertEquals(SizePreferenceService.GENERAL_CATEGORY, service.resolveCategoryKey(88L));
    }

    @Test
    void shouldReadProfileByUserAndCategory() {
        UserSizePreferenceProfile profile = profile(42L, "JACKET", "LOOSE", "LOOSE", 3);
        when(preferenceMapper.selectProfile(42L, "JACKET")).thenReturn(profile);

        SizePreferenceProfileDto dto = service.getPreference(42L, null, "jacket");

        assertEquals("JACKET", dto.getCategoryKey());
        assertEquals("LOOSE", dto.getExplicitPreference());
        assertEquals("LOOSE", dto.getEffectivePreference());
    }

    @Test
    void shouldSaveExplicitPreferenceForResolvedCategory() {
        SUniform uniform = new SUniform();
        uniform.setCategoryKey("shirt");
        when(sUniformMapper.selectSUniformById(88L)).thenReturn(uniform);
        UserSizePreferenceProfile saved = profile(42L, "SHIRT", "SLIM", null, 0);
        when(preferenceMapper.selectProfile(42L, "SHIRT")).thenReturn(null, saved);

        SizePreferenceRequest request = new SizePreferenceRequest();
        request.setUniformId(88L);
        request.setFitPreference("SLIM");

        SizePreferenceProfileDto dto = service.saveExplicitPreference(42L, request);

        ArgumentCaptor<UserSizePreferenceProfile> captor = ArgumentCaptor.forClass(UserSizePreferenceProfile.class);
        verify(preferenceMapper).upsertProfile(captor.capture());
        assertEquals("SHIRT", captor.getValue().getCategoryKey());
        assertEquals("SLIM", captor.getValue().getExplicitPreference());
        assertEquals("SLIM", dto.getEffectivePreference());
    }

    @Test
    void shouldLearnTooSmallAsLooseSignalAndRecomputeFromSingleEvent() {
        RecommendationLog log = new RecommendationLog();
        log.setCategoryKey("JACKET");
        log.setUniformId(88L);
        log.setRecommendedSizeName("160");
        SizeFeedback feedback = feedback(42L, 500L, 77L, "TOO_SMALL");
        SOrderItem orderItem = orderItem(77L, 500L, 88L, "160");
        when(preferenceMapper.selectEventsByUserAndCategory(42L, "JACKET"))
                .thenReturn(List.of(event(42L, "JACKET", 77L, "TOO_SMALL", 1, "shoulder,chest")));
        when(preferenceMapper.selectProfile(42L, "JACKET")).thenReturn(null);

        service.learnFromFeedback(new SizePreferenceEventInput(42L, feedback, orderItem, completedOrder(500L, 42L), log));

        ArgumentCaptor<UserSizePreferenceEvent> eventCaptor = ArgumentCaptor.forClass(UserSizePreferenceEvent.class);
        verify(preferenceMapper).upsertEvent(eventCaptor.capture());
        assertEquals(1, eventCaptor.getValue().getDirectionDelta());
        assertEquals("160", eventCaptor.getValue().getRecommendedSize());

        ArgumentCaptor<UserSizePreferenceProfile> profileCaptor = ArgumentCaptor.forClass(UserSizePreferenceProfile.class);
        verify(preferenceMapper).upsertProfile(profileCaptor.capture());
        assertEquals(1, profileCaptor.getValue().getSampleCount());
        assertEquals(1, profileCaptor.getValue().getLooseSignalCount());
        assertEquals("LOOSE", profileCaptor.getValue().getLearnedDirection());
        assertTrue(profileCaptor.getValue().getSourceSummary().contains("issuePartCounts"));
        assertTrue(profileCaptor.getValue().getSourceSummary().contains("shoulder"));
    }

    @Test
    void shouldRecomputeIdempotentlyFromExistingEventSetWhenFeedbackChanges() {
        SizeFeedback feedback = feedback(42L, 500L, 77L, "TOO_LARGE");
        SOrderItem orderItem = orderItem(77L, 500L, 88L, "160");
        when(sUniformMapper.selectSUniformById(88L)).thenReturn(uniform("PANTS"));
        when(preferenceMapper.selectEventsByUserAndCategory(42L, "PANTS"))
                .thenReturn(List.of(event(42L, "PANTS", 77L, "TOO_LARGE", -1)));
        when(preferenceMapper.selectProfile(42L, "PANTS")).thenReturn(profile(42L, "PANTS", null, "LOOSE", 1));

        service.learnFromFeedback(new SizePreferenceEventInput(42L, feedback, orderItem, completedOrder(500L, 42L), null));

        ArgumentCaptor<UserSizePreferenceProfile> profileCaptor = ArgumentCaptor.forClass(UserSizePreferenceProfile.class);
        verify(preferenceMapper).upsertProfile(profileCaptor.capture());
        assertEquals(1, profileCaptor.getValue().getSampleCount());
        assertEquals(0, profileCaptor.getValue().getLooseSignalCount());
        assertEquals(1, profileCaptor.getValue().getSlimSignalCount());
        assertEquals("SLIM", profileCaptor.getValue().getLearnedDirection());
    }

    @Test
    void shouldNotApplyLowConfidenceLearnedProfileButApplyExplicitProfile() {
        when(preferenceMapper.selectProfile(42L, "JACKET")).thenReturn(profile(42L, "JACKET", null, "LOOSE", 1));

        SizePreferenceAdjustmentResult lowConfidence = service.buildAdjustment(42L, "JACKET", List.of(size(1L, 150), size(2L, 160)));

        assertFalse(lowConfidence.isApplied());

        when(preferenceMapper.selectProfile(42L, "SHIRT")).thenReturn(profile(42L, "SHIRT", "SLIM", "LOOSE", 1));
        SizePreferenceAdjustmentResult explicit = service.buildAdjustment(42L, "SHIRT", List.of(size(1L, 150), size(2L, 160)));

        assertTrue(explicit.isApplied());
        assertEquals(BigDecimal.valueOf(8).setScale(2), explicit.getOffsetsBySizeId().get(1L));
        assertEquals(BigDecimal.ZERO.setScale(2), explicit.getOffsetsBySizeId().get(2L));
        assertEquals("EXPLICIT", explicit.getPreferenceSource());
        assertTrue(explicit.getSummary().contains("主动设置"));
    }

    @Test
    void shouldExposeLearnedIssuePartSummaryInAdjustment() {
        UserSizePreferenceProfile profile = profile(42L, "JACKET", null, "LOOSE", 3);
        profile.setSourceSummary("{\"issuePartCounts\":{\"shoulder\":2,\"chest\":1},\"confidenceScore\":90}");
        when(preferenceMapper.selectProfile(42L, "JACKET")).thenReturn(profile);

        SizePreferenceAdjustmentResult result = service.buildAdjustment(42L, "JACKET", List.of(size(1L, 150), size(2L, 160)));

        assertTrue(result.isApplied());
        assertEquals("LEARNED", result.getPreferenceSource());
        assertEquals(2, result.getIssuePartCounts().get("shoulder"));
        assertTrue(result.getIssuePartSummary().contains("肩宽"));
        assertTrue(result.getSummary().contains("历史穿着反馈"));
    }

    @Test
    void shouldReturnNullPreferenceForAnonymousUser() {
        assertNull(service.getPreference(null, 88L, null));
    }

    private UserSizePreferenceProfile profile(Long userId, String categoryKey, String explicit, String learned, int samples) {
        UserSizePreferenceProfile profile = new UserSizePreferenceProfile();
        profile.setUserId(userId);
        profile.setCategoryKey(categoryKey);
        profile.setExplicitPreference(explicit);
        profile.setLearnedDirection(learned);
        profile.setConfidenceLevel(samples >= 2 || explicit != null ? "HIGH" : "LOW");
        profile.setConfidenceScore(samples >= 2 || explicit != null ? 90 : 50);
        profile.setSampleCount(samples);
        profile.setFitCount(0);
        profile.setTooSmallCount(0);
        profile.setTooLargeCount(0);
        profile.setLooseSignalCount("LOOSE".equals(learned) ? samples : 0);
        profile.setSlimSignalCount("SLIM".equals(learned) ? samples : 0);
        profile.setStandardSignalCount("STANDARD".equals(learned) ? samples : 0);
        profile.setUpdateTime(new Date());
        return profile;
    }

    private UserSizePreferenceEvent event(Long userId, String categoryKey, Long orderItemId, String satisfaction, int directionDelta) {
        return event(userId, categoryKey, orderItemId, satisfaction, directionDelta, null);
    }

    private UserSizePreferenceEvent event(Long userId, String categoryKey, Long orderItemId, String satisfaction, int directionDelta, String issueParts) {
        UserSizePreferenceEvent event = new UserSizePreferenceEvent();
        event.setUserId(userId);
        event.setCategoryKey(categoryKey);
        event.setOrderItemId(orderItemId);
        event.setSatisfaction(satisfaction);
        event.setDirectionDelta(directionDelta);
        event.setIssueParts(issueParts);
        return event;
    }

    private SizeFeedback feedback(Long userId, Long orderId, Long orderItemId, String satisfaction) {
        SizeFeedback feedback = new SizeFeedback();
        feedback.setFeedbackId(9L);
        feedback.setUserId(userId);
        feedback.setOrderId(orderId);
        feedback.setOrderItemId(orderItemId);
        feedback.setSatisfaction(satisfaction);
        feedback.setRecommendedSize("160");
        feedback.setPurchasedSize("160");
        return feedback;
    }

    private SOrderItem orderItem(Long orderItemId, Long orderId, Long uniformId, String sizeName) {
        SOrderItem orderItem = new SOrderItem();
        orderItem.setOrderItemId(orderItemId);
        orderItem.setOrderId(orderId);
        orderItem.setUniformId(uniformId);
        orderItem.setSizeNameSnapshot(sizeName);
        return orderItem;
    }

    private SOrders completedOrder(Long orderId, Long userId) {
        SOrders order = new SOrders();
        order.setId(orderId);
        order.setUserId(userId);
        order.setStatus(3L);
        return order;
    }

    private SUniform uniform(String categoryKey) {
        SUniform uniform = new SUniform();
        uniform.setCategoryKey(categoryKey);
        return uniform;
    }

    private SSizes size(Long id, long minHeight) {
        SSizes size = new SSizes();
        size.setId(id);
        size.setSizeName(String.valueOf(minHeight));
        size.setMinHeight(minHeight);
        size.setMinWeight(BigDecimal.valueOf(minHeight / 3));
        return size;
    }
}
