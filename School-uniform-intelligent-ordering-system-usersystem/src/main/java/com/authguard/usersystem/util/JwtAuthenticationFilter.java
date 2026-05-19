package com.authguard.usersystem.util;

import com.authguard.usersystem.dto.ApiResponse;
import com.authguard.usersystem.entity.UserAccount;
import com.authguard.usersystem.exception.UnauthenticatedException;
import com.authguard.usersystem.security.UserPrincipal;
import com.authguard.usersystem.service.UserAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtils jwtUtils;
    private final UserAccountService userAccountService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserAccountService userAccountService, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.userAccountService = userAccountService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return matches(request, "/error")
                || matches(request, "/api/users/register")
                || matches(request, "/api/users/login")
                || matches(request, "/api/auth/validate")
                || matches(request, "/api/s-schools/selectList")
                || matches(request, "/api/s-grades/listBySchool")
                || matches(request, "/api/s-grades/listAll")
                || matches(request, "/api/s-sizes/all")
                || matches(request, "/api/s-uniform/active")
                || matches(request, "/api/s-style-guides/active")
                || matches(request, "/api/s-style-guides/*")
                || matches(request, "/api/s-reviews/uniform/**")
                || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !StringUtils.hasText(uri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith("Bearer ")) {
            writeUnauthorized(response, "Token 缺失或格式错误");
            return;
        }

        try {
            String jwtToken = authorization.substring(7);
            var claims = jwtUtils.validateToken(jwtToken);
            UserPrincipal principal = jwtUtils.toPrincipal(claims);
            if (principal.getUserId() == null) {
                UserAccount userAccount = userAccountService.getUserByAccount(principal.getUserAccount());
                if (userAccount == null) {
                    throw new UnauthenticatedException("Token 对应用户不存在");
                }
                principal.setUserId(userAccount.getUserId());
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    jwtToken,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            authentication.setDetails(request.getRemoteAddr());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authenticated user {}", principal.getUserAccount());
        } catch (UnauthenticatedException e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
            writeUnauthorized(response, e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean matches(HttpServletRequest request, String pattern) {
        return new AntPathRequestMatcher(pattern).matches(request);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(HttpServletResponse.SC_UNAUTHORIZED, message));
    }
}
