package com.authguard.usersystem.service;

import com.authguard.usersystem.entity.ShoppingCartItem;
import java.util.List;

public interface ShoppingCartService {
    ShoppingCartItem addItemToCart(Long userId, Long uniformId, Long sizeId, int quantity, Long recommendationLogId);
    List<ShoppingCartItem> getCartItems(Long userId);
    ShoppingCartItem updateCartItemQuantity(Long userId, Long cartItemId, int quantity);
    boolean removeItemFromCart(Long userId, Long cartItemId);
    void clearCart(Long userId);
}
