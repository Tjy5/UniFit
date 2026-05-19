package com.authguard.usersystem;

import com.authguard.usersystem.config.CalibrationProperties;
import com.authguard.usersystem.config.JwtProperties;
import com.authguard.usersystem.config.SizeRecommendationExperimentProperties;
import com.authguard.usersystem.config.SizePreferenceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({
        CalibrationProperties.class,
        JwtProperties.class,
        SizePreferenceProperties.class,
        SizeRecommendationExperimentProperties.class
})
public class UsersystemApplication {

    private static final Logger log = LoggerFactory.getLogger(UsersystemApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(UsersystemApplication.class, args);
        log.info("UsersystemApplication started successfully");
    }

}
