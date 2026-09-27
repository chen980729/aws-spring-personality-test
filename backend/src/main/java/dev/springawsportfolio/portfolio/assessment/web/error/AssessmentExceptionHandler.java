package dev.springawsportfolio.portfolio.assessment.web.error;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentAlreadySubmittedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionAlreadyAbandonedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.InvalidQuestionnaireResponseException;
import dev.springawsportfolio.portfolio.assessment.application.exception.QuestionnaireIncompleteException;
import org.springframework.dao.OptimisticLockingFailureException;
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

    @ExceptionHandler(
            AssessmentAlreadySubmittedException.class
    )
    ProblemDetail handleAssessmentAlreadySubmitted() {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.CONFLICT
                );

        problem.setType(
                URI.create(
                        "/problems/assessment-already-submitted"
                )
        );

        problem.setTitle(
                "Assessment already submitted"
        );

        problem.setDetail(
                "The questionnaire has already been submitted "
                        + "and can no longer be changed."
        );

        problem.setProperty(
                "code",
                "ASSESSMENT_ALREADY_SUBMITTED"
        );

        return problem;
    }

    @ExceptionHandler(
            AssessmentSessionAlreadyAbandonedException.class
    )
    ProblemDetail handleAssessmentSessionAlreadyAbandoned() {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.CONFLICT
                );

        problem.setType(
                URI.create(
                        "/problems/assessment-session-already-abandoned"
                )
        );

        problem.setTitle(
                "Assessment session already abandoned"
        );

        problem.setDetail(
                "The assessment session has already been abandoned "
                        + "and can no longer be changed."
        );

        problem.setProperty(
                "code",
                "ASSESSMENT_SESSION_ALREADY_ABANDONED"
        );

        return problem;
    }

    @ExceptionHandler(
            QuestionnaireIncompleteException.class
    )
    ProblemDetail handleQuestionnaireIncomplete() {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.UNPROCESSABLE_CONTENT
                );

        problem.setType(
                URI.create(
                        "/problems/questionnaire-incomplete"
                )
        );

        problem.setTitle(
                "Questionnaire incomplete"
        );

        problem.setDetail(
                "All required questionnaire questions "
                        + "must be answered before submission."
        );

        problem.setProperty(
                "code",
                "QUESTIONNAIRE_INCOMPLETE"
        );

        return problem;
    }

    @ExceptionHandler(
            InvalidQuestionnaireResponseException.class
    )
    ProblemDetail handleInvalidQuestionnaireResponse() {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.UNPROCESSABLE_CONTENT
                );

        problem.setType(
                URI.create(
                        "/problems/invalid-questionnaire-response"
                )
        );

        problem.setTitle(
                "Invalid questionnaire response"
        );

        problem.setDetail(
                "The questionnaire response is not valid "
                        + "for this assessment version."
        );

        problem.setProperty(
                "code",
                "INVALID_QUESTIONNAIRE_RESPONSE"
        );

        return problem;
    }

    @ExceptionHandler(
            OptimisticLockingFailureException.class
    )
    ProblemDetail handleOptimisticLockingFailure() {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.CONFLICT
                );

        problem.setType(
                URI.create(
                        "/problems/assessment-session-concurrent-modification"
                )
        );

        problem.setTitle(
                "Assessment session was concurrently modified"
        );

        problem.setDetail(
                "The assessment session changed while this request "
                        + "was being processed. Reload the current "
                        + "session state before retrying."
        );

        problem.setProperty(
                "code",
                "ASSESSMENT_SESSION_CONCURRENT_MODIFICATION"
        );

        return problem;
    }
}