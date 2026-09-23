package dev.springawsportfolio.portfolio.platform.web.error;

import dev.springawsportfolio.portfolio.platform.web.error.security.AccessDeniedApiException;
import dev.springawsportfolio.portfolio.platform.web.error.security.AuthenticationRequiredException;
import dev.springawsportfolio.portfolio.platform.web.error.security.CsrfValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class GlobalWebExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(
            MethodArgumentNotValidException exception
    ) {
        List<ApiFieldError> fieldErrors =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error -> new ApiFieldError(
                                error.getField(),
                                error.getCode(),
                                error.getDefaultMessage()
                        ))
                        .toList();

        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problem.setType(
                URI.create("/problems/validation-failed")
        );
        problem.setTitle("Request validation failed");
        problem.setDetail(
                "One or more request fields are invalid."
        );
        problem.setProperty(
                "code",
                "VALIDATION_FAILED"
        );
        problem.setProperty(
                "fieldErrors",
                fieldErrors
        );

        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleMalformedJson() {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problem.setType(
                URI.create("/problems/malformed-json")
        );
        problem.setTitle("Malformed JSON");
        problem.setProperty(
                "code",
                "MALFORMED_JSON"
        );

        return problem;
    }

    @ExceptionHandler(AuthenticationRequiredException.class)
    ProblemDetail handleAuthenticationRequired() {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);

        problem.setType(
                URI.create("/problems/authentication-required")
        );
        problem.setTitle("Authentication required");
        problem.setDetail(
                "Authentication is required to access this resource."
        );
        problem.setProperty(
                "code",
                "AUTHENTICATION_REQUIRED"
        );

        return problem;
    }

    @ExceptionHandler(CsrfValidationException.class)
    ProblemDetail handleCsrfValidation() {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.FORBIDDEN);

        problem.setType(
                URI.create("/problems/csrf-validation-failed")
        );
        problem.setTitle("CSRF validation failed");
        problem.setDetail(
                "The CSRF token is missing or invalid."
        );
        problem.setProperty(
                "code",
                "CSRF_VALIDATION_FAILED"
        );

        return problem;
    }

    @ExceptionHandler(AccessDeniedApiException.class)
    ProblemDetail handleAccessDenied() {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.FORBIDDEN);

        problem.setType(
                URI.create("/problems/access-denied")
        );
        problem.setTitle("Access denied");
        problem.setDetail(
                "You do not have permission to access this resource."
        );
        problem.setProperty(
                "code",
                "ACCESS_DENIED"
        );

        return problem;
    }
}
