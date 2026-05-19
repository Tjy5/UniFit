package com.authguard.usersystem.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.authguard.usersystem.service.UserAccountService;
import com.authguard.usersystem.util.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserAccountControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAccountService userAccountService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @Test
    void shouldRejectLoginWithShortPassword() throws Exception {
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userAccount": "student",
                                  "userPassword": "123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("密码长度")));
    }
}
