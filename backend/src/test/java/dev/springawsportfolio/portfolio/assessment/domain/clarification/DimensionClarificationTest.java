package dev.springawsportfolio.portfolio.assessment.domain.clarification;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DimensionClarificationTest {

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-09-28T00:00:00Z"
            );

    private static final Instant STARTED_AT =
            CREATED_AT.plusSeconds(10);

    private static final Instant FAILED_AT =
            STARTED_AT.plusSeconds(10);

    private static final Instant RETRIED_AT =
            FAILED_AT.plusSeconds(10);

    private static final Instant ACCEPTED_AT =
            RETRIED_AT.plusSeconds(10);

    private static final DimensionCode DIMENSION =
            new DimensionCode("EI");

    private static final PoleCode POLE_I =
            new PoleCode("I");

    private static final AIProvenance PROVENANCE =
            new AIProvenance(
                    "test-provider",
                    "test-model",
                    "clarification-v1"
            );

    @Test
    void pendingClarificationStartsInProgressWithExecutionToken() {
        DimensionClarification clarification =
                pending();

        ClarificationExecutionToken token =
                clarification.start(
                        STARTED_AT
                );

        assertEquals(
                DimensionClarificationStatus.IN_PROGRESS,
                clarification.status()
        );

        assertEquals(
                token,
                clarification.activeExecutionToken()
        );

        assertTrue(
                clarification.matchesActiveExecution(
                        token
                )
        );

        assertEquals(
                STARTED_AT,
                clarification.startedAt()
        );

        assertEquals(
                STARTED_AT,
                clarification.updatedAt()
        );

        assertNull(
                clarification.result()
        );

        assertNull(
                clarification.aiProvenance()
        );
    }

    @Test
    void acceptsResolvedResultOnlyForMatchingActiveExecution() {
        DimensionClarification clarification =
                pending();

        ClarificationExecutionToken token =
                clarification.start(
                        STARTED_AT
                );

        ClarificationResult result =
                new ClarificationResult(
                        ClarificationResolution.RESOLVED,
                        POLE_I,
                        ClarificationConfidence.HIGH,
                        "User consistently described an introversion preference."
                );

        clarification.acceptResult(
                token,
                result,
                PROVENANCE,
                ACCEPTED_AT
        );

        assertEquals(
                DimensionClarificationStatus.CLARIFIED,
                clarification.status()
        );

        assertNull(
                clarification.activeExecutionToken()
        );

        assertEquals(
                result,
                clarification.result()
        );

        assertEquals(
                PROVENANCE,
                clarification.aiProvenance()
        );

        assertEquals(
                ACCEPTED_AT,
                clarification.acceptedAt()
        );

        assertEquals(
                ACCEPTED_AT,
                clarification.updatedAt()
        );

        assertTrue(
                clarification.status().isTerminal()
        );
    }

    @Test
    void staleExecutionCannotAcceptResult() {
        DimensionClarification clarification =
                pending();

        ClarificationExecutionToken activeToken =
                clarification.start(
                        STARTED_AT
                );

        ClarificationExecutionToken staleToken =
                ClarificationExecutionToken.newToken();

        assertNotEquals(
                activeToken,
                staleToken
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        clarification.acceptResult(
                                staleToken,
                                new ClarificationResult(
                                        ClarificationResolution.RESOLVED,
                                        POLE_I,
                                        ClarificationConfidence.HIGH,
                                        null
                                ),
                                PROVENANCE,
                                ACCEPTED_AT
                        )
        );

        assertEquals(
                DimensionClarificationStatus.IN_PROGRESS,
                clarification.status()
        );

        assertEquals(
                activeToken,
                clarification.activeExecutionToken()
        );
    }

    @Test
    void acceptsUnclearResultWithoutSuggestedPole() {
        DimensionClarification clarification =
                pending();

        ClarificationExecutionToken token =
                clarification.start(
                        STARTED_AT
                );

        ClarificationResult result =
                new ClarificationResult(
                        ClarificationResolution.UNCLEAR,
                        null,
                        ClarificationConfidence.LOW,
                        "Available evidence did not resolve the dimension."
                );

        clarification.acceptResult(
                token,
                result,
                PROVENANCE,
                ACCEPTED_AT
        );

        assertEquals(
                DimensionClarificationStatus.CLARIFIED,
                clarification.status()
        );

        assertEquals(
                ClarificationResolution.UNCLEAR,
                clarification.result().resolution()
        );

        assertNull(
                clarification.result().suggestedPole()
        );
    }

    @Test
    void technicalFailureCanBeRetriedWithNewExecutionToken() {
        DimensionClarification clarification =
                pending();

        ClarificationExecutionToken firstToken =
                clarification.start(
                        STARTED_AT
                );

        clarification.failRetryable(
                firstToken,
                FAILED_AT
        );

        assertEquals(
                DimensionClarificationStatus.FAILED_RETRYABLE,
                clarification.status()
        );

        assertNull(
                clarification.activeExecutionToken()
        );

        assertEquals(
                STARTED_AT,
                clarification.startedAt()
        );

        ClarificationExecutionToken retryToken =
                clarification.retry(
                        RETRIED_AT
                );

        assertEquals(
                DimensionClarificationStatus.IN_PROGRESS,
                clarification.status()
        );

        assertNotEquals(
                firstToken,
                retryToken
        );

        assertEquals(
                retryToken,
                clarification.activeExecutionToken()
        );

        assertFalse(
                clarification.matchesActiveExecution(
                        firstToken
                )
        );

        assertEquals(
                STARTED_AT,
                clarification.startedAt()
        );

        assertEquals(
                RETRIED_AT,
                clarification.updatedAt()
        );
    }

    @Test
    void canSkipPendingInProgressOrRetryableClarification() {
        DimensionClarification pending =
                pending();

        pending.skip(
                STARTED_AT
        );

        assertEquals(
                DimensionClarificationStatus.SKIPPED,
                pending.status()
        );

        assertNull(
                pending.startedAt()
        );

        DimensionClarification inProgress =
                pending();

        inProgress.start(
                STARTED_AT
        );

        assertNotNull(
                inProgress.activeExecutionToken()
        );

        inProgress.skip(
                FAILED_AT
        );

        assertEquals(
                DimensionClarificationStatus.SKIPPED,
                inProgress.status()
        );

        assertEquals(
                STARTED_AT,
                inProgress.startedAt()
        );

        assertNull(
                inProgress.activeExecutionToken()
        );

        DimensionClarification retryable =
                pending();

        ClarificationExecutionToken token =
                retryable.start(
                        STARTED_AT
                );

        retryable.failRetryable(
                token,
                FAILED_AT
        );

        retryable.skip(
                RETRIED_AT
        );

        assertEquals(
                DimensionClarificationStatus.SKIPPED,
                retryable.status()
        );

        assertTrue(
                retryable.status().isTerminal()
        );
    }

    @Test
    void resolvedResultRequiresSuggestedPole() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ClarificationResult(
                                ClarificationResolution.RESOLVED,
                                null,
                                ClarificationConfidence.MEDIUM,
                                null
                        )
        );
    }

    @Test
    void unclearResultForbidsSuggestedPole() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ClarificationResult(
                                ClarificationResolution.UNCLEAR,
                                POLE_I,
                                ClarificationConfidence.MEDIUM,
                                null
                        )
        );
    }

    @Test
    void restoreRejectsAcceptedDataForNonClarifiedState() {
        ClarificationResult result =
                new ClarificationResult(
                        ClarificationResolution.RESOLVED,
                        POLE_I,
                        ClarificationConfidence.HIGH,
                        null
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        DimensionClarification.restore(
                                DimensionClarificationId.newId(),
                                AssessmentSessionId.newId(),
                                DIMENSION,
                                DimensionClarificationStatus.FAILED_RETRYABLE,
                                null,
                                result,
                                PROVENANCE,
                                STARTED_AT,
                                ACCEPTED_AT,
                                ACCEPTED_AT
                        )
        );
    }

    @Test
    void restoreRequiresExecutionTokenOnlyWhileInProgress() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        DimensionClarification.restore(
                                DimensionClarificationId.newId(),
                                AssessmentSessionId.newId(),
                                DIMENSION,
                                DimensionClarificationStatus.IN_PROGRESS,
                                null,
                                null,
                                null,
                                STARTED_AT,
                                null,
                                STARTED_AT
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        DimensionClarification.restore(
                                DimensionClarificationId.newId(),
                                AssessmentSessionId.newId(),
                                DIMENSION,
                                DimensionClarificationStatus.FAILED_RETRYABLE,
                                ClarificationExecutionToken.newToken(),
                                null,
                                null,
                                STARTED_AT,
                                null,
                                FAILED_AT
                        )
        );
    }

    @Test
    void terminalClarificationCannotTransitionAgain() {
        DimensionClarification clarification =
                pending();

        clarification.skip(
                STARTED_AT
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        clarification.start(
                                FAILED_AT
                        )
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        clarification.retry(
                                FAILED_AT
                        )
        );
    }

    @Test
    void transitionTimeCannotMoveBackwards() {
        DimensionClarification clarification =
                pending();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        clarification.start(
                                CREATED_AT.minusSeconds(1)
                        )
        );
    }

    private DimensionClarification pending() {
        return DimensionClarification.pending(
                DimensionClarificationId.newId(),
                AssessmentSessionId.newId(),
                DIMENSION,
                CREATED_AT
        );
    }
}
