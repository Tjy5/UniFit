package com.authguard.usersystem.service;

import com.authguard.usersystem.dto.SWishlistDetailDto;

import java.util.List;

public interface SWishlistService {
    boolean addToWishlist(Long userId, Long uniformId);
    boolean removeFromWishlist(Long userId, Long uniformId);
    List<SWishlistDetailDto> getWishlistByUserId(Long userId);
}
