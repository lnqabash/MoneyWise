package app.moneymate.common.security;

import app.moneymate.common.ApiException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/** Convenience accessor for the authenticated user id set by JwtAuthFilter. */
public final class CurrentUser {

    private CurrentUser() {}

    public static UUID id() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID userId)) {
            throw ApiException.unauthenticated("No authenticated user");
        }
        return userId;
    }
}
