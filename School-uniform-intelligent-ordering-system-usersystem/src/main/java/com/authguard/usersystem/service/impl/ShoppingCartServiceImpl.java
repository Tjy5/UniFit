package com.authguard.usersystem.service.impl;

import com.authguard.usersystem.entity.SUniform; // Assuming you have this
import com.authguard.usersystem.entity.ShoppingCartItem;
import com.authguard.usersystem.mapper.RecommendationLogMapper;
import com.authguard.usersystem.mapper.SUniformMapper; // Assuming you have this
import com.authguard.usersystem.mapper.ShoppingCartItemMapper;
import com.authguard.usersystem.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartItemMapper cartItemMapper;

    @Autowired
    private SUniformMapper uniformMapper; // To check uniform validity

    @Autowired
    private RecommendationLogMapper recommendationLogMapper;

    @Override
    @Transactional
    public ShoppingCartItem addItemToCart(Long userId, Long uniformId, Long sizeId, int quantity, Long recommendationLogId) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        validateRecommendationLog(userId, recommendationLogId);
        // Optional: Validate uniformId and sizeId exist
        SUniform uniform = uniformMapper.selectSUniformById(uniformId); // You need a method in SUniformMapper
        if (uniform == null || uniform.getStatus() != 0) { // Assuming status 0 is available
            throw new IllegalArgumentException("Uniform not available.");
        }
        // Optional: Validate sizeId against s_sizes if needed

        ShoppingCartItem existingItem = cartItemMapper.selectByUserIdAndUniformIdAndSizeId(userId, uniformId, sizeId);
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            existingItem.setRecommendationLogId(recommendationLogId != null ? recommendationLogId : existingItem.getRecommendationLogId());
            cartItemMapper.updateQuantity(existingItem);
            return existingItem;
        } else {
            ShoppingCartItem newItem = new ShoppingCartItem();
            newItem.setUserId(userId);
            newItem.setUniformId(uniformId);
            newItem.setSizeId(sizeId);
            newItem.setRecommendationLogId(recommendationLogId);
            newItem.setQuantity(quantity);
            newItem.setAddedAt(new Date());
            cartItemMapper.insert(newItem);
            return newItem;
        }
    }

    private void validateRecommendationLog(Long userId, Long recommendationLogId) {
        if (recommendationLogId == null) {
            return;
        }
        if (recommendationLogMapper.selectByLogIdAndUserId(recommendationLogId, userId) == null) {
            throw new IllegalArgumentException("Recommendation log not found for current user.");
        }
    }

    @Override
    public List<ShoppingCartItem> getCartItems(Long userId) {
        return cartItemMapper.selectByUserId(userId);
    }

    @Override
    @Transactional
    public ShoppingCartItem updateCartItemQuantity(Long userId, Long cartItemId, int quantity) {
        if (quantity <= 0) { // If quantity is 0 or less, remove the item
            removeItemFromCart(userId, cartItemId);
            return null;
        }
        // It's good practice to fetch the item first to ensure it belongs to the user
        ShoppingCartItem itemToUpdate = null;
        List<ShoppingCartItem> userCartItems = cartItemMapper.selectByUserId(userId);
        for(ShoppingCartItem item : userCartItems){
            if(item.getCartItemId().equals(cartItemId)){
                itemToUpdate = item;
                break;
            }
        }

        if (itemToUpdate == null) {
            throw new IllegalArgumentException("Cart item not found or does not belong to user.");
        }
        itemToUpdate.setQuantity(quantity);
        cartItemMapper.updateQuantity(itemToUpdate);
        return itemToUpdate;
    }

    @Override
    @Transactional
    public boolean removeItemFromCart(Long userId, Long cartItemId) {
        // Validate item belongs to user before deleting for security
        return cartItemMapper.deleteByUserIdAndCartItemId(userId, cartItemId) > 0;
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        cartItemMapper.deleteByUserId(userId);
    }
}
