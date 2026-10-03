package dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication;

import org.springframework.security.core.AuthenticatedPrincipal;

import java.io.Serializable;
import java.util.UUID;

public record AuthenticatedUserPrincipal(
        UUID userId,
        String email
) implements Serializable, AuthenticatedPrincipal {

    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return userId.toString();
    }
}
