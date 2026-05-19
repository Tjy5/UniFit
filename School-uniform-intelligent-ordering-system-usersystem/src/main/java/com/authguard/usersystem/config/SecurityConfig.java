package com.authguard.usersystem.config;

import com.authguard.usersystem.util.JwtAuthenticationFilter;
import com.authguard.usersystem.util.JwtUtils;
import com.authguard.usersystem.security.RestAccessDeniedHandler;
import com.authguard.usersystem.security.RestAuthenticationEntryPoint;
import com.authguard.usersystem.service.UserAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/users/register", "/api/users/login", "/api/auth/validate").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/s-schools/selectList").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/s-grades/listBySchool", "/api/s-grades/listAll").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/s-sizes/all").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/s-uniform/active").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/s-style-guides/active", "/api/s-style-guides/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/s-reviews/uniform/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtils jwtUtils,
                                                           UserAccountService userAccountService,
                                                           ObjectMapper objectMapper) {
        return new JwtAuthenticationFilter(jwtUtils, userAccountService, objectMapper);
    }

    @Bean
    public RestAuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return new RestAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public RestAccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return new RestAccessDeniedHandler(objectMapper);
    }
}
