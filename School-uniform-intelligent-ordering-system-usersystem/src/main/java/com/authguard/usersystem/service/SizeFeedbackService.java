package com.authguard.usersystem.service;

import com.authguard.usersystem.dto.SizeFeedbackRequest;
import com.authguard.usersystem.dto.SizeFeedbackResponse;
import com.authguard.usersystem.dto.SizeRecommendationResponse;

public interface SizeFeedbackService {
    SizeFeedbackResponse getFeedback(Long userId, Long orderItemId);

    SizeFeedbackResponse saveFeedback(Long userId, Long orderItemId, SizeFeedbackRequest request);

    SizeRecommendationResponse getRecommendationSnapshot(Long userId, Long orderItemId);
}
