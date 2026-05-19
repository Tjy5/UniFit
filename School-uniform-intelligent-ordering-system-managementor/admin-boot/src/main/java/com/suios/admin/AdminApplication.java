package com.suios.admin;

import com.suios.admin.upload.config.UploadProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@MapperScan(basePackages = {
        "com.suios.admin.auth.mapper",
        "com.suios.admin.analytics.mapper",
        "com.suios.admin.school.mapper",
        "com.suios.admin.grade.mapper",
        "com.suios.admin.size.mapper",
        "com.suios.admin.uniform.mapper",
        "com.suios.admin.order.mapper",
        "com.suios.admin.inventory.mapper",
        "com.suios.admin.review.mapper",
        "com.suios.admin.guide.mapper",
        "com.suios.admin.log.mapper",
        "com.suios.admin.activitylog.mapper",
        "com.suios.admin.dashboard.mapper",
        "com.suios.admin.calibration.mapper",
        "com.suios.admin.analytics.mapper"
})
@SpringBootApplication
@EnableConfigurationProperties(UploadProperties.class)
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
