package com.thonha.vuatho.security;

import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.thonha.vuatho.exception.UnauthorizedException;

@Component
public class CurrentUserProvider {
    public Long requireUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.isAuthenticated() && a.getPrincipal() instanceof Long id) return id;
        throw new UnauthorizedException("Please sign in to continue");
    }
}
