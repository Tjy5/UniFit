package com.authguard.usersystem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.authguard.usersystem.service.ISUserActivityLogService;
import com.authguard.usersystem.service.UserMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(UserMessageController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserMessageService userMessageService;

    @MockitoBean
    private ISUserActivityLogService activityLogService;

    @Test
    void shouldAcceptExtendedMeasurements() throws Exception {
        when(userMessageService.updateOrInsertUserMessage(any())).thenReturn(true);

        mockMvc.perform(put("/api/user-message/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messageUserId": 29,
                                  "height": 165.0,
                                  "weight": 52.0,
                                  "chest": 84.0,
                                  "waist": 72.0,
                                  "hip": 90.0,
                                  "shoulder": 39.0
                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新成功"));
    }

    @Test
    void shouldReturnBadRequestWhenMeasurementValidationFails() throws Exception {
        when(userMessageService.updateOrInsertUserMessage(any())).thenThrow(new IllegalArgumentException("胸围范围应在 50-150cm 之间"));

        mockMvc.perform(put("/api/user-message/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messageUserId": 29,
                                  "chest": 200.0
                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("胸围范围应在 50-150cm 之间"));
    }
}
