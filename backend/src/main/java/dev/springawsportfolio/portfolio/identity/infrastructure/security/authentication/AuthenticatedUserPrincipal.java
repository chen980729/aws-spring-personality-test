package dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication;

import java.io.Serializable;
import java.util.UUID;

public record AuthenticatedUserPrincipal(
        UUID userId,
        String email
) implements Serializable {

    private static final long serialVersionUID = 1L;
}
