package com.authguard.usersystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private long expiration = 30 * 60 * 1000L;
}
