package com.authguard.usersystem.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.authguard.usersystem.security.CurrentUserResolver;
import com.authguard.usersystem.service.ISReviewService;
import com.authguard.usersystem.service.ISUserActivityLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class SReviewControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ISReviewService reviewService;

    @MockitoBean
    private CurrentUserResolver currentUserResolver;

    @MockitoBean
    private ISUserActivityLogService activityLogService;

    @Test
    void shouldRejectRatingOutsideAllowedRange() throws Exception {
        mockMvc.perform(post("/api/s-reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "uniformId": 1,
                                  "orderItemId": 2,
                                  "rating": 6,
                                  "content": "ok",
                                  "isAnonymous": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("评分最高")));
    }
}
