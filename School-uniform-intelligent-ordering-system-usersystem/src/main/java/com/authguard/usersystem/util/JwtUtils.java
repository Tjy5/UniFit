package com.authguard.usersystem.util;

import com.authguard.usersystem.config.JwtProperties;
import com.authguard.usersystem.exception.UnauthenticatedException;
import com.authguard.usersystem.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtUtils {
    private final JwtProperties jwtProperties;

    public String generateToken(String userAccount) {
        return generateToken(null, userAccount);
    }

    public String generateToken(Long userId, String userAccount) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + jwtProperties.getExpiration());
        return Jwts.builder()
                .setSubject(userAccount)
                .claim("userId", userId)
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new UnauthenticatedException("Token 已过期");
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthenticatedException("Token 无效");
        }
    }

    public String parseToken(String token) {
        return validateToken(token).getSubject();
    }

    public UserPrincipal toPrincipal(Claims claims) {
        Number userId = claims.get("userId", Number.class);
        return new UserPrincipal(userId == null ? null : userId.longValue(), claims.getSubject());
    }

    private SecretKey signingKey() {
        if (!StringUtils.hasText(jwtProperties.getSecret())) {
            throw new IllegalStateException("JWT secret is not configured");
        }
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
