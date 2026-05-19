package com.authguard.usersystem.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.authguard.usersystem.security.CurrentUserResolver;
import com.authguard.usersystem.service.ISUserActivityLogService;
import com.authguard.usersystem.service.SOrdersService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SOrdersController.class)
@AutoConfigureMockMvc(addFilters = false)
class SOrdersControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SOrdersService ordersService;

    @MockitoBean
    private CurrentUserResolver currentUserResolver;

    @MockitoBean
    private ISUserActivityLogService activityLogService;

    @Test
    void shouldRejectCreateOrderWithoutAddressId() throws Exception {
        mockMvc.perform(post("/api/s-orders/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("addressId")));
    }

    @Test
    void shouldRejectInvalidOrderStatus() throws Exception {
        mockMvc.perform(put("/api/s-orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("orderStatus")));
    }
}
