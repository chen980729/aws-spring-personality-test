package dev.springawsportfolio.portfolio.assessment.web.error;

import dev.springawsportfolio.portfolio.assessment.application.exception.ClarificationTurnConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssessmentExceptionHandlerTest {

    @Test
    void mapsClarificationTurnConflictTo409() {
        AssessmentExceptionHandler handler =
                new AssessmentExceptionHandler();

        ProblemDetail problem =
                handler.handleClarificationTurnConflict(
                        new ClarificationTurnConflictException(
                                "synthetic turn conflict"
                        )
                );

        assertEquals(
                HttpStatus.CONFLICT.value(),
                problem.getStatus()
        );

        assertEquals(
                "CLARIFICATION_TURN_CONFLICT",
                problem.getProperties()
                        .get(
                                "code"
                        )
        );
    }
}
