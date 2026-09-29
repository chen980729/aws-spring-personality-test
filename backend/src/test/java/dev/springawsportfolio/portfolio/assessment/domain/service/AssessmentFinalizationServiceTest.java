package dev.springawsportfolio.portfolio.assessment.domain.service;

import dev.springawsportfolio.portfolio.assessment.domain.clarification.AIProvenance;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationConfidence;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResolution;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResult;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDecisionSource;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.tiebreak.DimensionTieBreak;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentFinalizationServiceTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-28T00:00:00Z"
            );

    private AssessmentFinalizationService service;

    @BeforeEach
    void setUp() {
        service =
                new AssessmentFinalizationService();
    }

    @Test
    void finalizesClearDimensionsFromQuestionnaireOnly() {
        DimensionCode firstDimension =
                new DimensionCode("AB");

        DimensionCode secondDimension =
                new DimensionCode("CD");

        PoleCode poleA =
                new PoleCode("A");

        PoleCode poleB =
                new PoleCode("B");

        PoleCode poleC =
                new PoleCode("C");

        PoleCode poleD =
                new PoleCode("D");

        List<DimensionDefinition> dimensions =
                List.of(
                        new DimensionDefinition(
                                secondDimension,
                                2,
                                poleC,
                                poleD
                        ),
                        new DimensionDefinition(
                                firstDimension,
                                1,
                                poleA,
                                poleB
                        )
                );

        InitialAssessmentResult initialResult =
                new InitialAssessmentResult(
                        List.of(
                                new InitialDimensionResult(
                                        secondDimension,
                                        -4,
                                        poleD,
                                        false
                                ),
                                new InitialDimensionResult(
                                        firstDimension,
                                        5,
                                        poleA,
                                        false
                                )
                        )
                );

        FinalAssessmentResult result =
                service.finalizeWithoutClarification(
                        dimensions,
                        initialResult
                );

        assertEquals(
                "AD",
                result.finalType()
        );

        assertEquals(
                FinalDecisionSource.QUESTIONNAIRE,
                result
                        .dimensions()
                        .get(0)
                        .decisionSource()
        );

        assertEquals(
                FinalDecisionSource.QUESTIONNAIRE,
                result
                        .dimensions()
                        .get(1)
                        .decisionSource()
        );
    }

    @Test
    void rejectsImmediateFinalizationWhenAmbiguityExists() {
        DimensionCode dimension =
                new DimensionCode("AB");

        PoleCode poleA =
                new PoleCode("A");

        PoleCode poleB =
                new PoleCode("B");

        InitialAssessmentResult initialResult =
                new InitialAssessmentResult(
                        List.of(
                                new InitialDimensionResult(
                                        dimension,
                                        2,
                                        poleA,
                                        true
                                )
                        )
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        service.finalizeWithoutClarification(
                                List.of(
                                        definition(
                                                dimension,
                                                poleA,
                                                poleB
                                        )
                                ),
                                initialResult
                        )
        );
    }

    @Test
    void rejectsMissingDimensionResult() {
        DimensionCode firstDimension =
                new DimensionCode("AB");

        DimensionCode secondDimension =
                new DimensionCode("CD");

        PoleCode poleA =
                new PoleCode("A");

        PoleCode poleB =
                new PoleCode("B");

        PoleCode poleC =
                new PoleCode("C");

        PoleCode poleD =
                new PoleCode("D");

        InitialAssessmentResult initialResult =
                new InitialAssessmentResult(
                        List.of(
                                new InitialDimensionResult(
                                        firstDimension,
                                        4,
                                        poleA,
                                        false
                                )
                        )
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.finalizeWithoutClarification(
                                List.of(
                                        definition(
                                                firstDimension,
                                                poleA,
                                                poleB
                                        ),
                                        new DimensionDefinition(
                                                secondDimension,
                                                2,
                                                poleC,
                                                poleD
                                        )
                                ),
                                initialResult
                        )
        );
    }

    @Test
    void resolvedClarificationThatConfirmsBaselineUsesConfirmedSource() {
        DimensionCode dimension =
                new DimensionCode("AB");

        PoleCode poleA =
                new PoleCode("A");

        PoleCode poleB =
                new PoleCode("B");

        AssessmentSessionId sessionId =
                AssessmentSessionId.newId();

        InitialAssessmentResult initialResult =
                ambiguousResult(
                        dimension,
                        2,
                        poleA
                );

        DimensionClarification clarification =
                resolvedClarification(
                        sessionId,
                        dimension,
                        poleA
                );

        FinalAssessmentResult result =
                service.finalizeIfReady(
                                List.of(
                                        definition(
                                                dimension,
                                                poleA,
                                                poleB
                                        )
                                ),
                                initialResult,
                                List.of(clarification),
                                List.of()
                        )
                        .orElseThrow();

        assertEquals(
                poleA,
                result
                        .dimensions()
                        .getFirst()
                        .finalPreference()
        );

        assertEquals(
                FinalDecisionSource
                        .QUESTIONNAIRE_CONFIRMED_BY_CLARIFICATION,
                result
                        .dimensions()
                        .getFirst()
                        .decisionSource()
        );
    }

    @Test
    void resolvedClarificationCanOverrideAmbiguousBaseline() {
        DimensionCode dimension =
                new DimensionCode("AB");

        PoleCode poleA =
                new PoleCode("A");

        PoleCode poleB =
                new PoleCode("B");

        AssessmentSessionId sessionId =
                AssessmentSessionId.newId();

        FinalAssessmentResult result =
                service.finalizeIfReady(
                                List.of(
                                        definition(
                                                dimension,
                                                poleA,
                                                poleB
                                        )
                                ),
                                ambiguousResult(
                                        dimension,
                                        2,
                                        poleA
                                ),
                                List.of(
                                        resolvedClarification(
                                                sessionId,
                                                dimension,
                                                poleB
                                        )
                                ),
                                List.of()
                        )
                        .orElseThrow();

        assertEquals(
                poleB,
                result
                        .dimensions()
                        .getFirst()
                        .finalPreference()
        );

        assertEquals(
                FinalDecisionSource.AI_CLARIFICATION,
                result
                        .dimensions()
                        .getFirst()
                        .decisionSource()
        );
    }

    @Test
    void exactTieCanBeResolvedByClarificationWithoutUserTieBreak() {
        DimensionCode dimension =
                new DimensionCode("AB");

        PoleCode poleA =
                new PoleCode("A");

        PoleCode poleB =
                new PoleCode("B");

        AssessmentSessionId sessionId =
                AssessmentSessionId.newId();

        FinalAssessmentResult result =
                service.finalizeIfReady(
                                List.of(
                                        definition(
                                                dimension,
                                                poleA,
                                                poleB
                                        )
                                ),
                                exactTieResult(
                                        dimension
                                ),
                                List.of(
                                        resolvedClarification(
                                                sessionId,
                                                dimension,
                                                poleB
                                        )
                                ),
                                List.of()
                        )
                        .orElseThrow();

        assertEquals(
                poleB,
                result
                        .dimensions()
                        .getFirst()
                        .finalPreference()
        );

        assertEquals(
                FinalDecisionSource.AI_CLARIFICATION,
                result
                        .dimensions()
                        .getFirst()
                        .decisionSource()
        );
    }

    @Test
    void unclearClarificationFallsBackToNonZeroQuestionnaireBaseline() {
        DimensionCode dimension =
                new DimensionCode("AB");

        PoleCode poleA =
                new PoleCode("A");

        PoleCode poleB =
                new PoleCode("B");

        AssessmentSessionId sessionId =
                AssessmentSessionId.newId();

        FinalAssessmentResult result =
                service.finalizeIfReady(
                                List.of(
                                        definition(
                                                dimension,
                                                poleA,
                                                poleB
                                        )
                                ),
                                ambiguousResult(
                                        dimension,
                                        -2,
                                        poleB
                                ),
                                List.of(
                                        unclearClarification(
                                                sessionId,
                                                dimension
                                        )
                                ),
                                List.of()
                        )
                        .orElseThrow();

        assertEquals(
                poleB,
                result
                        .dimensions()
                        .getFirst()
                        .finalPreference()
        );

        assertEquals(
                FinalDecisionSource.QUESTIONNAIRE_FALLBACK,
                result
                        .dimensions()
                        .getFirst()
                        .decisionSource()
        );
    }

    @Test
    void skippedClarificationFallsBackToNonZeroQuestionnaireBaseline() {
        DimensionCode dimension =
                new DimensionCode("AB");

        PoleCode poleA =
                new PoleCode("A");

        PoleCode poleB =
                new PoleCode("B");

        AssessmentSessionId sessionId =
                AssessmentSessionId.newId();

        FinalAssessmentResult result =
                service.finalizeIfReady(
                                List.of(
                                        definition(
                                                dimension,
                                                poleA,
                                                poleB
                                        )
                                ),
                                ambiguousResult(
                                        dimension,
                                        2,
                                        poleA
                                ),
                                List.of(
                                        skippedClarification(
                                                sessionId,
                                                dimension
                                        )
                                ),
                                List.of()
                        )
                        .orElseThrow();

        assertEquals(
                poleA,
                result
                        .dimensions()
                        .getFirst()
                        .finalPreference()
        );

        assertEquals(
                FinalDecisionSource.QUESTIONNAIRE_FALLBACK,
                result
                        .dimensions()
                        .getFirst()
                        .decisionSource()
        );
    }

    @Test
    void exactTieWithUnclearClarificationIsNotReadyWithoutTieBreak() {
        DimensionCode dimension =
                new DimensionCode("AB");

        PoleCode poleA =
                new PoleCode("A");

        PoleCode poleB =
                new PoleCode("B");

        AssessmentSessionId sessionId =
                AssessmentSessionId.newId();

        Optional<FinalAssessmentResult> result =
                service.finalizeIfReady(
                        List.of(
                                definition(
                                        dimension,
                                        poleA,
                                        poleB
                                )
                        ),
                        exactTieResult(
                                dimension
                        ),
                        List.of(
                                unclearClarification(
                                        sessionId,
                                        dimension
                                )
                        ),
                        List.of()
                );

        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    void exactTieWithSkippedClarificationIsNotReadyWithoutTieBreak() {
        DimensionCode dimension =
                new DimensionCode("AB");

        PoleCode poleA =
                new PoleCode("A");

        PoleCode poleB =
                new PoleCode("B");

        AssessmentSessionId sessionId =
                AssessmentSessionId.newId();

        Optional<FinalAssessmentResult> result =
                service.finalizeIfReady(
                        List.of(
                                definition(
                                        dimension,
                                        poleA,
                                        poleB
                                )
                        ),
                        exactTieResult(
                                dimension
                        ),
                        List.of(
                                skippedClarification(
                                        sessionId,
                                        dimension
                                )
                        ),
                        List.of()
                );

        assertFalse(
                result.isPresent()
        );
    }

    @Test
    void exactTieWithSkippedClarificationUsesPersistedUserTieBreak() {
        DimensionCode dimension =
                new DimensionCode("AB");

        PoleCode poleA =
                new PoleCode("A");

        PoleCode poleB =
                new PoleCode("B");

        AssessmentSessionId sessionId =
                AssessmentSessionId.newId();

        FinalAssessmentResult result =
                service.finalizeIfReady(
                                List.of(
                                        definition(
                                                dimension,
                                                poleA,
                                                poleB
                                        )
                                ),
                                exactTieResult(
                                        dimension
                                ),
                                List.of(
                                        skippedClarification(
                                                sessionId,
                                                dimension
                                        )
                                ),
                                List.of(
                                        new DimensionTieBreak(
                                                sessionId,
                                                dimension,
                                                poleB,
                                                NOW.plusSeconds(30)
                                        )
                                )
                        )
                        .orElseThrow();

        assertEquals(
                poleB,
                result
                        .dimensions()
                        .getFirst()
                        .finalPreference()
        );

        assertEquals(
                FinalDecisionSource.USER_TIE_BREAK,
                result
                        .dimensions()
                        .getFirst()
                        .decisionSource()
        );
    }

    @Test
    void pendingClarificationKeepsAssessmentNotReady() {
        DimensionCode dimension =
                new DimensionCode("AB");

        PoleCode poleA =
                new PoleCode("A");

        PoleCode poleB =
                new PoleCode("B");

        AssessmentSessionId sessionId =
                AssessmentSessionId.newId();

        DimensionClarification clarification =
                DimensionClarification.pending(
                        DimensionClarificationId.newId(),
                        sessionId,
                        dimension,
                        NOW
                );

        Optional<FinalAssessmentResult> result =
                service.finalizeIfReady(
                        List.of(
                                definition(
                                        dimension,
                                        poleA,
                                        poleB
                                )
                        ),
                        ambiguousResult(
                                dimension,
                                2,
                                poleA
                        ),
                        List.of(clarification),
                        List.of()
                );

        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    void failedRetryableClarificationKeepsAssessmentNotReady() {
        DimensionCode dimension =
                new DimensionCode("AB");

        PoleCode poleA =
                new PoleCode("A");

        PoleCode poleB =
                new PoleCode("B");

        AssessmentSessionId sessionId =
                AssessmentSessionId.newId();

        DimensionClarification clarification =
                DimensionClarification.pending(
                        DimensionClarificationId.newId(),
                        sessionId,
                        dimension,
                        NOW
                );

        clarification.start(
                NOW.plusSeconds(10)
        );

        clarification.failRetryable(
                clarification.activeExecutionToken(),
                NOW.plusSeconds(20)
        );

        Optional<FinalAssessmentResult> result =
                service.finalizeIfReady(
                        List.of(
                                definition(
                                        dimension,
                                        poleA,
                                        poleB
                                )
                        ),
                        ambiguousResult(
                                dimension,
                                2,
                                poleA
                        ),
                        List.of(clarification),
                        List.of()
                );

        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    void rejectsClarificationPoleOutsideBoundDimension() {
        DimensionCode dimension =
                new DimensionCode("AB");

        PoleCode poleA =
                new PoleCode("A");

        PoleCode poleB =
                new PoleCode("B");

        AssessmentSessionId sessionId =
                AssessmentSessionId.newId();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.finalizeIfReady(
                                List.of(
                                        definition(
                                                dimension,
                                                poleA,
                                                poleB
                                        )
                                ),
                                ambiguousResult(
                                        dimension,
                                        2,
                                        poleA
                                ),
                                List.of(
                                        resolvedClarification(
                                                sessionId,
                                                dimension,
                                                new PoleCode("Z")
                                        )
                                ),
                                List.of()
                        )
        );
    }

    @Test
    void rejectsTieBreakFromDifferentSession() {
        DimensionCode dimension =
                new DimensionCode("AB");

        PoleCode poleA =
                new PoleCode("A");

        PoleCode poleB =
                new PoleCode("B");

        AssessmentSessionId clarificationSession =
                AssessmentSessionId.newId();

        AssessmentSessionId differentSession =
                AssessmentSessionId.newId();

        assertThrows(
                IllegalStateException.class,
                () ->
                        service.finalizeIfReady(
                                List.of(
                                        definition(
                                                dimension,
                                                poleA,
                                                poleB
                                        )
                                ),
                                exactTieResult(
                                        dimension
                                ),
                                List.of(
                                        skippedClarification(
                                                clarificationSession,
                                                dimension
                                        )
                                ),
                                List.of(
                                        new DimensionTieBreak(
                                                differentSession,
                                                dimension,
                                                poleA,
                                                NOW.plusSeconds(30)
                                        )
                                )
                        )
        );
    }

    private DimensionDefinition definition(
            DimensionCode dimension,
            PoleCode poleA,
            PoleCode poleB
    ) {
        return new DimensionDefinition(
                dimension,
                1,
                poleA,
                poleB
        );
    }

    private InitialAssessmentResult ambiguousResult(
            DimensionCode dimension,
            int rawScore,
            PoleCode questionnairePreference
    ) {
        return new InitialAssessmentResult(
                List.of(
                        new InitialDimensionResult(
                                dimension,
                                rawScore,
                                questionnairePreference,
                                true
                        )
                )
        );
    }

    private InitialAssessmentResult exactTieResult(
            DimensionCode dimension
    ) {
        return new InitialAssessmentResult(
                List.of(
                        new InitialDimensionResult(
                                dimension,
                                0,
                                null,
                                true
                        )
                )
        );
    }

    private DimensionClarification resolvedClarification(
            AssessmentSessionId sessionId,
            DimensionCode dimension,
            PoleCode suggestedPole
    ) {
        DimensionClarification clarification =
                inProgressClarification(
                        sessionId,
                        dimension
                );

        clarification.acceptResult(
                clarification.activeExecutionToken(),
                new ClarificationResult(
                        ClarificationResolution.RESOLVED,
                        suggestedPole,
                        ClarificationConfidence.HIGH,
                        "Resolved clarification."
                ),
                provenance(),
                NOW.plusSeconds(20)
        );

        return clarification;
    }

    private DimensionClarification unclearClarification(
            AssessmentSessionId sessionId,
            DimensionCode dimension
    ) {
        DimensionClarification clarification =
                inProgressClarification(
                        sessionId,
                        dimension
                );

        clarification.acceptResult(
                clarification.activeExecutionToken(),
                new ClarificationResult(
                        ClarificationResolution.UNCLEAR,
                        null,
                        ClarificationConfidence.LOW,
                        "Clarification remained unclear."
                ),
                provenance(),
                NOW.plusSeconds(20)
        );

        return clarification;
    }

    private DimensionClarification skippedClarification(
            AssessmentSessionId sessionId,
            DimensionCode dimension
    ) {
        DimensionClarification clarification =
                DimensionClarification.pending(
                        DimensionClarificationId.newId(),
                        sessionId,
                        dimension,
                        NOW
                );

        clarification.skip(
                NOW.plusSeconds(10)
        );

        return clarification;
    }

    private DimensionClarification inProgressClarification(
            AssessmentSessionId sessionId,
            DimensionCode dimension
    ) {
        DimensionClarification clarification =
                DimensionClarification.pending(
                        DimensionClarificationId.newId(),
                        sessionId,
                        dimension,
                        NOW
                );

        clarification.start(
                NOW.plusSeconds(10)
        );

        return clarification;
    }

    private AIProvenance provenance() {
        return new AIProvenance(
                "test-provider",
                "test-model",
                "clarification-v1"
        );
    }
}
