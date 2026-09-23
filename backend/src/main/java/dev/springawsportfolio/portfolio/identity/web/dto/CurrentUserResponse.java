package dev.springawsportfolio.portfolio.identity.web.dto;

import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String email,
        String displayName
) {
}
