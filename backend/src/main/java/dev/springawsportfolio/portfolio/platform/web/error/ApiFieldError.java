package dev.springawsportfolio.portfolio.platform.web.error;

public record ApiFieldError(
        String field,
        String code,
        String message
) {
}
