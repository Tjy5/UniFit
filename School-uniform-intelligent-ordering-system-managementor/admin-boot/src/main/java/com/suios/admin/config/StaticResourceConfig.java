package com.suios.admin.config;

import com.suios.admin.upload.config.UploadProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

    public StaticResourceConfig(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(uploadProperties.getUrlPrefix() + "/**")
                .addResourceLocations("file:" + appendSlash(uploadProperties.getPath()));
        registry.addResourceHandler("/profile/upload/**")
                .addResourceLocations("file:" + appendSlash(uploadProperties.getLegacyPath()));
    }

    private String appendSlash(String path) {
        return path.endsWith("/") ? path : path + "/";
    }
}
