package dev.springawsportfolio.portfolio.identity.application.command.authenticate;

import dev.springawsportfolio.portfolio.identity.application.exception.InvalidCredentialsException;
import dev.springawsportfolio.portfolio.identity.application.port.out.PasswordHasher;
import dev.springawsportfolio.portfolio.identity.application.support.EmailCanonicalizer;
import dev.springawsportfolio.portfolio.identity.domain.account.UserAccount;
import dev.springawsportfolio.portfolio.identity.domain.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticateUserService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordHasher passwordHasher;
    private final EmailCanonicalizer emailCanonicalizer;

    public AuthenticateUserService(
            UserAccountRepository userAccountRepository,
            PasswordHasher passwordHasher,
            EmailCanonicalizer emailCanonicalizer
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordHasher = passwordHasher;
        this.emailCanonicalizer = emailCanonicalizer;
    }

    @Transactional(readOnly = true)
    public AuthenticateUserResult authenticate(
            AuthenticateUserCommand command
    ) {
        String canonicalEmail =
                emailCanonicalizer.canonicalize(command.email());

        UserAccount account = userAccountRepository
                .findByEmail(canonicalEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(
                command.password(),
                account.passwordHash()
        )) {
            throw new InvalidCredentialsException();
        }

        return new AuthenticateUserResult(
                account.id(),
                account.email()
        );
    }
}
