package dev.springawsportfolio.portfolio.identity.web.auth.dto;

public record CsrfTokenResponse(
        String token,
        String headerName,
        String parameterName
) {
}