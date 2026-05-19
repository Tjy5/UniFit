package com.authguard.usersystem.mapper;

import com.authguard.usersystem.dto.SizePreferenceBackfillRecord;
import com.authguard.usersystem.entity.UserSizePreferenceEvent;
import com.authguard.usersystem.entity.UserSizePreferenceProfile;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserSizePreferenceMapper {
    UserSizePreferenceProfile selectProfile(@Param("userId") Long userId,
                                            @Param("categoryKey") String categoryKey);

    int upsertProfile(UserSizePreferenceProfile profile);

    int upsertEvent(UserSizePreferenceEvent event);

    List<UserSizePreferenceEvent> selectEventsByUserAndCategory(@Param("userId") Long userId,
                                                                @Param("categoryKey") String categoryKey);

    List<SizePreferenceBackfillRecord> selectCompletedFeedbackBackfillRecords();
}
