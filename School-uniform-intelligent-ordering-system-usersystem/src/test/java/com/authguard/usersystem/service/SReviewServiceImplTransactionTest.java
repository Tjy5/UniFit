package com.authguard.usersystem.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.authguard.usersystem.service.impl.SReviewServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class SReviewServiceImplTransactionTest {

    @Test
    void updateUniformRatingAndCountShouldUseWriteTransaction() throws Exception {
        Transactional transactional = SReviewServiceImpl.class
                .getMethod("updateUniformRatingAndCount", Long.class)
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertFalse(transactional.readOnly());
    }
}
