package dev.springawsportfolio.portfolio.identity.application.command.register;

import dev.springawsportfolio.portfolio.identity.application.exception.EmailAlreadyRegisteredException;
import dev.springawsportfolio.portfolio.identity.application.exception.InvalidPasswordException;
import dev.springawsportfolio.portfolio.identity.application.port.out.PasswordHasher;
import dev.springawsportfolio.portfolio.identity.application.support.EmailCanonicalizer;
import dev.springawsportfolio.portfolio.identity.domain.account.UserAccount;
import dev.springawsportfolio.portfolio.identity.domain.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegisterUserServiceTest {

    private UserAccountRepository repository;
    private PasswordHasher passwordHasher;
    private RegisterUserService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserAccountRepository.class);
        passwordHasher = mock(PasswordHasher.class);

        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-09-22T10:00:00Z"),
                ZoneOffset.UTC
        );

        service = new RegisterUserService(
                repository,
                passwordHasher,
                new EmailCanonicalizer(),
                fixedClock
        );
    }

    @Test
    void registersNewUser() {
        when(repository.existsByEmail(
                "user@example.com"
        )).thenReturn(false);

        when(passwordHasher.hash(
                "a-secure-password"
        )).thenReturn("hashed-password");

        when(repository.save(any(UserAccount.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        RegisterUserResult result = service.register(
                new RegisterUserCommand(
                        "  USER@Example.COM ",
                        "a-secure-password",
                        "Chen"
                )
        );

        assertEquals(
                "user@example.com",
                result.email()
        );

        assertEquals(
                "Chen",
                result.displayName()
        );

        ArgumentCaptor<UserAccount> captor =
                ArgumentCaptor.forClass(
                        UserAccount.class
                );

        verify(repository).save(captor.capture());

        UserAccount savedAccount =
                captor.getValue();

        assertEquals(
                "user@example.com",
                savedAccount.email()
        );

        assertEquals(
                "hashed-password",
                savedAccount.passwordHash()
        );
    }

    @Test
    void rejectsDuplicateCanonicalEmail() {
        when(repository.existsByEmail(
                "user@example.com"
        )).thenReturn(true);

        assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> service.register(
                        new RegisterUserCommand(
                                " USER@example.com ",
                                "a-secure-password",
                                "Chen"
                        )
                )
        );

        verifyNoInteractions(passwordHasher);

        verify(
                repository,
                never()
        ).save(any());
    }

    @Test
    void rejectsPasswordShorterThanMinimum() {
        assertThrows(
                InvalidPasswordException.class,
                () -> service.register(
                        new RegisterUserCommand(
                                "user@example.com",
                                "too-short",
                                "Chen"
                        )
                )
        );

        verifyNoInteractions(passwordHasher);
    }
}