package com.authguard.usersystem.security;

import com.authguard.usersystem.exception.UnauthenticatedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CurrentUserResolver {

    public UserPrincipal requireUser(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            if (principal.getUserId() != null && StringUtils.hasText(principal.getUserAccount())) {
                return principal;
            }
        }

        throw new UnauthenticatedException("未登录或Token无效");
    }

    public Long requireUserId(HttpServletRequest request) {
        return requireUser(request).getUserId();
    }
}
