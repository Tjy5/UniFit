package com.authguard.usersystem.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.authguard.usersystem.security.CurrentUserResolver;
import com.authguard.usersystem.service.ISAddressService;
import com.authguard.usersystem.service.ISUserActivityLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SAddressController.class)
@AutoConfigureMockMvc(addFilters = false)
class SAddressControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ISAddressService addressService;

    @MockitoBean
    private CurrentUserResolver currentUserResolver;

    @MockitoBean
    private ISUserActivityLogService activityLogService;

    @Test
    void shouldRejectAddressWithoutRequiredFields() throws Exception {
        mockMvc.perform(post("/api/s-addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientName": "",
                                  "phoneNumber": "123",
                                  "province": "",
                                  "city": "",
                                  "district": "",
                                  "streetAddress": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("收货人姓名")));
    }
}
