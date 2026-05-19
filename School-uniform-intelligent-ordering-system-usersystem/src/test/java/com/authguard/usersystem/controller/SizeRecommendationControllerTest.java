package com.authguard.usersystem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.authguard.usersystem.dto.SizeRecommendationCandidateDto;
import com.authguard.usersystem.dto.SizeRecommendationResponse;
import com.authguard.usersystem.security.CurrentUserResolver;
import com.authguard.usersystem.service.ISUserActivityLogService;
import com.authguard.usersystem.service.SizeRecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(SizeRecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class SizeRecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SizeRecommendationService sizeRecommendationService;

    @MockitoBean
    private CurrentUserResolver currentUserResolver;

    @MockitoBean
    private ISUserActivityLogService activityLogService;

    @Test
    void shouldReturnRecommendationPayload() throws Exception {
        when(currentUserResolver.requireUserId(any())).thenReturn(42L);

        SizeRecommendationCandidateDto candidate = new SizeRecommendationCandidateDto();
        candidate.setSizeName("165");
        SizeRecommendationResponse response = new SizeRecommendationResponse();
        response.setAvailable(true);
        response.setConfidence(88);
        response.setRecommendationLogId(123L);
        response.setPersonalizationApplied(true);
        response.setPreferenceSummary("已参考个人偏宽松尺码偏好");
        response.setRecommended(candidate);
        response.setMessage("已基于身高、体重为您推荐 165 码");
        when(sizeRecommendationService.recommend(eq(42L), any())).thenReturn(response);

        mockMvc.perform(post("/api/size-recommendations/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "height": 165,
                                  "weight": 52
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.recommended.sizeName").value("165"))
                .andExpect(jsonPath("$.data.confidence").value(88))
                .andExpect(jsonPath("$.data.personalizationApplied").value(true))
                .andExpect(jsonPath("$.data.preferenceSummary").value("已参考个人偏宽松尺码偏好"))
                .andExpect(jsonPath("$.data.recommendationLogId").value(123));
    }

    @Test
    void shouldRejectOutOfRangeBodyData() throws Exception {
        mockMvc.perform(post("/api/size-recommendations/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "height": 260,
                                  "weight": 52
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("height")));
    }

    @Test
    void shouldRejectInvalidRecommendationSourceCharacters() throws Exception {
        mockMvc.perform(post("/api/size-recommendations/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "height": 165,
                                  "weight": 52,
                                  "source": "mall/home"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("source")));
    }
}
