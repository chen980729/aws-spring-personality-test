package dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication;

import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.application.command.authenticate.AuthenticateUserCommand;
import dev.springawsportfolio.portfolio.identity.application.command.authenticate.AuthenticateUserResult;
import dev.springawsportfolio.portfolio.identity.application.command.authenticate.AuthenticateUserService;
import dev.springawsportfolio.portfolio.identity.application.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationAuthenticationProviderTest {

    @Mock
    private AuthenticateUserService authenticateUserService;

    private ApplicationAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ApplicationAuthenticationProvider(
                authenticateUserService
        );
    }

    @Test
    void returnsAuthenticatedPrincipalWhenCredentialsAreValid() {
        UserId userId =
                new UserId(UUID.randomUUID());

        when(authenticateUserService.authenticate(
                new AuthenticateUserCommand(
                        "user@example.com",
                        "correct-password"
                )
        )).thenReturn(
                new AuthenticateUserResult(
                        userId,
                        "user@example.com"
                )
        );

        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken
                        .unauthenticated(
                                "user@example.com",
                                "correct-password"
                        );

        Authentication result =
                provider.authenticate(
                        authenticationRequest
                );

        assertTrue(result.isAuthenticated());

        assertInstanceOf(
                AuthenticatedUserPrincipal.class,
                result.getPrincipal()
        );

        AuthenticatedUserPrincipal principal =
                (AuthenticatedUserPrincipal)
                        result.getPrincipal();

        assertEquals(
                userId.value(),
                principal.userId()
        );

        assertEquals(
                "user@example.com",
                principal.email()
        );

        assertEquals(
                userId.value().toString(),
                result.getName()
        );

        assertNull(result.getCredentials());
    }

    @Test
    void convertsInvalidCredentialsToBadCredentialsException() {
        when(authenticateUserService.authenticate(
                any(AuthenticateUserCommand.class)
        )).thenThrow(
                new InvalidCredentialsException()
        );

        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken
                        .unauthenticated(
                                "user@example.com",
                                "wrong-password"
                        );

        assertThrows(
                BadCredentialsException.class,
                () -> provider.authenticate(
                        authenticationRequest
                )
        );
    }

    @Test
    void supportsUsernamePasswordAuthenticationToken() {
        assertTrue(
                provider.supports(
                        UsernamePasswordAuthenticationToken.class
                )
        );
    }
}