package com.authguard.usersystem.service;

import com.authguard.usersystem.dto.SizeRecommendationRequest;
import com.authguard.usersystem.dto.SizeRecommendationResponse;

public interface SizeRecommendationService {
    SizeRecommendationResponse recommend(Long userId, SizeRecommendationRequest request);
}
