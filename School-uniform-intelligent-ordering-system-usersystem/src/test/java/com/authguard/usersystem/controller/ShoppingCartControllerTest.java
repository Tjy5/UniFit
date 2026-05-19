package com.authguard.usersystem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.authguard.usersystem.entity.ShoppingCartItem;
import com.authguard.usersystem.security.CurrentUserResolver;
import com.authguard.usersystem.service.ISUserActivityLogService;
import com.authguard.usersystem.service.ShoppingCartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(ShoppingCartController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShoppingCartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShoppingCartService shoppingCartService;

    @MockitoBean
    private CurrentUserResolver currentUserResolver;

    @MockitoBean
    private ISUserActivityLogService activityLogService;

    @Test
    void shouldAcceptRecommendationLogIdWhenAddingToCart() throws Exception {
        when(currentUserResolver.requireUserId(any())).thenReturn(42L);

        ShoppingCartItem item = new ShoppingCartItem();
        item.setCartItemId(1L);
        item.setUniformId(100L);
        item.setSizeId(5L);
        item.setRecommendationLogId(99L);
        item.setQuantity(2);
        when(shoppingCartService.addItemToCart(42L, 100L, 5L, 2, 99L)).thenReturn(item);

        mockMvc.perform(post("/api/cart/add")
                        .param("uniformId", "100")
                        .param("sizeId", "5")
                        .param("quantity", "2")
                        .param("recommendationLogId", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.recommendationLogId").value(99));
    }

    @Test
    void shouldRejectInvalidQuantity() throws Exception {
        mockMvc.perform(post("/api/cart/add")
                        .param("uniformId", "100")
                        .param("sizeId", "5")
                        .param("quantity", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("quantity")));
    }
}
