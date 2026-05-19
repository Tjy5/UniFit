package com.suios.admin.auth.jwt;

import com.suios.admin.auth.entity.AdminUser;
import com.suios.admin.auth.security.AdminUserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    public String createToken(AdminUser adminUser) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(adminUser.getUsername())
                .claim("userId", adminUser.getUserId())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public AdminUserPrincipal getPrincipal(String token) {
        Claims claims = parseClaims(token);
        Number userIdValue = claims.get("userId", Number.class);
        Long userId = userIdValue == null ? null : userIdValue.longValue();
        return new AdminUserPrincipal(userId, claims.getSubject());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
