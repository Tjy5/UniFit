package com.authguard.usersystem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.authguard.usersystem.dto.SizeFeedbackResponse;
import com.authguard.usersystem.dto.SizeRecommendationCandidateDto;
import com.authguard.usersystem.dto.SizeRecommendationResponse;
import com.authguard.usersystem.security.CurrentUserResolver;
import com.authguard.usersystem.service.ISUserActivityLogService;
import com.authguard.usersystem.service.SizeFeedbackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(SizeFeedbackController.class)
@AutoConfigureMockMvc(addFilters = false)
class SizeFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SizeFeedbackService sizeFeedbackService;

    @MockitoBean
    private CurrentUserResolver currentUserResolver;

    @MockitoBean
    private ISUserActivityLogService activityLogService;

    @Test
    void shouldSaveFeedbackForCompletedOrderItem() throws Exception {
        when(currentUserResolver.requireUserId(any())).thenReturn(42L);

        SizeFeedbackResponse response = new SizeFeedbackResponse();
        response.setOrderItemId(12L);
        response.setSatisfaction("TOO_SMALL");
        response.setPurchasedSize("160");
        response.setRecommendedSize("165");
        when(sizeFeedbackService.saveFeedback(eq(42L), eq(12L), any())).thenReturn(response);

        mockMvc.perform(put("/api/size-feedback/order-items/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recommendedSize": "165",
                                  "purchasedSize": "160",
                                  "satisfaction": "TOO_SMALL",
                                  "issueParts": ["shoulder", "length"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderItemId").value(12))
                .andExpect(jsonPath("$.data.satisfaction").value("TOO_SMALL"))
                .andExpect(jsonPath("$.data.recommendedSize").value("165"));
    }

    @Test
    void shouldReturnRecommendationSnapshotForOrderItem() throws Exception {
        when(currentUserResolver.requireUserId(any())).thenReturn(42L);

        SizeRecommendationCandidateDto candidate = new SizeRecommendationCandidateDto();
        candidate.setSizeId(5L);
        candidate.setSizeName("165");
        SizeRecommendationResponse response = new SizeRecommendationResponse();
        response.setAvailable(true);
        response.setRecommendationLogId(99L);
        response.setRecommended(candidate);
        response.setMessage("以下展示的是您下单时关联的推荐快照");
        when(sizeFeedbackService.getRecommendationSnapshot(42L, 12L)).thenReturn(response);

        mockMvc.perform(get("/api/size-feedback/order-items/12/recommendation")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.recommendationLogId").value(99))
                .andExpect(jsonPath("$.data.recommended.sizeName").value("165"));
    }

    @Test
    void shouldRejectInvalidSatisfaction() throws Exception {
        mockMvc.perform(put("/api/size-feedback/order-items/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "satisfaction": "BAD"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("satisfaction")));
    }
}
