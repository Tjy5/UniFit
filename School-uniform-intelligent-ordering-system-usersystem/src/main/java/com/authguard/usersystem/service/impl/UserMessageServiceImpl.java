package com.authguard.usersystem.service.impl;

import com.authguard.usersystem.entity.UserMessage;
import com.authguard.usersystem.mapper.UserMessageMapper;
import com.authguard.usersystem.service.UserMessageService;
import java.math.BigDecimal;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserMessageServiceImpl implements UserMessageService {

    @Autowired
    private UserMessageMapper userMessageMapper;

    private static final BigDecimal HEIGHT_MIN = new BigDecimal("50");
    private static final BigDecimal HEIGHT_MAX = new BigDecimal("250");
    private static final BigDecimal WEIGHT_MIN = new BigDecimal("10");
    private static final BigDecimal WEIGHT_MAX = new BigDecimal("200");
    private static final BigDecimal CHEST_MIN = new BigDecimal("50");
    private static final BigDecimal CHEST_MAX = new BigDecimal("150");
    private static final BigDecimal WAIST_MIN = new BigDecimal("45");
    private static final BigDecimal WAIST_MAX = new BigDecimal("150");
    private static final BigDecimal HIP_MIN = new BigDecimal("60");
    private static final BigDecimal HIP_MAX = new BigDecimal("170");
    private static final BigDecimal SHOULDER_MIN = new BigDecimal("25");
    private static final BigDecimal SHOULDER_MAX = new BigDecimal("70");

    @Override
    public UserMessage getUserMessageByUserId(Long messageUserId) {
        return userMessageMapper.selectByMessageUserId(messageUserId);
    }

    @Override
    public boolean updateOrInsertUserMessage(UserMessage userMessage) {
        validateMeasurements(userMessage);
        UserMessage existingMessage = userMessageMapper.selectByMessageUserId(userMessage.getMessageUserId());

        if (existingMessage != null) {
            userMessage.setMessageId(existingMessage.getMessageId());
            userMessage.setCreateBy(existingMessage.getCreateBy());
            userMessage.setCreateTime(existingMessage.getCreateTime());
            userMessage.setUpdateBy(userMessage.getUpdateBy() != null ? userMessage.getUpdateBy() : userMessage.getMessageUserId().toString());
            userMessage.setUpdateTime(new Date());
            return userMessageMapper.updateUserMessage(userMessage) > 0;
        } else {
            userMessage.setMessageId(null);
            userMessage.setCreateBy(userMessage.getCreateBy() != null ? userMessage.getCreateBy() : userMessage.getMessageUserId().toString());
            userMessage.setCreateTime(new Date());
            userMessage.setUpdateBy(userMessage.getUpdateBy() != null ? userMessage.getUpdateBy() : userMessage.getMessageUserId().toString());
            userMessage.setUpdateTime(new Date());
            return userMessageMapper.insertUserMessage(userMessage) > 0;
        }
    }

    private void validateMeasurements(UserMessage userMessage) {
        validateRange(userMessage.getHeight(), HEIGHT_MIN, HEIGHT_MAX, "身高");
        validateRange(userMessage.getWeight(), WEIGHT_MIN, WEIGHT_MAX, "体重");
        validateRange(userMessage.getChest(), CHEST_MIN, CHEST_MAX, "胸围");
        validateRange(userMessage.getWaist(), WAIST_MIN, WAIST_MAX, "腰围");
        validateRange(userMessage.getHip(), HIP_MIN, HIP_MAX, "臀围");
        validateRange(userMessage.getShoulder(), SHOULDER_MIN, SHOULDER_MAX, "肩宽");
    }

    private void validateRange(BigDecimal value, BigDecimal min, BigDecimal max, String label) {
        if (value == null) {
            return;
        }
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new IllegalArgumentException(label + "范围应在 " + min.stripTrailingZeros().toPlainString()
                    + "-" + max.stripTrailingZeros().toPlainString() + "cm 之间");
        }
    }
}
