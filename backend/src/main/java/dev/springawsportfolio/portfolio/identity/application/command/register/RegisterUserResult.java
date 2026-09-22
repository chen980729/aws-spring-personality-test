package dev.springawsportfolio.portfolio.identity.application.command.register;

import dev.springawsportfolio.portfolio.identity.api.UserId;

import java.time.Instant;

public record RegisterUserResult(
        UserId id,
        String email,
        String displayName,
        Instant createdAt
) {
}