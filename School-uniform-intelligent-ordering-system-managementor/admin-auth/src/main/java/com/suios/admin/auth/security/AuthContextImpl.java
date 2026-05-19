package com.suios.admin.auth.security;

import com.suios.admin.common.security.AuthContext;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthContextImpl implements AuthContext {

    @Override
    public String getCurrentUsername() {
        return getCurrentPrincipal()
                .map(AdminUserPrincipal::username)
                .orElse("system");
    }

    @Override
    public Long getCurrentUserId() {
        return getCurrentPrincipal()
                .map(AdminUserPrincipal::userId)
                .orElse(null);
    }

    private Optional<AdminUserPrincipal> getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AdminUserPrincipal adminUserPrincipal) {
            return Optional.of(adminUserPrincipal);
        }
        return Optional.empty();
    }
}
