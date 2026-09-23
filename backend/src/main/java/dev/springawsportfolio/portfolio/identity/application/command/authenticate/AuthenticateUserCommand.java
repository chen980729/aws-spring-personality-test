package dev.springawsportfolio.portfolio.identity.application.command.authenticate;

public record AuthenticateUserCommand(
        String email,
        String password
) {
}
