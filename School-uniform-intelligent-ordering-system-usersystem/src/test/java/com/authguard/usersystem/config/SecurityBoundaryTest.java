package com.authguard.usersystem.config;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.authguard.usersystem.controller.SSchoolController;
import com.authguard.usersystem.controller.ShoppingCartController;
import com.authguard.usersystem.exception.GlobalExceptionHandler;
import com.authguard.usersystem.security.CurrentUserResolver;
import com.authguard.usersystem.service.ISSchoolService;
import com.authguard.usersystem.service.ISUserActivityLogService;
import com.authguard.usersystem.service.ShoppingCartService;
import com.authguard.usersystem.service.UserAccountService;
import com.authguard.usersystem.util.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {SSchoolController.class, ShoppingCartController.class})
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtUtils.class, CurrentUserResolver.class, GlobalExceptionHandler.class})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "jwt.secret=test-usersystem-jwt-secret-long-enough-for-hs256",
        "jwt.expiration=1800000"
})
class SecurityBoundaryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockitoBean
    private UserAccountService userAccountService;

    @MockitoBean
    private ISSchoolService schoolService;

    @MockitoBean
    private ShoppingCartService shoppingCartService;

    @MockitoBean
    private ISUserActivityLogService activityLogService;

    @Test
    void shouldAllowPublicBrowseEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/s-schools/selectList"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldAllowProtectedEndpointWithValidToken() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + jwtUtils.generateToken(42L, "student")))
                .andExpect(status().isOk());

        verify(userAccountService, never()).getUserByAccount("student");
        verify(shoppingCartService).getCartItems(42L);
    }
}
