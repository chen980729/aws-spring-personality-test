package dev.springawsportfolio.portfolio.identity.application.command.register;

public record RegisterUserCommand(
        String email,
        String password,
        String displayName
) {
}
