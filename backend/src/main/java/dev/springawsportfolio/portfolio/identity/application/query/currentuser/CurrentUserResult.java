package dev.springawsportfolio.portfolio.identity.application.query.currentuser;

import dev.springawsportfolio.portfolio.identity.api.UserId;

public record CurrentUserResult(
        UserId id,
        String email,
        String displayName
) {
}
