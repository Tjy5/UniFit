package com.authguard.usersystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.authguard.usersystem.dto.SizeFeedbackRequest;
import com.authguard.usersystem.dto.SizePreferenceEventInput;
import com.authguard.usersystem.dto.SizeFeedbackResponse;
import com.authguard.usersystem.dto.SizeRecommendationResponse;
import com.authguard.usersystem.entity.RecommendationLog;
import com.authguard.usersystem.entity.SOrderItem;
import com.authguard.usersystem.entity.SOrders;
import com.authguard.usersystem.entity.SizeFeedback;
import com.authguard.usersystem.mapper.RecommendationLogMapper;
import com.authguard.usersystem.mapper.SOrderItemMapper;
import com.authguard.usersystem.mapper.SizeFeedbackMapper;
import com.authguard.usersystem.service.impl.SizeFeedbackServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class SizeFeedbackServiceImplTest {

    @Mock
    private SizeFeedbackMapper sizeFeedbackMapper;

    @Mock
    private SOrderItemMapper sOrderItemMapper;

    @Mock
    private SOrdersService sOrdersService;

    @Mock
    private RecommendationLogMapper recommendationLogMapper;

    @Mock
    private SizePreferenceService sizePreferenceService;

    private SizeFeedbackServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SizeFeedbackServiceImpl(
                sizeFeedbackMapper,
                sOrderItemMapper,
                sOrdersService,
                recommendationLogMapper,
                sizePreferenceService,
                new ObjectMapper()
        );
    }

    @Test
    void shouldPreferLinkedRecommendationSnapshotWhenSavingFeedback() {
        Long userId = 42L;
        Long orderItemId = 77L;

        when(sOrderItemMapper.selectOrderItemById(orderItemId)).thenReturn(orderItem(orderItemId, 500L, "160"));
        when(sOrdersService.getOrderById(500L)).thenReturn(completedOrder(500L, userId));
        when(sizeFeedbackMapper.selectByOrderItemId(orderItemId)).thenReturn(null);

        RecommendationLog linkedRecommendation = new RecommendationLog();
        linkedRecommendation.setRecommendedSizeName("165");
        linkedRecommendation.setCreateTime(new Date());
        when(recommendationLogMapper.selectByOrderItemId(orderItemId)).thenReturn(linkedRecommendation);
        doAnswer(invocation -> {
            SizeFeedback feedback = invocation.getArgument(0);
            feedback.setFeedbackId(1L);
            return 1;
        }).when(sizeFeedbackMapper).insert(any(SizeFeedback.class));

        SizeFeedbackRequest request = new SizeFeedbackRequest();
        request.setRecommendedSize("160");
        request.setSatisfaction("TOO_SMALL");

        SizeFeedbackResponse response = service.saveFeedback(userId, orderItemId, request);

        ArgumentCaptor<SizeFeedback> feedbackCaptor = ArgumentCaptor.forClass(SizeFeedback.class);
        verify(sizeFeedbackMapper).insert(feedbackCaptor.capture());
        SizeFeedback savedFeedback = feedbackCaptor.getValue();

        assertEquals("165", savedFeedback.getRecommendedSize());
        assertEquals("160", savedFeedback.getPurchasedSize());
        assertEquals("165", response.getRecommendedSize());
        assertEquals("160", response.getPurchasedSize());
        assertTrue(response.isSubmitted());
        verify(sizePreferenceService).learnFromFeedback(any(SizePreferenceEventInput.class));
    }

    @Test
    void saveFeedbackShouldRunInTransaction() throws Exception {
        Transactional transactional = SizeFeedbackServiceImpl.class
                .getMethod("saveFeedback", Long.class, Long.class, SizeFeedbackRequest.class)
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
    }

    @Test
    void shouldUpdateExistingFeedbackInsteadOfInsertingDuplicate() {
        Long userId = 42L;
        Long orderItemId = 77L;

        when(sOrderItemMapper.selectOrderItemById(orderItemId)).thenReturn(orderItem(orderItemId, 500L, "160"));
        when(sOrdersService.getOrderById(500L)).thenReturn(completedOrder(500L, userId));

        SizeFeedback existing = new SizeFeedback();
        existing.setFeedbackId(9L);
        existing.setOrderItemId(orderItemId);
        existing.setRecommendedSize("165");
        when(sizeFeedbackMapper.selectByOrderItemId(orderItemId)).thenReturn(existing);

        SizeFeedbackRequest request = new SizeFeedbackRequest();
        request.setPurchasedSize("160");
        request.setSatisfaction("FIT");

        service.saveFeedback(userId, orderItemId, request);

        verify(sizeFeedbackMapper).update(any(SizeFeedback.class));
        verify(sizeFeedbackMapper, never()).insert(any(SizeFeedback.class));
    }

    @Test
    void shouldRejectInvalidSatisfactionBeforeWriting() {
        SizeFeedbackRequest request = new SizeFeedbackRequest();
        request.setSatisfaction("BAD");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.saveFeedback(42L, 77L, request));

        verify(sizeFeedbackMapper, never()).insert(any(SizeFeedback.class));
        verify(sizeFeedbackMapper, never()).update(any(SizeFeedback.class));
    }

    @Test
    void shouldReconstructRecommendationSnapshotFromStoredLogFieldsWhenSummaryIsUnreadable() {
        Long userId = 42L;
        Long orderItemId = 88L;

        when(sOrderItemMapper.selectOrderItemById(orderItemId)).thenReturn(orderItem(orderItemId, 600L, "165"));
        when(sOrdersService.getOrderById(600L)).thenReturn(completedOrder(600L, userId));

        RecommendationLog linkedRecommendation = new RecommendationLog();
        linkedRecommendation.setLogId(123L);
        linkedRecommendation.setRecommendedSizeId(5L);
        linkedRecommendation.setRecommendedSizeName("165");
        linkedRecommendation.setConfidenceScore(72);
        linkedRecommendation.setConfidenceLevel("MEDIUM");
        linkedRecommendation.setCalibrationApplied(false);
        linkedRecommendation.setCreateTime(new Date());
        linkedRecommendation.setResultSummary("{not-json}");
        when(recommendationLogMapper.selectByOrderItemId(orderItemId)).thenReturn(linkedRecommendation);

        SizeRecommendationResponse response = service.getRecommendationSnapshot(userId, orderItemId);

        assertNotNull(response);
        assertTrue(response.isAvailable());
        assertEquals(123L, response.getRecommendationLogId());
        assertEquals(5L, response.getRecommended().getSizeId());
        assertEquals("165", response.getRecommended().getSizeName());
        assertTrue(response.getMessage().contains("下单时关联"));
        assertTrue(response.getReasons().stream().anyMatch(reason -> reason.contains("历史快照")));
    }

    @Test
    void shouldReconstructPersonalizationSnapshotFromStoredLogFieldsWhenSummaryIsUnreadable() {
        Long userId = 42L;
        Long orderItemId = 90L;

        when(sOrderItemMapper.selectOrderItemById(orderItemId)).thenReturn(orderItem(orderItemId, 602L, "170"));
        when(sOrdersService.getOrderById(602L)).thenReturn(completedOrder(602L, userId));

        RecommendationLog linkedRecommendation = new RecommendationLog();
        linkedRecommendation.setLogId(124L);
        linkedRecommendation.setRecommendedSizeId(6L);
        linkedRecommendation.setRecommendedSizeName("170");
        linkedRecommendation.setConfidenceScore(80);
        linkedRecommendation.setConfidenceLevel("HIGH");
        linkedRecommendation.setCalibrationApplied(false);
        linkedRecommendation.setPersonalizationApplied(true);
        linkedRecommendation.setCategoryKey("JACKET");
        linkedRecommendation.setPersonalizationDetails("{\"applied\":true}");
        linkedRecommendation.setResultSummary("{not-json}");
        when(recommendationLogMapper.selectByOrderItemId(orderItemId)).thenReturn(linkedRecommendation);

        SizeRecommendationResponse response = service.getRecommendationSnapshot(userId, orderItemId);

        assertTrue(response.isPersonalizationApplied());
        assertEquals("JACKET", response.getPersonalizationDetails().get("categoryKey"));
        assertTrue(response.getReasons().stream().anyMatch(reason -> reason.contains("历史穿着反馈")));
    }

    @Test
    void shouldReturnNullWhenNoLinkedRecommendationSnapshotExists() {
        Long userId = 42L;
        Long orderItemId = 89L;

        when(sOrderItemMapper.selectOrderItemById(orderItemId)).thenReturn(orderItem(orderItemId, 601L, "165"));
        when(sOrdersService.getOrderById(601L)).thenReturn(completedOrder(601L, userId));
        when(recommendationLogMapper.selectByOrderItemId(orderItemId)).thenReturn(null);

        assertNull(service.getRecommendationSnapshot(userId, orderItemId));
    }

    private SOrderItem orderItem(Long orderItemId, Long orderId, String sizeNameSnapshot) {
        SOrderItem orderItem = new SOrderItem();
        orderItem.setOrderItemId(orderItemId);
        orderItem.setOrderId(orderId);
        orderItem.setSizeNameSnapshot(sizeNameSnapshot);
        return orderItem;
    }

    private SOrders completedOrder(Long orderId, Long userId) {
        SOrders order = new SOrders();
        order.setId(orderId);
        order.setUserId(userId);
        order.setStatus(3L);
        return order;
    }
}
