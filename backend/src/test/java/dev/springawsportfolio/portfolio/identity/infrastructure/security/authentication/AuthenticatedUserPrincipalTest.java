package dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthenticatedUserPrincipalTest {

    @Test
    void exposesStableUserIdAsPrincipalName() {
        UUID userId = UUID.randomUUID();

        AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(
                        userId,
                        "a-very-long-email-address-that-must-not-be-used-as-the-session-principal-name@example.com"
                );

        assertEquals(
                userId.toString(),
                principal.getName()
        );
    }
}
