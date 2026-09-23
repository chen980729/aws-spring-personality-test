package dev.springawsportfolio.portfolio.platform.web.error.security;

public class AccessDeniedApiException extends RuntimeException {

    public AccessDeniedApiException() {
        super("Access is denied");
    }
}
