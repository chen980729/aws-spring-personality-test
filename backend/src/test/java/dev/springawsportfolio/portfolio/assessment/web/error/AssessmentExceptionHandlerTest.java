package dev.springawsportfolio.portfolio.assessment.web.error;

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
                handler.handleClarificationTurnConflict();

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
