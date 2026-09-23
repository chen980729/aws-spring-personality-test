package dev.springawsportfolio.portfolio.platform.web.error.security;

public class CsrfValidationException extends RuntimeException {

    public CsrfValidationException() {
        super("CSRF validation failed");
    }
}