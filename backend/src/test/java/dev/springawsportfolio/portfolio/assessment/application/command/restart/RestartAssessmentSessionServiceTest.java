package dev.springawsportfolio.portfolio.assessment.application.command.restart;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionAlreadyAbandonedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionAlreadyCompletedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDecisionSource;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDimensionConclusion;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.QuestionnaireResponse;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RestartAssessmentSessionServiceTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-27T06:00:00Z"
            );

    private static final Instant CREATED_AT =
            NOW.minusSeconds(300);

    private AssessmentSessionRepository
            sessionRepository;

    private AssessmentDefinitionRepository
            definitionRepository;

    private AssessmentDefinitionVersionRepository
            versionRepository;

    private RestartAssessmentSessionService service;

    private UserId actorUserId;

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

        service =
                new RestartAssessmentSessionService(
                        sessionRepository,
                        definitionRepository,
                        versionRepository,
                        Clock.fixed(
                                NOW,
                                ZoneOffset.UTC
                        )
                );

        actorUserId =
                new UserId(
                        UUID.randomUUID()
                );
    }

    @Test
    void abandonsOldSessionAndCreatesReplacementUsingCurrentAvailableVersion() {
        AssessmentDefinitionId definitionId =
                AssessmentDefinitionId.newId();

        AssessmentDefinitionVersionId oldVersionId =
                AssessmentDefinitionVersionId.newId();

        AssessmentDefinitionVersionId replacementVersionId =
                AssessmentDefinitionVersionId.newId();

        AssessmentSession oldSession =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        actorUserId,
                        definitionId,
                        oldVersionId,
                        CREATED_AT
                );

        AssessmentDefinition definition =
                mock(
                        AssessmentDefinition.class
                );

        AssessmentDefinitionVersion replacementVersion =
                mock(
                        AssessmentDefinitionVersion.class
                );

        when(
                sessionRepository.findOwnedByIdForUpdate(
                        oldSession.id(),
                        actorUserId
                )
        )
                .thenReturn(
                        Optional.of(oldSession)
                );

        when(
                definitionRepository.findById(
                        definitionId
                )
        )
                .thenReturn(
                        Optional.of(definition)
                );

        when(
                definition.code()
        )
                .thenReturn(
                        "TEST_ASSESSMENT"
                );

        when(
                versionRepository.findAvailableByDefinitionId(
                        definitionId
                )
        )
                .thenReturn(
                        Optional.of(replacementVersion)
                );

        when(
                replacementVersion.id()
        )
                .thenReturn(
                        replacementVersionId
                );

        when(
                replacementVersion.definitionId()
        )
                .thenReturn(
                        definitionId
                );

        when(
                replacementVersion.versionCode()
        )
                .thenReturn(
                        "2.0"
                );

        RestartAssessmentSessionResult result =
                service.execute(
                        new RestartAssessmentSessionCommand(
                                actorUserId,
                                oldSession.id()
                        )
                );

        assertEquals(
                AssessmentSessionStatus.ABANDONED,
                oldSession.status()
        );

        assertEquals(
                NOW,
                oldSession.abandonedAt()
        );

        assertEquals(
                oldSession.id().value(),
                result.abandonedSessionId()
        );

        assertEquals(
                AssessmentSessionStatus.IN_PROGRESS,
                result.session().status()
        );

        assertEquals(
                "2.0",
                result.session().assessmentVersion()
        );

        assertTrue(
                result.session()
                        .answers()
                        .isEmpty()
        );

        assertNull(
                result.session()
                        .questionnaireSubmittedAt()
        );

        ArgumentCaptor<AssessmentSession>
                replacementCaptor =
                ArgumentCaptor.forClass(
                        AssessmentSession.class
                );

        verify(
                sessionRepository
        )
                .replaceActive(
                        org.mockito.ArgumentMatchers.same(
                                oldSession
                        ),
                        replacementCaptor.capture()
                );

        AssessmentSession replacement =
                replacementCaptor.getValue();

        assertNotEquals(
                oldSession.id(),
                replacement.id()
        );

        assertEquals(
                actorUserId,
                replacement.ownerUserId()
        );

        assertEquals(
                definitionId,
                replacement.definitionId()
        );

        assertEquals(
                replacementVersionId,
                replacement.definitionVersionId()
        );

        assertEquals(
                NOW,
                replacement.createdAt()
        );
    }

    @Test
    void allowsRestartFromAwaitingClarification() {
        AssessmentDefinitionId definitionId =
                AssessmentDefinitionId.newId();

        AssessmentDefinitionVersionId oldVersionId =
                AssessmentDefinitionVersionId.newId();

        AssessmentDefinitionVersionId replacementVersionId =
                AssessmentDefinitionVersionId.newId();

        DimensionCode dimension =
                new DimensionCode("XY");

        InitialAssessmentResult initialResult =
                new InitialAssessmentResult(
                        List.of(
                                new InitialDimensionResult(
                                        dimension,
                                        0,
                                        null,
                                        true
                                )
                        )
                );

        AssessmentSession oldSession =
                AssessmentSession.restore(
                        AssessmentSessionId.newId(),
                        actorUserId,
                        definitionId,
                        oldVersionId,
                        AssessmentSessionStatus
                                .AWAITING_CLARIFICATION,
                        QuestionnaireResponse.empty(),
                        CREATED_AT.plusSeconds(120),
                        initialResult,
                        null,
                        CREATED_AT,
                        null,
                        null
                );

        prepareRestartDependencies(
                oldSession,
                replacementVersionId
        );

        service.execute(
                new RestartAssessmentSessionCommand(
                        actorUserId,
                        oldSession.id()
                )
        );

        assertEquals(
                AssessmentSessionStatus.ABANDONED,
                oldSession.status()
        );

        assertEquals(
                initialResult,
                oldSession.initialResult()
        );

        assertEquals(
                CREATED_AT.plusSeconds(120),
                oldSession.questionnaireSubmittedAt()
        );

        verify(
                sessionRepository
        )
                .replaceActive(
                        org.mockito.ArgumentMatchers.same(
                                oldSession
                        ),
                        any(
                                AssessmentSession.class
                        )
                );
    }

    @Test
    void retryAgainstAlreadyAbandonedOldSessionIsRejected() {
        AssessmentDefinitionId definitionId =
                AssessmentDefinitionId.newId();

        AssessmentSession oldSession =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        actorUserId,
                        definitionId,
                        AssessmentDefinitionVersionId.newId(),
                        CREATED_AT
                );

        oldSession.abandon(
                NOW.minusSeconds(30)
        );

        when(
                sessionRepository.findOwnedByIdForUpdate(
                        oldSession.id(),
                        actorUserId
                )
        )
                .thenReturn(
                        Optional.of(oldSession)
                );

        assertThrows(
                AssessmentSessionAlreadyAbandonedException.class,
                () ->
                        service.execute(
                                new RestartAssessmentSessionCommand(
                                        actorUserId,
                                        oldSession.id()
                                )
                        )
        );

        verifyNoInteractions(
                definitionRepository
        );

        verifyNoInteractions(
                versionRepository
        );

        verify(
                sessionRepository,
                never()
        )
                .replaceActive(
                        any(),
                        any()
                );
    }

    @Test
    void completedSessionCannotBeRestarted() {
        AssessmentDefinitionId definitionId =
                AssessmentDefinitionId.newId();

        DimensionCode dimension =
                new DimensionCode("XY");

        PoleCode poleX =
                new PoleCode("X");

        InitialAssessmentResult initialResult =
                new InitialAssessmentResult(
                        List.of(
                                new InitialDimensionResult(
                                        dimension,
                                        3,
                                        poleX,
                                        false
                                )
                        )
                );

        FinalAssessmentResult finalResult =
                new FinalAssessmentResult(
                        "X",
                        List.of(
                                new FinalDimensionConclusion(
                                        dimension,
                                        poleX,
                                        FinalDecisionSource.QUESTIONNAIRE
                                )
                        )
                );

        Instant submittedAt =
                CREATED_AT.plusSeconds(120);

        AssessmentSession completed =
                AssessmentSession.restore(
                        AssessmentSessionId.newId(),
                        actorUserId,
                        definitionId,
                        AssessmentDefinitionVersionId.newId(),
                        AssessmentSessionStatus.COMPLETED,
                        QuestionnaireResponse.empty(),
                        submittedAt,
                        initialResult,
                        finalResult,
                        CREATED_AT,
                        submittedAt,
                        null
                );

        when(
                sessionRepository.findOwnedByIdForUpdate(
                        completed.id(),
                        actorUserId
                )
        )
                .thenReturn(
                        Optional.of(completed)
                );

        assertThrows(
                AssessmentSessionAlreadyCompletedException.class,
                () ->
                        service.execute(
                                new RestartAssessmentSessionCommand(
                                        actorUserId,
                                        completed.id()
                                )
                        )
        );

        verifyNoInteractions(
                definitionRepository
        );

        verifyNoInteractions(
                versionRepository
        );

        verify(
                sessionRepository,
                never()
        )
                .replaceActive(
                        any(),
                        any()
                );
    }

    @Test
    void missingOrOtherOwnerSessionReturnsNotFound() {
        AssessmentSessionId sessionId =
                AssessmentSessionId.newId();

        when(
                sessionRepository.findOwnedByIdForUpdate(
                        sessionId,
                        actorUserId
                )
        )
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                AssessmentSessionNotFoundException.class,
                () ->
                        service.execute(
                                new RestartAssessmentSessionCommand(
                                        actorUserId,
                                        sessionId
                                )
                        )
        );

        verifyNoInteractions(
                definitionRepository
        );

        verifyNoInteractions(
                versionRepository
        );
    }

    private void prepareRestartDependencies(
            AssessmentSession oldSession,
            AssessmentDefinitionVersionId replacementVersionId
    ) {
        AssessmentDefinition definition =
                mock(
                        AssessmentDefinition.class
                );

        AssessmentDefinitionVersion replacementVersion =
                mock(
                        AssessmentDefinitionVersion.class
                );

        when(
                sessionRepository.findOwnedByIdForUpdate(
                        oldSession.id(),
                        actorUserId
                )
        )
                .thenReturn(
                        Optional.of(oldSession)
                );

        when(
                definitionRepository.findById(
                        oldSession.definitionId()
                )
        )
                .thenReturn(
                        Optional.of(definition)
                );

        when(
                definition.code()
        )
                .thenReturn(
                        "TEST_ASSESSMENT"
                );

        when(
                versionRepository.findAvailableByDefinitionId(
                        oldSession.definitionId()
                )
        )
                .thenReturn(
                        Optional.of(replacementVersion)
                );

        when(
                replacementVersion.id()
        )
                .thenReturn(
                        replacementVersionId
                );

        when(
                replacementVersion.definitionId()
        )
                .thenReturn(
                        oldSession.definitionId()
                );

        when(
                replacementVersion.versionCode()
        )
                .thenReturn(
                        "2.0"
                );
    }
}
