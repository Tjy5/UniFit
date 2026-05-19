package com.authguard.usersystem.service.impl;

import com.authguard.usersystem.entity.SWishlist;
import com.authguard.usersystem.mapper.SWishlistMapper;
import com.authguard.usersystem.service.SWishlistService;
import com.authguard.usersystem.dto.SWishlistDetailDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException; // For handling unique constraint violation
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.util.Date;
import java.util.List;

@Service
public class SWishlistServiceImpl implements SWishlistService {

    @Autowired
    private SWishlistMapper wishlistMapper;

    @Transactional // Added for atomicity
    @Override
    public boolean addToWishlist(Long userId, Long uniformId) {
        // First check remains to provide a specific business logic flow (already exists)
        if (wishlistMapper.countByUserAndUniform(userId, uniformId) > 0) {
            return false; // Already wishlisted by this user
        }
        SWishlist wishlist = new SWishlist();
        wishlist.setUserId(userId);
        wishlist.setUniformId(uniformId);
        wishlist.setAddedTime(new Date());
        try {
            wishlistMapper.insertWishlist(wishlist);
            // The insertWishlist in mapper can be modified to return the generated ID if needed
            // e.g., wishlist.getWishlistId() will be populated if useGeneratedKeys is true
            return true;
        } catch (DataIntegrityViolationException e) {
            // This catch block handles the case where the unique constraint (user_id, uniform_id)
            // is violated at the database level, which means a concurrent add attempt happened.
            // Or if the primary check was somehow bypassed.
            // Log the exception: e.g., logger.warn("Data integrity violation while adding to wishlist for user {} and uniform {}", userId, uniformId, e);
            return false; // Indicate failure, possibly due to already existing (covered by check or constraint)
        }
    }

    @Transactional // Added for atomicity
    @Override
    public boolean removeFromWishlist(Long userId, Long uniformId) {
        int affectedRows = wishlistMapper.deleteWishlist(userId, uniformId);
        return affectedRows > 0; // True if one or more rows were deleted, false otherwise
    }

    @Override
    public List<SWishlistDetailDto> getWishlistByUserId(Long userId) {
        return wishlistMapper.selectWishlistWithDetailsByUserId(userId);
    }
}
