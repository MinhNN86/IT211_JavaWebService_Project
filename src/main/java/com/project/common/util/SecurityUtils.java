package com.project.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.project.common.exception.UnauthorizedException;

public final class SecurityUtils {
    private static final String ANONYMOUS_USER = "anonymousUser";
    private static final String ROLE_PREFIX = "ROLE_";

    private SecurityUtils() {
    }

    public static String currentUsername() {
        Authentication authentication = getAuthentication();

        if (!isAuthenticated(authentication) || ANONYMOUS_USER.equals(authentication.getName())) {
            throw new UnauthorizedException("Authentication required");
        }

        return authentication.getName();
    }

    public static boolean hasRole(String role) {
        Authentication authentication = getAuthentication();
        String requiredRole = ROLE_PREFIX + role;

        return isAuthenticated(authentication)
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> requiredRole.equals(authority.getAuthority()));
    }

    private static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }
}
