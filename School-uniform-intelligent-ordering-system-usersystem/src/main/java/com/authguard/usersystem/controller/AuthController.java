package com.authguard.usersystem.controller;

import com.authguard.usersystem.dto.ApiResponse;
import com.authguard.usersystem.util.JwtUtils;
import com.authguard.usersystem.exception.UnauthenticatedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtils jwtUtils;

    public AuthController(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Void>> validateToken(@RequestHeader(value = "Authorization", required = false) String token) {
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            throw new UnauthenticatedException("Token 缺失或格式错误");
        }

        String jwtToken = token.substring(7);
        jwtUtils.validateToken(jwtToken);
        return ResponseEntity.ok(ApiResponse.success("Token 有效", null));
    }
}
