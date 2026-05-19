package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.RecommendationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecommendationLogMapper {
    int insert(RecommendationLog recommendationLog);

    RecommendationLog selectByLogIdAndUserId(@Param("logId") Long logId, @Param("userId") Long userId);

    RecommendationLog selectByOrderItemId(@Param("orderItemId") Long orderItemId);

    int updateOrderItemLink(@Param("logId") Long logId,
                            @Param("userId") Long userId,
                            @Param("orderItemId") Long orderItemId);
}
