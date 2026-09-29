package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.domain.clarification.AIProvenance;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationConfidence;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationExecutionToken;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResolution;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResult;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationId;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.AssessmentSpecification;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.ScoringPolicy;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionClarificationRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionTieBreakRepository;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.QuestionnaireResponse;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClarificationExecutionServicesTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-29T00:00:00Z"
            );

    private static final DimensionCode DIMENSION =
            new DimensionCode("EI");

    private static final PoleCode POLE_E =
            new PoleCode("E");

    private static final PoleCode POLE_I =
            new PoleCode("I");

    private AssessmentSessionRepository
            sessionRepository;

    private AssessmentDefinitionRepository
            definitionRepository;

    private AssessmentDefinitionVersionRepository
            versionRepository;

    private DimensionClarificationRepository
            clarificationRepository;

    private DimensionTieBreakRepository
            tieBreakRepository;

    private StartDimensionClarificationService
            startService;

    private RetryDimensionClarificationService
            retryService;

    private CompleteClarificationExecutionService
            completeService;

    private UserId actorUserId;

    private AssessmentDefinitionId definitionId;

    private AssessmentDefinitionVersionId versionId;

    private AssessmentDefinitionVersion version;

    @BeforeEach
    void setUp() {
        sessionRepository =
                mock(
                        AssessmentSessionRepository.class
                );

        definitionRepository =
                mock(
                        AssessmentDefinitionRepository.class
                );

        versionRepository =
                mock(
                        AssessmentDefinitionVersionRepository.class
                );

        clarificationRepository =
                mock(
                        DimensionClarificationRepository.class
                );

        tieBreakRepository =
                mock(
                        DimensionTieBreakRepository.class
                );

        Clock clock =
                Clock.fixed(
                        NOW,
                        ZoneOffset.UTC
                );

        startService =
                new StartDimensionClarificationService(
                        sessionRepository,
                        versionRepository,
                        clarificationRepository,
                        clock
                );

        retryService =
                new RetryDimensionClarificationService(
                        sessionRepository,
                        versionRepository,
                        clarificationRepository,
                        clock
                );

        completeService =
                new CompleteClarificationExecutionService(
                        sessionRepository,
                        definitionRepository,
                        versionRepository,
                        clarificationRepository,
                        tieBreakRepository,
                        clock
                );

        actorUserId =
                new UserId(
                        UUID.randomUUID()
                );

        definitionId =
                AssessmentDefinitionId.newId();

        versionId =
                AssessmentDefinitionVersionId.newId();

        version =
                version();
    }

    @Test
    void startsPendingClarificationAndReturnsExecutionTicket() {
        AssessmentSession session =
                awaitingSession();

        DimensionClarification clarification =
                pendingClarification(
                        session.id()
                );

        prepareSessionAndVersion(
                session
        );

        when(
                clarificationRepository.findBySessionId(
                        session.id()
                )
        )
                .thenReturn(
                        List.of(clarification)
                );

        when(
                clarificationRepository.findBySessionIdAndDimensionForUpdate(
                        session.id(),
                        DIMENSION
                )
        )
                .thenReturn(
                        Optional.of(clarification)
                );

        ClarificationExecutionTicket ticket =
                startService.execute(
                        new StartDimensionClarificationCommand(
                                actorUserId,
                                session.id(),
                                DIMENSION
                        )
                );

        assertEquals(
                AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS,
                session.status()
        );

        assertEquals(
                DimensionClarificationStatus.IN_PROGRESS,
                clarification.status()
        );

        assertNotNull(
                clarification.activeExecutionToken()
        );

        assertEquals(
                clarification.activeExecutionToken(),
                ticket.executionToken()
        );

        verify(
                clarificationRepository
        )
                .update(
                        same(clarification)
                );

        verify(
                sessionRepository
        )
                .update(
                        same(session)
                );
    }

    @Test
    void retryCreatesNewExecutionTokenForSameClarification() {
        AssessmentSession session =
                awaitingSession();

        DimensionClarification clarification =
                pendingClarification(
                        session.id()
                );

        ClarificationExecutionToken firstToken =
                clarification.start(
                        NOW.minusSeconds(30)
                );

        clarification.failRetryable(
                firstToken,
                NOW.minusSeconds(20)
        );

        prepareSessionAndVersion(
                session
        );

        when(
                clarificationRepository.findBySessionId(
                        session.id()
                )
        )
                .thenReturn(
                        List.of(clarification)
                );

        when(
                clarificationRepository.findBySessionIdAndDimensionForUpdate(
                        session.id(),
                        DIMENSION
                )
        )
                .thenReturn(
                        Optional.of(clarification)
                );

        ClarificationExecutionTicket retryTicket =
                retryService.execute(
                        new RetryDimensionClarificationCommand(
                                actorUserId,
                                session.id(),
                                DIMENSION
                        )
                );

        assertNotEquals(
                firstToken,
                retryTicket.executionToken()
        );

        assertEquals(
                retryTicket.executionToken(),
                clarification.activeExecutionToken()
        );

        assertEquals(
                AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS,
                session.status()
        );
    }

    @Test
    void acceptedResolvedResultCanFinalizeAssessment() {
        AssessmentSession session =
                awaitingSession();

        DimensionClarification clarification =
                pendingClarification(
                        session.id()
                );

        ClarificationExecutionToken token =
                clarification.start(
                        NOW.minusSeconds(10)
                );

        session.beginClarification();

        ClarificationExecutionTicket ticket =
                new ClarificationExecutionTicket(
                        actorUserId,
                        session.id(),
                        DIMENSION,
                        token
                );

        prepareCompletionDependencies(
                session,
                clarification
        );

        ClarificationExecutionCompletionResult completion =
                completeService.accept(
                        ticket,
                        new ClarificationResult(
                                ClarificationResolution.RESOLVED,
                                POLE_I,
                                ClarificationConfidence.HIGH,
                                "Accepted clarification result."
                        ),
                        provenance()
                );

        assertTrue(
                completion.accepted()
        );

        assertEquals(
                AssessmentSessionStatus.COMPLETED,
                session.status()
        );

        assertEquals(
                DimensionClarificationStatus.CLARIFIED,
                clarification.status()
        );

        assertNull(
                clarification.activeExecutionToken()
        );

        assertEquals(
                "I",
                completion
                        .session()
                        .finalResult()
                        .finalType()
        );
    }

    @Test
    void staleExecutionTokenIsDiscardedWithoutMutation() {
        AssessmentSession session =
                awaitingSession();

        DimensionClarification clarification =
                pendingClarification(
                        session.id()
                );

        ClarificationExecutionToken activeToken =
                clarification.start(
                        NOW.minusSeconds(10)
                );

        session.beginClarification();

        prepareSessionAndVersion(
                session
        );

        when(
                clarificationRepository.findBySessionIdAndDimensionForUpdate(
                        session.id(),
                        DIMENSION
                )
        )
                .thenReturn(
                        Optional.of(clarification)
                );

        ClarificationExecutionCompletionResult completion =
                completeService.accept(
                        new ClarificationExecutionTicket(
                                actorUserId,
                                session.id(),
                                DIMENSION,
                                ClarificationExecutionToken.newToken()
                        ),
                        new ClarificationResult(
                                ClarificationResolution.RESOLVED,
                                POLE_I,
                                ClarificationConfidence.HIGH,
                                null
                        ),
                        provenance()
                );

        assertFalse(
                completion.accepted()
        );

        assertEquals(
                ClarificationExecutionCompletionStatus.STALE_DISCARDED,
                completion.status()
        );

        assertEquals(
                AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS,
                session.status()
        );

        assertEquals(
                activeToken,
                clarification.activeExecutionToken()
        );

        verify(
                clarificationRepository,
                never()
        )
                .update(
                        same(clarification)
                );
    }

    @Test
    void technicalFailureReturnsSessionToAwaitingAndMarksRetryable() {
        AssessmentSession session =
                awaitingSession();

        DimensionClarification clarification =
                pendingClarification(
                        session.id()
                );

        ClarificationExecutionToken token =
                clarification.start(
                        NOW.minusSeconds(10)
                );

        session.beginClarification();

        prepareCompletionDependencies(
                session,
                clarification
        );

        ClarificationExecutionCompletionResult completion =
                completeService.failRetryable(
                        new ClarificationExecutionTicket(
                                actorUserId,
                                session.id(),
                                DIMENSION,
                                token
                        )
                );

        assertTrue(
                completion.accepted()
        );

        assertEquals(
                AssessmentSessionStatus.AWAITING_CLARIFICATION,
                session.status()
        );

        assertEquals(
                DimensionClarificationStatus.FAILED_RETRYABLE,
                clarification.status()
        );

        assertNull(
                clarification.activeExecutionToken()
        );

        assertTrue(
                completion
                        .session()
                        .workflow()
                        .retryableClarificationDimensions()
                        .contains(
                                DIMENSION.value()
                        )
        );
    }

    private AssessmentSession awaitingSession() {
        return AssessmentSession.restore(
                AssessmentSessionId.newId(),
                actorUserId,
                definitionId,
                versionId,
                AssessmentSessionStatus.AWAITING_CLARIFICATION,
                QuestionnaireResponse.empty(),
                NOW.minusSeconds(100),
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
                NOW.minusSeconds(200),
                null,
                null
        );
    }

    private DimensionClarification pendingClarification(
            AssessmentSessionId sessionId
    ) {
        return DimensionClarification.pending(
                DimensionClarificationId.newId(),
                sessionId,
                DIMENSION,
                NOW.minusSeconds(90)
        );
    }

    private void prepareSessionAndVersion(
            AssessmentSession session
    ) {
        when(
                sessionRepository.findOwnedByIdForUpdate(
                        session.id(),
                        actorUserId
                )
        )
                .thenReturn(
                        Optional.of(session)
                );

        when(
                versionRepository.findById(
                        versionId
                )
        )
                .thenReturn(
                        Optional.of(version)
                );
    }

    private void prepareCompletionDependencies(
            AssessmentSession session,
            DimensionClarification clarification
    ) {
        prepareSessionAndVersion(
                session
        );

        when(
                clarificationRepository.findBySessionIdAndDimensionForUpdate(
                        session.id(),
                        DIMENSION
                )
        )
                .thenReturn(
                        Optional.of(clarification)
                );

        when(
                clarificationRepository.findBySessionId(
                        session.id()
                )
        )
                .thenReturn(
                        List.of(clarification)
                );

        when(
                tieBreakRepository.findBySessionId(
                        session.id()
                )
        )
                .thenReturn(
                        List.of()
                );

        AssessmentDefinition definition =
                mock(
                        AssessmentDefinition.class
                );

        when(
                definition.code()
        )
                .thenReturn(
                        "TEST_ASSESSMENT"
                );

        when(
                definitionRepository.findById(
                        definitionId
                )
        )
                .thenReturn(
                        Optional.of(definition)
                );
    }

    private AssessmentDefinitionVersion version() {
        AssessmentDefinitionVersion result =
                mock(
                        AssessmentDefinitionVersion.class
                );

        AssessmentSpecification specification =
                mock(
                        AssessmentSpecification.class
                );

        ScoringPolicy scoringPolicy =
                mock(
                        ScoringPolicy.class
                );

        when(
                result.id()
        )
                .thenReturn(
                        versionId
                );

        when(
                result.definitionId()
        )
                .thenReturn(
                        definitionId
                );

        when(
                result.versionCode()
        )
                .thenReturn(
                        "1.0"
                );

        when(
                result.specification()
        )
                .thenReturn(
                        specification
                );

        when(
                specification.dimensions()
        )
                .thenReturn(
                        List.of(
                                new DimensionDefinition(
                                        DIMENSION,
                                        1,
                                        POLE_E,
                                        POLE_I
                                )
                        )
                );

        when(
                specification.scoringPolicy()
        )
                .thenReturn(
                        scoringPolicy
                );

        when(
                scoringPolicy.maximumAbsoluteRawScore()
        )
                .thenReturn(
                        24
                );

        return result;
    }

    private AIProvenance provenance() {
        return new AIProvenance(
                "test-provider",
                "test-model",
                "clarification-v1"
        );
    }
}
