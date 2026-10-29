package com.upc.pre.peaceapp.shared.interfaces.rest;

import com.upc.pre.peaceapp.security.iam.domain.model.valueobjects.Roles;
import org.springframework.security.core.Authentication;

public final class AuthenticatedRequester {
    private AuthenticatedRequester() {
    }

    public static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Roles.ROLE_ADMIN.name()));
    }
}
