package com.authguard.usersystem.dto;

import com.authguard.usersystem.entity.RecommendationLog;
import com.authguard.usersystem.entity.SOrderItem;
import com.authguard.usersystem.entity.SOrders;
import com.authguard.usersystem.entity.SizeFeedback;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SizePreferenceEventInput {
    private Long userId;
    private SizeFeedback feedback;
    private SOrderItem orderItem;
    private SOrders order;
    private RecommendationLog recommendationLog;
}
