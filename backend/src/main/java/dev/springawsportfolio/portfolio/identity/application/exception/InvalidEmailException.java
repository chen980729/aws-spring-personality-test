package dev.springawsportfolio.portfolio.identity.application.exception;

public class InvalidEmailException extends RuntimeException {

    public InvalidEmailException() {
        super("Email format is invalid");
    }
}
