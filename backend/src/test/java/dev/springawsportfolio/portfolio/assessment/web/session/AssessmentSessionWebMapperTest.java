package dev.springawsportfolio.portfolio.assessment.web.session;

import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionWorkflowSnapshot;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDecisionSource;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDimensionConclusion;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.AssessmentSessionResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class AssessmentSessionWebMapperTest {

    private final AssessmentSessionWebMapper mapper =
            new AssessmentSessionWebMapper();

    @Test
    void mapsPersistedClarificationResultAndTieBreak() {
        Instant submittedAt =
                Instant.parse(
                        "2026-09-27T10:00:00Z"
                );

        Instant startedAt =
                Instant.parse(
                        "2026-09-27T10:01:00Z"
                );

        Instant acceptedAt =
                Instant.parse(
                        "2026-09-27T10:02:00Z"
                );

        Instant decidedAt =
                Instant.parse(
                        "2026-09-27T10:03:00Z"
                );

        InitialAssessmentResult initialResult =
                new InitialAssessmentResult(
                        List.of(
                                new InitialDimensionResult(
                                        new DimensionCode("XY"),
                                        0,
                                        null,
                                        true
                                )
                        )
                );

        AssessmentSessionResult session =
                new AssessmentSessionResult(
                        UUID.randomUUID(),
                        "TEST_ASSESSMENT",
                        "1.0",
                        AssessmentSessionStatus
                                .AWAITING_CLARIFICATION,
                        List.of(),
                        submittedAt,
                        initialResult,
                        List.of(
                                new AssessmentSessionResult
                                        .DimensionEvidenceResult(
                                        "XY",
                                        "X",
                                        50.0,
                                        "Y",
                                        50.0
                                )
                        ),
                        List.of(
                                new AssessmentSessionResult
                                        .ClarificationStateResult(
                                        "XY",
                                        DimensionClarificationStatus
                                                .CLARIFIED,
                                        new AssessmentSessionResult
                                                .ClarificationResultData(
                                                AssessmentSessionWorkflowSnapshot
                                                        .ClarificationResolution
                                                        .UNCLEAR,
                                                null,
                                                "LOW",
                                                "The evidence remained balanced."
                                        ),
                                        startedAt,
                                        acceptedAt
                                )
                        ),
                        List.of(
                                new AssessmentSessionResult
                                        .TieBreakStateResult(
                                        "XY",
                                        "X",
                                        decidedAt
                                )
                        ),
                        null,
                        new AssessmentSessionResult
                                .WorkflowResult(
                                List.of(),
                                List.of(),
                                List.of(),
                                false
                        ),
                        Instant.parse(
                                "2026-09-27T09:00:00Z"
                        ),
                        null,
                        null
                );

        AssessmentSessionResponse response =
                mapper.toSessionResponse(
                        session
                );

        assertEquals(
                1,
                response.clarifications().size()
        );

        AssessmentSessionResponse.ClarificationStateResponse
                clarification =
                response
                        .clarifications()
                        .getFirst();

        assertEquals(
                "CLARIFIED",
                clarification.status()
        );

        AssessmentSessionResponse
                .UnclearClarificationResultResponse clarificationResult =
                assertInstanceOf(
                        AssessmentSessionResponse
                                .UnclearClarificationResultResponse.class,
                        clarification.result()
                );

        assertEquals(
                "UNCLEAR",
                clarificationResult.resolution()
        );

        assertNull(
                clarificationResult.suggestedPole()
        );

        assertEquals(
                "LOW",
                clarificationResult.confidence()
        );

        assertEquals(
                "The evidence remained balanced.",
                clarificationResult.reasoningSummary()
        );

        assertEquals(
                startedAt,
                clarification.startedAt()
        );

        assertEquals(
                acceptedAt,
                clarification.acceptedAt()
        );

        assertEquals(
                1,
                response.tieBreaks().size()
        );

        AssessmentSessionResponse.LegacyTieBreakStateResponse
                tieBreak =
                assertInstanceOf(
                        AssessmentSessionResponse
                                .LegacyTieBreakStateResponse.class,
                        response
                                .tieBreaks()
                                .getFirst()
                );

        assertEquals(
                "X",
                tieBreak.selectedPole()
        );

        assertEquals(
                decidedAt,
                tieBreak.decidedAt()
        );
    }

    @Test
    void mapsCompletedExactTieResolvedByUserTieBreak() {
        Instant submittedAt =
                Instant.parse(
                        "2026-10-03T10:00:00Z"
                );

        Instant decidedAt =
                Instant.parse(
                        "2026-10-03T10:05:00Z"
                );

        Instant completedAt =
                Instant.parse(
                        "2026-10-03T10:05:01Z"
                );

        InitialAssessmentResult initialResult =
                new InitialAssessmentResult(
                        List.of(
                                new InitialDimensionResult(
                                        new DimensionCode("XY"),
                                        0,
                                        null,
                                        true
                                )
                        )
                );

        FinalAssessmentResult finalResult =
                new FinalAssessmentResult(
                        "X",
                        List.of(
                                new FinalDimensionConclusion(
                                        new DimensionCode("XY"),
                                        new PoleCode("X"),
                                        FinalDecisionSource
                                                .USER_TIE_BREAK
                                )
                        )
                );

        AssessmentSessionResult session =
                new AssessmentSessionResult(
                        UUID.randomUUID(),
                        "TEST_ASSESSMENT",
                        "1.0",
                        AssessmentSessionStatus.COMPLETED,
                        List.of(),
                        submittedAt,
                        initialResult,
                        List.of(
                                new AssessmentSessionResult
                                        .DimensionEvidenceResult(
                                        "XY",
                                        "X",
                                        50.0,
                                        "Y",
                                        50.0
                                )
                        ),
                        List.of(),
                        List.of(
                                new AssessmentSessionResult
                                        .TieBreakStateResult(
                                        "XY",
                                        "X",
                                        decidedAt
                                )
                        ),
                        finalResult,
                        new AssessmentSessionResult
                                .WorkflowResult(
                                List.of(),
                                List.of(),
                                List.of(),
                                true
                        ),
                        Instant.parse(
                                "2026-10-03T09:00:00Z"
                        ),
                        completedAt,
                        null
                );

        AssessmentSessionResponse response =
                mapper.toSessionResponse(
                        session
                );

        AssessmentSessionResponse
                .FinalDimensionConclusionResponse dimension =
                response
                        .finalResult()
                        .dimensions()
                        .getFirst();

        assertNull(
                dimension.questionnairePreference()
        );

        assertEquals(
                "X",
                dimension.finalPreference()
        );

        assertEquals(
                "USER_TIE_BREAK",
                dimension.source()
        );

        assertEquals(
                false,
                dimension.overrodeBaseline()
        );
    }

}
