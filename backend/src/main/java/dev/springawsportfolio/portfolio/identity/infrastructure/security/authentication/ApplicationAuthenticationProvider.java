package dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication;

import dev.springawsportfolio.portfolio.identity.application.command.authenticate.AuthenticateUserCommand;
import dev.springawsportfolio.portfolio.identity.application.command.authenticate.AuthenticateUserResult;
import dev.springawsportfolio.portfolio.identity.application.command.authenticate.AuthenticateUserService;
import dev.springawsportfolio.portfolio.identity.application.exception.InvalidCredentialsException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

@Component
public class ApplicationAuthenticationProvider
        implements AuthenticationProvider {

    private final AuthenticateUserService authenticateUserService;

    public ApplicationAuthenticationProvider(
            AuthenticateUserService authenticateUserService
    ) {
        this.authenticateUserService = authenticateUserService;
    }

    @Override
    public Authentication authenticate(
            Authentication authentication
    ) throws AuthenticationException {

        Object credentials = authentication.getCredentials();

        if (!(credentials instanceof String password)) {
            throw new BadCredentialsException(
                    "Invalid credentials"
            );
        }

        AuthenticateUserResult result;

        try {
            result = authenticateUserService.authenticate(
                    new AuthenticateUserCommand(
                            authentication.getName(),
                            password
                    )
            );
        } catch (InvalidCredentialsException exception) {
            throw new BadCredentialsException(
                    "Invalid credentials"
            );
        }

        AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(
                        result.id().value(),
                        result.email()
                );

        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                AuthorityUtils.NO_AUTHORITIES
        );
    }

    @Override
    public boolean supports(
            Class<?> authentication
    ) {
        return UsernamePasswordAuthenticationToken.class
                .isAssignableFrom(authentication);
    }
}
