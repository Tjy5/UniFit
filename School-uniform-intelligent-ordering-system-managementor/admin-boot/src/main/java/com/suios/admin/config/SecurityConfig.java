package com.suios.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suios.admin.auth.jwt.JwtAuthFilter;
import com.suios.admin.common.result.R;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(
                                "/error",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/upload/**",
                                "/profile/upload/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, exception) ->
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, R.unauthorized("Authentication required")))
                        .accessDeniedHandler((request, response, exception) ->
                                writeJson(response, HttpServletResponse.SC_FORBIDDEN, R.forbidden("Access denied")))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    private void writeJson(HttpServletResponse response, int status, R<Void> body) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
