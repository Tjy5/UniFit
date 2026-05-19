package com.authguard.usersystem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.authguard.usersystem.dto.SizePreferenceProfileDto;
import com.authguard.usersystem.security.CurrentUserResolver;
import com.authguard.usersystem.service.ISUserActivityLogService;
import com.authguard.usersystem.service.SizePreferenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SizePreferenceController.class)
@AutoConfigureMockMvc(addFilters = false)
class SizePreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SizePreferenceService sizePreferenceService;

    @MockitoBean
    private CurrentUserResolver currentUserResolver;

    @MockitoBean
    private ISUserActivityLogService activityLogService;

    @Test
    void shouldReadExplicitPreference() throws Exception {
        when(currentUserResolver.requireUserId(any())).thenReturn(42L);
        SizePreferenceProfileDto dto = new SizePreferenceProfileDto();
        dto.setUserId(42L);
        dto.setCategoryKey("JACKET");
        dto.setExplicitPreference("LOOSE");
        dto.setEffectivePreference("LOOSE");
        when(sizePreferenceService.getPreference(eq(42L), eq(88L), any())).thenReturn(dto);

        mockMvc.perform(get("/api/size-preferences").param("uniformId", "88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryKey").value("JACKET"))
                .andExpect(jsonPath("$.data.explicitPreference").value("LOOSE"));
    }

    @Test
    void shouldSaveExplicitPreference() throws Exception {
        when(currentUserResolver.requireUserId(any())).thenReturn(42L);
        SizePreferenceProfileDto dto = new SizePreferenceProfileDto();
        dto.setUserId(42L);
        dto.setCategoryKey("JACKET");
        dto.setExplicitPreference("SLIM");
        dto.setEffectivePreference("SLIM");
        when(sizePreferenceService.saveExplicitPreference(eq(42L), any())).thenReturn(dto);

        mockMvc.perform(put("/api/size-preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "uniformId": 88,
                                  "fitPreference": "SLIM"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effectivePreference").value("SLIM"));
    }

    @Test
    void shouldRejectInvalidPreference() throws Exception {
        mockMvc.perform(put("/api/size-preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fitPreference": "BAGGY"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("fitPreference")));
    }
}
