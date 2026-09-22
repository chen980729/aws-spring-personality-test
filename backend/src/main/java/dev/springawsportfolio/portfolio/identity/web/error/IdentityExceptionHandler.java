package dev.springawsportfolio.portfolio.identity.web.error;

import dev.springawsportfolio.portfolio.identity.application.exception.EmailAlreadyRegisteredException;
import dev.springawsportfolio.portfolio.identity.application.exception.InvalidEmailException;
import dev.springawsportfolio.portfolio.identity.application.exception.InvalidPasswordException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(
        basePackages = "dev.springawsportfolio.portfolio.identity.web"
)
public class IdentityExceptionHandler {

    @ExceptionHandler(
            EmailAlreadyRegisteredException.class
    )
    ProblemDetail handleEmailAlreadyRegistered() {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.CONFLICT);

        problem.setType(
                URI.create(
                        "/problems/email-already-registered"
                )
        );
        problem.setTitle(
                "Email already registered"
        );
        problem.setProperty(
                "code",
                "EMAIL_ALREADY_REGISTERED"
        );

        return problem;
    }

    @ExceptionHandler(InvalidEmailException.class)
    ProblemDetail handleInvalidEmail() {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problem.setType(
                URI.create("/problems/invalid-email")
        );
        problem.setTitle("Invalid email");
        problem.setProperty(
                "code",
                "INVALID_EMAIL"
        );

        return problem;
    }

    @ExceptionHandler(InvalidPasswordException.class)
    ProblemDetail handleInvalidPassword() {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problem.setType(
                URI.create("/problems/invalid-password")
        );
        problem.setTitle("Invalid password");
        problem.setProperty(
                "code",
                "INVALID_PASSWORD"
        );

        return problem;
    }
}
