package com.project.common.util;

import org.springframework.security.core.context.SecurityContextHolder;

import com.project.common.exception.UnauthorizedException;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
            throw new UnauthorizedException("Authentication required");
        return auth.getName();
    }

    public static boolean hasRole(String role) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && auth.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
