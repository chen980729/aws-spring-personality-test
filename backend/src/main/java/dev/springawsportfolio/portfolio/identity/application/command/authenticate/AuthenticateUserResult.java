package dev.springawsportfolio.portfolio.identity.application.command.authenticate;

import dev.springawsportfolio.portfolio.identity.api.UserId;

public record AuthenticateUserResult(
        UserId id,
        String email
) {
}
