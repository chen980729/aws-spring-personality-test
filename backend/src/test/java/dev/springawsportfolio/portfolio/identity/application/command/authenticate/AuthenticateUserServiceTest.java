package dev.springawsportfolio.portfolio.identity.application.command.authenticate;

import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.application.exception.InvalidCredentialsException;
import dev.springawsportfolio.portfolio.identity.application.port.out.PasswordHasher;
import dev.springawsportfolio.portfolio.identity.application.support.EmailCanonicalizer;
import dev.springawsportfolio.portfolio.identity.domain.account.UserAccount;
import dev.springawsportfolio.portfolio.identity.domain.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserServiceTest {

    @Mock
    private UserAccountRepository repository;

    @Mock
    private PasswordHasher passwordHasher;

    private AuthenticateUserService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticateUserService(
                repository,
                passwordHasher,
                new EmailCanonicalizer()
        );
    }

    @Test
    void authenticatesWithCanonicalEmailAndCorrectPassword() {
        UserId userId = UserId.newId();

        UserAccount account = UserAccount.create(
                userId,
                "user@example.com",
                "Chen",
                "stored-password-hash",
                Instant.parse("2026-09-22T00:00:00Z")
        );

        when(repository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(account));

        when(passwordHasher.matches(
                "correct-password",
                "stored-password-hash"
        )).thenReturn(true);

        AuthenticateUserResult result =
                service.authenticate(
                        new AuthenticateUserCommand(
                                " USER@Example.COM ",
                                "correct-password"
                        )
                );

        assertEquals(userId, result.id());
        assertEquals("user@example.com", result.email());

        verify(repository)
                .findByEmail("user@example.com");

        verify(passwordHasher)
                .matches(
                        "correct-password",
                        "stored-password-hash"
                );
    }

    @Test
    void rejectsUnknownEmailAsInvalidCredentials() {
        when(repository.findByEmail(
                "unknown@example.com"
        )).thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.authenticate(
                        new AuthenticateUserCommand(
                                "unknown@example.com",
                                "some-password"
                        )
                )
        );

        verifyNoInteractions(passwordHasher);
    }

    @Test
    void rejectsWrongPasswordAsInvalidCredentials() {
        UserAccount account = UserAccount.create(
                UserId.newId(),
                "user@example.com",
                "Chen",
                "stored-password-hash",
                Instant.parse("2026-09-22T00:00:00Z")
        );

        when(repository.findByEmail(
                "user@example.com"
        )).thenReturn(Optional.of(account));

        when(passwordHasher.matches(
                "wrong-password",
                "stored-password-hash"
        )).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.authenticate(
                        new AuthenticateUserCommand(
                                "user@example.com",
                                "wrong-password"
                        )
                )
        );
    }
}
