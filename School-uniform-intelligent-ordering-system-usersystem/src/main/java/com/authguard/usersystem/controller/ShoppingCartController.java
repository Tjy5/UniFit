package com.authguard.usersystem.controller;

import com.authguard.usersystem.dto.ApiResponse;
import com.authguard.usersystem.dto.CartItemRequestDto;
import com.authguard.usersystem.entity.ShoppingCartItem;
import com.authguard.usersystem.exception.NotFoundException;
import com.authguard.usersystem.security.CurrentUserResolver;
import com.authguard.usersystem.service.ISUserActivityLogService;
import com.authguard.usersystem.service.ShoppingCartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/cart")
public class ShoppingCartController {

    private static final Logger log = LoggerFactory.getLogger(ShoppingCartController.class);

    private final ShoppingCartService shoppingCartService;
    private final CurrentUserResolver currentUserResolver;
    private final ISUserActivityLogService activityLogService;

    public ShoppingCartController(ShoppingCartService shoppingCartService,
                                  CurrentUserResolver currentUserResolver,
                                  ISUserActivityLogService activityLogService) {
        this.shoppingCartService = shoppingCartService;
        this.currentUserResolver = currentUserResolver;
        this.activityLogService = activityLogService;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<ShoppingCartItem>> addItem(
            @RequestParam @NotNull Long uniformId,
            @RequestParam @NotNull Long sizeId,
            @RequestParam @Min(value = 1, message = "quantity必须大于等于1") int quantity,
            @RequestParam(required = false) Long recommendationLogId,
            HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        String targetId = "uniformId=" + uniformId + ",sizeId=" + sizeId;
        try {
            ShoppingCartItem item = shoppingCartService.addItemToCart(userId, uniformId, sizeId, quantity, recommendationLogId);
            activityLogService.recordActivityAsync(userId, "ADD_TO_CART_SUCCESS", "UNIFORM", uniformId.toString(),
                    "sizeId:" + sizeId + ",quantity:" + quantity + ",recommendationLogId:" + recommendationLogId, request);
            return ResponseEntity.ok(ApiResponse.success(item));
        } catch (RuntimeException ex) {
            activityLogService.recordActivityAsync(userId, "ADD_TO_CART_FAILED", "CART_REQUEST_ADD", targetId, ex.getMessage(), request);
            throw ex;
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShoppingCartItem>> addItemFromBody(@Valid @RequestBody CartItemRequestDto dto,
                                                                          HttpServletRequest request) {
        return addItem(dto.getUniformId(), dto.getSizeId(), dto.getQuantity(), dto.getRecommendationLogId(), request);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShoppingCartItem>>> getCart(HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        List<ShoppingCartItem> cartItems = shoppingCartService.getCartItems(userId);
        activityLogService.recordActivityAsync(userId, "VIEW_CART", "CART", null, null, request);
        return ResponseEntity.ok(ApiResponse.success(cartItems));
    }

    @PutMapping("/item/{cartItemId}")
    public ResponseEntity<ApiResponse<ShoppingCartItem>> updateItemQuantity(@PathVariable Long cartItemId,
                                                                            @RequestParam @Min(value = 1, message = "quantity必须大于等于1") int quantity,
                                                                            HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        ShoppingCartItem item = shoppingCartService.updateCartItemQuantity(userId, cartItemId, quantity);
        activityLogService.recordActivityAsync(userId, "UPDATE_CART_ITEM_SUCCESS", "CART_ITEM", cartItemId.toString(),
                "new_quantity:" + quantity, request);
        return ResponseEntity.ok(ApiResponse.success(item));
    }

    @DeleteMapping("/item/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(@PathVariable Long cartItemId, HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        boolean removed = shoppingCartService.removeItemFromCart(userId, cartItemId);
        if (!removed) {
            activityLogService.recordActivityAsync(userId, "REMOVE_CART_ITEM_FAILED", "CART_ITEM", cartItemId.toString(), "Not found", request);
            throw new NotFoundException("Item not found or could not be removed.");
        }
        activityLogService.recordActivityAsync(userId, "REMOVE_CART_ITEM_SUCCESS", "CART_ITEM", cartItemId.toString(), null, request);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart.", null));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(HttpServletRequest request) {
        Long userId = currentUserResolver.requireUserId(request);
        shoppingCartService.clearCart(userId);
        activityLogService.recordActivityAsync(userId, "CLEAR_CART_SUCCESS", "CART", null, null, request);
        log.debug("Cart cleared for user {}", userId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared.", null));
    }
}
