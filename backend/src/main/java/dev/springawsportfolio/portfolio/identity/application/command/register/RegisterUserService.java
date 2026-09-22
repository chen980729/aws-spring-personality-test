package dev.springawsportfolio.portfolio.identity.application.command.register;

import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.application.exception.EmailAlreadyRegisteredException;
import dev.springawsportfolio.portfolio.identity.application.exception.InvalidPasswordException;
import dev.springawsportfolio.portfolio.identity.application.port.out.PasswordHasher;
import dev.springawsportfolio.portfolio.identity.application.support.EmailCanonicalizer;
import dev.springawsportfolio.portfolio.identity.domain.account.UserAccount;
import dev.springawsportfolio.portfolio.identity.domain.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class RegisterUserService {

    private static final int MIN_PASSWORD_LENGTH = 15;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private final Clock clock;

    private final UserAccountRepository userAccountRepository;
    private final PasswordHasher passwordHasher;
    private final EmailCanonicalizer emailCanonicalizer;

    public RegisterUserService(
            UserAccountRepository userAccountRepository,
            PasswordHasher passwordHasher,
            EmailCanonicalizer emailCanonicalizer,
            Clock clock
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordHasher = passwordHasher;
        this.emailCanonicalizer = emailCanonicalizer;
        this.clock = clock;
    }

    @Transactional
    public RegisterUserResult register(RegisterUserCommand command) {

        String canonicalEmail =
                emailCanonicalizer.canonicalize(command.email());

        validatePassword(command.password());

        if (userAccountRepository.existsByEmail(canonicalEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        String passwordHash =
                passwordHasher.hash(command.password());

        UserAccount account = UserAccount.create(
                UserId.newId(),
                canonicalEmail,
                command.displayName(),
                passwordHash,
                clock.instant()
        );

        UserAccount saved =
                userAccountRepository.save(account);

        return new RegisterUserResult(
                saved.id(),
                saved.email(),
                saved.displayName(),
                saved.createdAt()
        );
    }

    private void validatePassword(String password) {
        if (password == null) {
            throw new InvalidPasswordException(
                    "Password must not be null"
            );
        }

        int length = password.codePointCount(
                0,
                password.length()
        );

        if (length < MIN_PASSWORD_LENGTH
                || length > MAX_PASSWORD_LENGTH) {
            throw new InvalidPasswordException(
                    "Password length must be between "
                            + MIN_PASSWORD_LENGTH
                            + " and "
                            + MAX_PASSWORD_LENGTH
            );
        }
    }
}