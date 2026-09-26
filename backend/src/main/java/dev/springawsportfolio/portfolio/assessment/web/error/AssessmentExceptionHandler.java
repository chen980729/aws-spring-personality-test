package dev.springawsportfolio.portfolio.assessment.web.error;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(
        basePackages =
                "dev.springawsportfolio.portfolio.assessment.web"
)
public class AssessmentExceptionHandler {

    @ExceptionHandler(
            AssessmentNotFoundException.class
    )
    ProblemDetail handleAssessmentNotFound() {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.NOT_FOUND
                );

        problem.setType(
                URI.create(
                        "/problems/assessment-not-found"
                )
        );

        problem.setTitle(
                "Assessment not found"
        );

        problem.setDetail(
                "The requested assessment is not available."
        );

        problem.setProperty(
                "code",
                "ASSESSMENT_NOT_FOUND"
        );

        return problem;
    }

    @ExceptionHandler(
            AssessmentSessionNotFoundException.class
    )
    ProblemDetail handleAssessmentSessionNotFound() {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.NOT_FOUND
                );

        problem.setType(
                URI.create(
                        "/problems/assessment-session-not-found"
                )
        );

        problem.setTitle(
                "Assessment session not found"
        );

        problem.setDetail(
                "The requested assessment session "
                        + "is not available."
        );

        problem.setProperty(
                "code",
                "ASSESSMENT_SESSION_NOT_FOUND"
        );

        return problem;
    }
}