package dev.springawsportfolio.portfolio.assessment.web.error;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentAlreadySubmittedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionAlreadyAbandonedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionAlreadyCompletedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.ClarificationNotAllowedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.InvalidDimensionTieBreakException;
import dev.springawsportfolio.portfolio.assessment.application.exception.TieBreakAlreadyDecidedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.TieBreakInteractionUnavailableException;
import dev.springawsportfolio.portfolio.assessment.application.exception.TieBreakNotRequiredException;
import dev.springawsportfolio.portfolio.assessment.application.exception.InvalidAssessmentHistoryPageException;
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
                        + "and cannot be restarted again."
        );

        problem.setProperty(
                "code",
                "ASSESSMENT_SESSION_ALREADY_ABANDONED"
        );

        return problem;
    }

    @ExceptionHandler(
            AssessmentSessionAlreadyCompletedException.class
    )
    ProblemDetail handleAssessmentSessionAlreadyCompleted() {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.CONFLICT
                );

        problem.setType(
                URI.create(
                        "/problems/assessment-session-already-completed"
                )
        );

        problem.setTitle(
                "Assessment session already completed"
        );

        problem.setDetail(
                "The completed assessment session is immutable "
                        + "and cannot be restarted."
        );

        problem.setProperty(
                "code",
                "ASSESSMENT_SESSION_ALREADY_COMPLETED"
        );

        return problem;
    }

    @ExceptionHandler(
            InvalidAssessmentHistoryPageException.class
    )
    ProblemDetail handleInvalidAssessmentHistoryPage() {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.BAD_REQUEST
                );

        problem.setType(
                URI.create(
                        "/problems/assessment-history-pagination-invalid"
                )
        );

        problem.setTitle(
                "Assessment history pagination is invalid"
        );

        problem.setDetail(
                "History page must be at least 1 and size must be "
                        + "between 1 and 100."
        );

        problem.setProperty(
                "code",
                "VALIDATION_FAILED"
        );

        return problem;
    }

    @ExceptionHandler(
            InvalidAssessmentHistoryStatusException.class
    )
    ProblemDetail handleInvalidAssessmentHistoryStatus() {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.BAD_REQUEST
                );

        problem.setType(
                URI.create(
                        "/problems/assessment-history-status-invalid"
                )
        );

        problem.setTitle(
                "Assessment history status is invalid"
        );

        problem.setDetail(
                "Assessment history currently supports only "
                        + "status=COMPLETED."
        );

        problem.setProperty(
                "code",
                "VALIDATION_FAILED"
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
            ClarificationNotAllowedException.class
    )
    ProblemDetail handleClarificationNotAllowed() {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.UNPROCESSABLE_CONTENT
                );

        problem.setType(
                URI.create(
                        "/problems/clarification-not-allowed"
                )
        );

        problem.setTitle(
                "Clarification action is not allowed"
        );

        problem.setDetail(
                "The requested clarification action is not valid "
                        + "for the current assessment state or dimension."
        );

        problem.setProperty(
                "code",
                "CLARIFICATION_NOT_ALLOWED"
        );

        return problem;
    }

    @ExceptionHandler(
            TieBreakNotRequiredException.class
    )
    ProblemDetail handleTieBreakNotRequired() {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.UNPROCESSABLE_CONTENT
                );

        problem.setType(
                URI.create(
                        "/problems/tie-break-not-required"
                )
        );

        problem.setTitle(
                "Tie-break is not required"
        );

        problem.setDetail(
                "A tie-break is not semantically valid for this "
                        + "assessment dimension in its current state."
        );

        problem.setProperty(
                "code",
                "TIE_BREAK_NOT_REQUIRED"
        );

        return problem;
    }

    @ExceptionHandler(
            TieBreakAlreadyDecidedException.class
    )
    ProblemDetail handleTieBreakAlreadyDecided() {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.CONFLICT
                );

        problem.setType(
                URI.create(
                        "/problems/tie-break-already-decided"
                )
        );

        problem.setTitle(
                "Tie-break already decided"
        );

        problem.setDetail(
                "The accepted tie-break decision is immutable and "
                        + "cannot be replaced with a different selection."
        );

        problem.setProperty(
                "code",
                "TIE_BREAK_ALREADY_DECIDED"
        );

        return problem;
    }

    @ExceptionHandler(
            TieBreakInteractionUnavailableException.class
    )
    ProblemDetail handleTieBreakInteractionUnavailable() {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.CONFLICT
                );

        problem.setType(
                URI.create(
                        "/problems/tie-break-interaction-unavailable"
                )
        );

        problem.setTitle(
                "Tie-break interaction unavailable"
        );

        problem.setDetail(
                "The assessment lifecycle no longer permits reading "
                        + "a pending tie-break interaction."
        );

        problem.setProperty(
                "code",
                "TIE_BREAK_INTERACTION_UNAVAILABLE"
        );

        return problem;
    }

    @ExceptionHandler(
            InvalidDimensionTieBreakException.class
    )
    ProblemDetail handleInvalidDimensionTieBreak() {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.UNPROCESSABLE_CONTENT
                );

        problem.setType(
                URI.create(
                        "/problems/invalid-dimension-tie-break"
                )
        );

        problem.setTitle(
                "Invalid dimension tie-break"
        );

        problem.setDetail(
                "The submitted tie-break selection is not valid for "
                        + "the target dimension and bound assessment version."
        );

        problem.setProperty(
                "code",
                "INVALID_DIMENSION_TIE_BREAK"
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