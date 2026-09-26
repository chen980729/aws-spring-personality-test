package dev.springawsportfolio.portfolio.assessment.domain.session;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionId;
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

        assertEquals(
                CREATED_AT,
                session.createdAt()
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
                session.questionnaireSubmittedAt()
        );

        assertNull(
                session.completedAt()
        );

        assertNull(
                session.abandonedAt()
        );
    }

    @Test
    void newSessionStartsWithEmptyQuestionnaireResponse() {
        AssessmentSession session =
                newInProgressSession();

        assertTrue(
                session
                        .questionnaireResponse()
                        .answers()
                        .isEmpty()
        );

        assertFalse(
                session.isQuestionnaireSubmitted()
        );
    }

    @Test
    void replacesQuestionnaireResponseBeforeSubmission() {
        AssessmentSession session =
                newInProgressSession();

        QuestionnaireResponse response =
                new QuestionnaireResponse(
                        List.of(
                                new Answer(
                                        new QuestionId("Q1"),
                                        5
                                )
                        )
                );

        session.replaceQuestionnaireResponse(
                response
        );

        assertEquals(
                response,
                session.questionnaireResponse()
        );
    }

    @Test
    void rejectsReplacingQuestionnaireResponseAfterSubmission() {
        AssessmentSession session =
                restoredSession(
                        AssessmentSessionStatus
                                .AWAITING_CLARIFICATION,
                        SUBMITTED_AT,
                        null,
                        null
                );

        QuestionnaireResponse replacement =
                new QuestionnaireResponse(
                        List.of(
                                new Answer(
                                        new QuestionId("Q1"),
                                        5
                                )
                        )
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        session.replaceQuestionnaireResponse(
                                replacement
                        )
        );
    }

    @Test
    void abandonsActiveSession() {
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

        assertFalse(
                session.isActive()
        );

        assertEquals(
                abandonedAt,
                session.abandonedAt()
        );

        assertNull(
                session.completedAt()
        );
    }

    @Test
    void canAbandonAwaitingClarificationSession() {
        AssessmentSession session =
                restoredSession(
                        AssessmentSessionStatus
                                .AWAITING_CLARIFICATION,
                        SUBMITTED_AT,
                        null,
                        null
                );

        session.abandon(
                CREATED_AT.plusSeconds(60)
        );

        assertEquals(
                AssessmentSessionStatus.ABANDONED,
                session.status()
        );
    }

    @Test
    void canAbandonClarificationInProgressSession() {
        AssessmentSession session =
                restoredSession(
                        AssessmentSessionStatus
                                .CLARIFICATION_IN_PROGRESS,
                        SUBMITTED_AT,
                        null,
                        null
                );

        session.abandon(
                CREATED_AT.plusSeconds(60)
        );

        assertEquals(
                AssessmentSessionStatus.ABANDONED,
                session.status()
        );
    }

    @Test
    void rejectsAbandoningCompletedSession() {
        AssessmentSession session =
                restoredSession(
                        AssessmentSessionStatus.COMPLETED,
                        SUBMITTED_AT,
                        CREATED_AT.plusSeconds(60),
                        null
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        session.abandon(
                                CREATED_AT.plusSeconds(120)
                        )
        );
    }

    @Test
    void rejectsAbandoningAlreadyAbandonedSession() {
        AssessmentSession session =
                restoredSession(
                        AssessmentSessionStatus.ABANDONED,
                        null,
                        null,
                        CREATED_AT.plusSeconds(60)
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        session.abandon(
                                CREATED_AT.plusSeconds(120)
                        )
        );
    }

    @Test
    void rejectsAbandonmentBeforeCreation() {
        AssessmentSession session =
                newInProgressSession();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        session.abandon(
                                CREATED_AT.minusSeconds(1)
                        )
        );
    }

    @Test
    void rejectsActiveSessionWithTerminalTimestamp() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        restoredSession(
                                AssessmentSessionStatus.IN_PROGRESS,
                                null,
                                null,
                                CREATED_AT.plusSeconds(60)
                        )
        );
    }

    @Test
    void rejectsAwaitingClarificationSessionWithoutSubmission() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        restoredSession(
                                AssessmentSessionStatus
                                        .AWAITING_CLARIFICATION,
                                null,
                                null,
                                null
                        )
        );
    }

    @Test
    void rejectsClarificationInProgressSessionWithoutSubmission() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        restoredSession(
                                AssessmentSessionStatus
                                        .CLARIFICATION_IN_PROGRESS,
                                null,
                                null,
                                null
                        )
        );
    }

    @Test
    void rejectsCompletedSessionWithoutCompletedAt() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        restoredSession(
                                AssessmentSessionStatus.COMPLETED,
                                SUBMITTED_AT,
                                null,
                                null
                        )
        );
    }

    @Test
    void rejectsCompletedSessionWithoutSubmission() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        restoredSession(
                                AssessmentSessionStatus.COMPLETED,
                                null,
                                CREATED_AT.plusSeconds(60),
                                null
                        )
        );
    }

    @Test
    void rejectsAbandonedSessionWithoutAbandonedAt() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        restoredSession(
                                AssessmentSessionStatus.ABANDONED,
                                null,
                                null,
                                null
                        )
        );
    }

    @Test
    void rejectsSubmissionTimestampBeforeCreation() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        restoredSession(
                                AssessmentSessionStatus
                                        .AWAITING_CLARIFICATION,
                                CREATED_AT.minusSeconds(1),
                                null,
                                null
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

    private AssessmentSession restoredSession(
            AssessmentSessionStatus status,
            Instant submittedAt,
            Instant completedAt,
            Instant abandonedAt
    ) {
        return AssessmentSession.restore(
                AssessmentSessionId.newId(),
                new UserId(
                        UUID.randomUUID()
                ),
                AssessmentDefinitionId.newId(),
                AssessmentDefinitionVersionId.newId(),
                status,
                QuestionnaireResponse.empty(),
                submittedAt,
                CREATED_AT,
                completedAt,
                abandonedAt
        );
    }
}
