package dev.springawsportfolio.portfolio.assessment.domain.session;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionId;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDecisionSource;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDimensionConclusion;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.Answer;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.QuestionnaireResponse;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentSessionTest {

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-09-25T00:00:00Z"
            );

    private static final Instant SUBMITTED_AT =
            CREATED_AT.plusSeconds(30);

    private static final DimensionCode DIMENSION =
            new DimensionCode("XY");

    private static final PoleCode POLE_X =
            new PoleCode("X");

    @Test
    void startsInProgressBoundToOwnerAndDefinitionVersion() {
        AssessmentSessionId sessionId =
                AssessmentSessionId.newId();

        UserId ownerUserId =
                new UserId(
                        UUID.randomUUID()
                );

        AssessmentDefinitionId definitionId =
                AssessmentDefinitionId.newId();

        AssessmentDefinitionVersionId versionId =
                AssessmentDefinitionVersionId.newId();

        AssessmentSession session =
                AssessmentSession.start(
                        sessionId,
                        ownerUserId,
                        definitionId,
                        versionId,
                        CREATED_AT
                );

        assertEquals(
                sessionId,
                session.id()
        );

        assertEquals(
                ownerUserId,
                session.ownerUserId()
        );

        assertEquals(
                definitionId,
                session.definitionId()
        );

        assertEquals(
                versionId,
                session.definitionVersionId()
        );

        assertEquals(
                AssessmentSessionStatus.IN_PROGRESS,
                session.status()
        );

        assertTrue(
                session.isActive()
        );

        assertFalse(
                session.isQuestionnaireSubmitted()
        );

        assertTrue(
                session
                        .questionnaireResponse()
                        .answers()
                        .isEmpty()
        );

        assertNull(
                session.initialResult()
        );

        assertNull(
                session.finalResult()
        );

        assertNull(
                session.completedAt()
        );
    }

    @Test
    void replacesQuestionnaireResponseBeforeSubmission() {
        AssessmentSession session =
                newInProgressSession();

        QuestionnaireResponse response =
                response();

        session.replaceQuestionnaireResponse(
                response
        );

        assertEquals(
                response,
                session.questionnaireResponse()
        );
    }

    @Test
    void submitsAmbiguousResultAndAwaitsClarification() {
        AssessmentSession session =
                newInProgressSession();

        QuestionnaireResponse finalResponse =
                response();

        InitialAssessmentResult initialResult =
                ambiguousInitialResult();

        AssessmentSubmissionOutcome outcome =
                session.submit(
                        finalResponse,
                        initialResult,
                        null,
                        SUBMITTED_AT
                );

        assertEquals(
                AssessmentSessionStatus
                        .AWAITING_CLARIFICATION,
                session.status()
        );

        assertTrue(
                session.isQuestionnaireSubmitted()
        );

        assertEquals(
                SUBMITTED_AT,
                session.questionnaireSubmittedAt()
        );

        assertEquals(
                finalResponse,
                session.questionnaireResponse()
        );

        assertEquals(
                initialResult,
                session.initialResult()
        );

        assertNull(
                session.finalResult()
        );

        assertNull(
                session.completedAt()
        );

        assertFalse(
                outcome.completed()
        );

        assertEquals(
                List.of(DIMENSION),
                outcome.ambiguousDimensions()
        );
    }

    @Test
    void submitsClearResultAndCompletesImmediately() {
        AssessmentSession session =
                newInProgressSession();

        QuestionnaireResponse finalResponse =
                response();

        InitialAssessmentResult initialResult =
                clearInitialResult();

        FinalAssessmentResult finalResult =
                finalResult();

        AssessmentSubmissionOutcome outcome =
                session.submit(
                        finalResponse,
                        initialResult,
                        finalResult,
                        SUBMITTED_AT
                );

        assertEquals(
                AssessmentSessionStatus.COMPLETED,
                session.status()
        );

        assertFalse(
                session.isActive()
        );

        assertEquals(
                SUBMITTED_AT,
                session.questionnaireSubmittedAt()
        );

        assertEquals(
                initialResult,
                session.initialResult()
        );

        assertEquals(
                finalResult,
                session.finalResult()
        );

        assertEquals(
                SUBMITTED_AT,
                session.completedAt()
        );

        assertTrue(
                outcome.completed()
        );

        assertTrue(
                outcome
                        .ambiguousDimensions()
                        .isEmpty()
        );
    }

    @Test
    void rejectsImmediateFinalResultWhenAmbiguityExists() {
        AssessmentSession session =
                newInProgressSession();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        session.submit(
                                response(),
                                ambiguousInitialResult(),
                                finalResult(),
                                SUBMITTED_AT
                        )
        );
    }

    @Test
    void requiresImmediateFinalResultWhenNoAmbiguityExists() {
        AssessmentSession session =
                newInProgressSession();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        session.submit(
                                response(),
                                clearInitialResult(),
                                null,
                                SUBMITTED_AT
                        )
        );
    }

    @Test
    void rejectsSecondSubmission() {
        AssessmentSession session =
                newInProgressSession();

        session.submit(
                response(),
                clearInitialResult(),
                finalResult(),
                SUBMITTED_AT
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        session.submit(
                                response(),
                                clearInitialResult(),
                                finalResult(),
                                SUBMITTED_AT.plusSeconds(1)
                        )
        );
    }

    @Test
    void rejectsQuestionnaireReplacementAfterSubmission() {
        AssessmentSession session =
                newInProgressSession();

        session.submit(
                response(),
                ambiguousInitialResult(),
                null,
                SUBMITTED_AT
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        session.replaceQuestionnaireResponse(
                                QuestionnaireResponse.empty()
                        )
        );
    }

    @Test
    void rejectsSubmissionBeforeCreation() {
        AssessmentSession session =
                newInProgressSession();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        session.submit(
                                response(),
                                clearInitialResult(),
                                finalResult(),
                                CREATED_AT.minusSeconds(1)
                        )
        );
    }

    @Test
    void rejectsCompletedRestoreWithoutFinalResult() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        AssessmentSession.restore(
                                AssessmentSessionId.newId(),
                                new UserId(
                                        UUID.randomUUID()
                                ),
                                AssessmentDefinitionId.newId(),
                                AssessmentDefinitionVersionId.newId(),
                                AssessmentSessionStatus.COMPLETED,
                                response(),
                                SUBMITTED_AT,
                                clearInitialResult(),
                                null,
                                CREATED_AT,
                                SUBMITTED_AT,
                                null
                        )
        );
    }

    @Test
    void rejectsPostSubmissionActiveRestoreWithoutInitialResult() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        AssessmentSession.restore(
                                AssessmentSessionId.newId(),
                                new UserId(
                                        UUID.randomUUID()
                                ),
                                AssessmentDefinitionId.newId(),
                                AssessmentDefinitionVersionId.newId(),
                                AssessmentSessionStatus
                                        .AWAITING_CLARIFICATION,
                                response(),
                                SUBMITTED_AT,
                                null,
                                null,
                                CREATED_AT,
                                null,
                                null
                        )
        );
    }

    @Test
    void canAbandonInProgressSession() {
        AssessmentSession session =
                newInProgressSession();

        Instant abandonedAt =
                CREATED_AT.plusSeconds(60);

        session.abandon(
                abandonedAt
        );

        assertEquals(
                AssessmentSessionStatus.ABANDONED,
                session.status()
        );

        assertEquals(
                abandonedAt,
                session.abandonedAt()
        );
    }

    @Test
    void canAbandonPostSubmissionSessionAndPreserveInitialEvidence() {
        AssessmentSession session =
                newInProgressSession();

        InitialAssessmentResult initialResult =
                ambiguousInitialResult();

        session.submit(
                response(),
                initialResult,
                null,
                SUBMITTED_AT
        );

        Instant abandonedAt =
                SUBMITTED_AT.plusSeconds(30);

        session.abandon(
                abandonedAt
        );

        assertEquals(
                AssessmentSessionStatus.ABANDONED,
                session.status()
        );

        assertEquals(
                initialResult,
                session.initialResult()
        );

        assertEquals(
                SUBMITTED_AT,
                session.questionnaireSubmittedAt()
        );

        assertNull(
                session.finalResult()
        );
    }

    @Test
    void rejectsAbandoningCompletedSession() {
        AssessmentSession session =
                newInProgressSession();

        session.submit(
                response(),
                clearInitialResult(),
                finalResult(),
                SUBMITTED_AT
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        session.abandon(
                                SUBMITTED_AT.plusSeconds(30)
                        )
        );
    }

    private AssessmentSession newInProgressSession() {
        return AssessmentSession.start(
                AssessmentSessionId.newId(),
                new UserId(
                        UUID.randomUUID()
                ),
                AssessmentDefinitionId.newId(),
                AssessmentDefinitionVersionId.newId(),
                CREATED_AT
        );
    }

    private QuestionnaireResponse response() {
        return new QuestionnaireResponse(
                List.of(
                        new Answer(
                                new QuestionId("Q1"),
                                5
                        )
                )
        );
    }

    private InitialAssessmentResult clearInitialResult() {
        return new InitialAssessmentResult(
                List.of(
                        new InitialDimensionResult(
                                DIMENSION,
                                3,
                                POLE_X,
                                false
                        )
                )
        );
    }

    private InitialAssessmentResult ambiguousInitialResult() {
        return new InitialAssessmentResult(
                List.of(
                        new InitialDimensionResult(
                                DIMENSION,
                                2,
                                POLE_X,
                                true
                        )
                )
        );
    }

    private FinalAssessmentResult finalResult() {
        return new FinalAssessmentResult(
                "X",
                List.of(
                        new FinalDimensionConclusion(
                                DIMENSION,
                                POLE_X,
                                FinalDecisionSource.QUESTIONNAIRE
                        )
                )
        );
    }
}