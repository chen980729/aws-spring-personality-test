package dev.springawsportfolio.portfolio.assessment.domain.session;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDecisionSource;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDimensionConclusion;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.QuestionnaireResponse;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssessmentSessionClarificationLifecycleTest {

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-09-28T00:00:00Z"
            );

    private static final Instant SUBMITTED_AT =
            CREATED_AT.plusSeconds(30);

    private static final Instant COMPLETED_AT =
            SUBMITTED_AT.plusSeconds(60);

    private static final DimensionCode DIMENSION =
            new DimensionCode("EI");

    private static final PoleCode POLE_I =
            new PoleCode("I");

    @Test
    void beginsAndReturnsFromClarificationPhase() {
        AssessmentSession session =
                awaitingClarificationSession();

        session.beginClarification();

        assertEquals(
                AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS,
                session.status()
        );

        session.returnToAwaitingClarification();

        assertEquals(
                AssessmentSessionStatus.AWAITING_CLARIFICATION,
                session.status()
        );
    }

    @Test
    void completesAfterClarificationFromAwaitingState() {
        AssessmentSession session =
                awaitingClarificationSession();

        FinalAssessmentResult finalResult =
                finalResult();

        session.completeAfterClarification(
                finalResult,
                COMPLETED_AT
        );

        assertEquals(
                AssessmentSessionStatus.COMPLETED,
                session.status()
        );

        assertEquals(
                finalResult,
                session.finalResult()
        );

        assertEquals(
                COMPLETED_AT,
                session.completedAt()
        );
    }

    @Test
    void completesAfterClarificationFromInProgressState() {
        AssessmentSession session =
                awaitingClarificationSession();

        session.beginClarification();

        session.completeAfterClarification(
                finalResult(),
                COMPLETED_AT
        );

        assertEquals(
                AssessmentSessionStatus.COMPLETED,
                session.status()
        );
    }

    @Test
    void clarificationHooksRejectInvalidSessionStates() {
        AssessmentSession inProgress =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        new UserId(
                                UUID.randomUUID()
                        ),
                        AssessmentDefinitionId.newId(),
                        AssessmentDefinitionVersionId.newId(),
                        CREATED_AT
                );

        assertThrows(
                IllegalStateException.class,
                inProgress::beginClarification
        );

        AssessmentSession awaiting =
                awaitingClarificationSession();

        assertThrows(
                IllegalStateException.class,
                awaiting::returnToAwaitingClarification
        );
    }

    @Test
    void clarificationCompletionCannotPrecedeSubmission() {
        AssessmentSession session =
                awaitingClarificationSession();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        session.completeAfterClarification(
                                finalResult(),
                                SUBMITTED_AT.minusSeconds(1)
                        )
        );
    }

    private AssessmentSession awaitingClarificationSession() {
        AssessmentSession session =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        new UserId(
                                UUID.randomUUID()
                        ),
                        AssessmentDefinitionId.newId(),
                        AssessmentDefinitionVersionId.newId(),
                        CREATED_AT
                );

        session.submit(
                QuestionnaireResponse.empty(),
                new InitialAssessmentResult(
                        List.of(
                                new InitialDimensionResult(
                                        DIMENSION,
                                        0,
                                        null,
                                        true
                                )
                        )
                ),
                null,
                SUBMITTED_AT
        );

        return session;
    }

    private FinalAssessmentResult finalResult() {
        return new FinalAssessmentResult(
                "I",
                List.of(
                        new FinalDimensionConclusion(
                                DIMENSION,
                                POLE_I,
                                FinalDecisionSource.USER_TIE_BREAK
                        )
                )
        );
    }
}
