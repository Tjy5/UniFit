package com.authguard.usersystem.service;

import com.authguard.usersystem.dto.SizePreferenceAdjustmentResult;
import com.authguard.usersystem.dto.SizePreferenceEventInput;
import com.authguard.usersystem.dto.SizePreferenceProfileDto;
import com.authguard.usersystem.dto.SizePreferenceRequest;
import com.authguard.usersystem.entity.SSizes;
import java.util.List;

public interface SizePreferenceService {
    String GENERAL_CATEGORY = "GENERAL";

    String resolveCategoryKey(Long uniformId);

    SizePreferenceProfileDto getPreference(Long userId, Long uniformId, String categoryKey);

    SizePreferenceProfileDto saveExplicitPreference(Long userId, SizePreferenceRequest request);

    void learnFromFeedback(SizePreferenceEventInput input);

    SizePreferenceAdjustmentResult buildAdjustment(Long userId, String categoryKey, List<SSizes> sizeOrder);

    int backfillCompletedFeedback();
}
