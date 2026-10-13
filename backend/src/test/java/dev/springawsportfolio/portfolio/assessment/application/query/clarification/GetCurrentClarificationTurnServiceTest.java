package dev.springawsportfolio.portfolio.assessment.application.query.clarification;

import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeContext;
import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeMessage;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.ClarificationNotAllowedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.ClarificationTurnConflictException;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationRuntimeContextStore;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationExecutionToken;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionClarificationRepository;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GetCurrentClarificationTurnServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-13T09:00:00Z");

    private final AssessmentSessionRepository sessionRepository =
            mock(AssessmentSessionRepository.class);
    private final AssessmentDefinitionVersionRepository versionRepository =
            mock(AssessmentDefinitionVersionRepository.class);
    private final DimensionClarificationRepository clarificationRepository =
            mock(DimensionClarificationRepository.class);
    private final ClarificationRuntimeContextStore contextStore =
            mock(ClarificationRuntimeContextStore.class);

    private final GetCurrentClarificationTurnService service =
            new GetCurrentClarificationTurnService(
                    sessionRepository,
                    versionRepository,
                    clarificationRepository,
                    contextStore,
                    Clock.fixed(NOW, ZoneOffset.UTC)
            );

    private final UserId actor = new UserId(UUID.randomUUID());
    private final AssessmentSessionId sessionId = AssessmentSessionId.newId();
    private final DimensionCode dimension = new DimensionCode("EI");
    private final ClarificationExecutionToken token = ClarificationExecutionToken.newToken();

    @BeforeEach
    void givenActiveOwnedClarification() {
        AssessmentSession session = mock(AssessmentSession.class);
        when(session.status()).thenReturn(AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS);

        AssessmentDefinitionVersion version = mock(
                AssessmentDefinitionVersion.class, RETURNS_DEEP_STUBS
        );
        DimensionDefinition boundDimension = mock(DimensionDefinition.class);
        when(boundDimension.code()).thenReturn(dimension);
        when(version.specification().dimensions()).thenReturn(List.of(boundDimension));

        when(sessionRepository.findOwnedById(sessionId, actor))
                .thenReturn(Optional.of(session));
        when(versionRepository.findById(any()))
                .thenReturn(Optional.of(version));

        DimensionClarification clarification = mock(DimensionClarification.class);
        when(clarification.status()).thenReturn(DimensionClarificationStatus.IN_PROGRESS);
        when(clarification.activeExecutionToken()).thenReturn(token);
        when(clarificationRepository.findBySessionIdAndDimension(sessionId, dimension))
                .thenReturn(Optional.of(clarification));
    }

    @Test
    void returnsOnlyCurrentQuestionAfterCheckingOwnership() {
        when(contextStore.loadActive(sessionId, dimension, token))
                .thenReturn(context(NOW.plusSeconds(1800), true));

        CurrentClarificationTurnResult result = service.execute(actor, sessionId, dimension);

        assertEquals(sessionId.value(), result.sessionId());
        assertEquals("EI", result.dimensionCode());
        assertEquals(3, result.assistantSequence());
        assertEquals("Second question", result.question());
        verify(sessionRepository).findOwnedById(sessionId, actor);
    }

    @Test
    void rejectsUnknownOrForeignSessionWithoutReadingRuntimeContext() {
        when(sessionRepository.findOwnedById(sessionId, actor))
                .thenReturn(Optional.empty());

        assertThrows(AssessmentSessionNotFoundException.class,
                () -> service.execute(actor, sessionId, dimension));
        verifyNoInteractions(contextStore);
    }

    @Test
    void rejectsExpiredQuestionAsConflict() {
        when(contextStore.loadActive(sessionId, dimension, token))
                .thenReturn(context(NOW.minusSeconds(1), true));

        assertThrows(ClarificationTurnConflictException.class,
                () -> service.execute(actor, sessionId, dimension));
    }

    @Test
    void rejectsAnswerStillProcessingAsConflict() {
        when(contextStore.loadActive(sessionId, dimension, token))
                .thenReturn(context(NOW.plusSeconds(1800), false));

        assertThrows(ClarificationTurnConflictException.class,
                () -> service.execute(actor, sessionId, dimension));
    }

    @Test
    void rejectsInactiveClarificationWithoutLoadingTranscript() {
        AssessmentSession session = sessionRepository
                .findOwnedById(sessionId, actor).orElseThrow();
        when(session.status()).thenReturn(AssessmentSessionStatus.COMPLETED);

        assertThrows(ClarificationNotAllowedException.class,
                () -> service.execute(actor, sessionId, dimension));
        verify(contextStore, never()).loadActive(any(), any(), any());
    }

    private ClarificationRuntimeContext context(Instant expiry, boolean questionLast) {
        ClarificationRuntimeMessage first = new ClarificationRuntimeMessage(
                1, ClarificationRuntimeMessage.Role.ASSISTANT_QUESTION,
                "First question", NOW.minusSeconds(600), expiry
        );
        ClarificationRuntimeMessage answer = new ClarificationRuntimeMessage(
                2, ClarificationRuntimeMessage.Role.USER_ANSWER,
                "First answer", NOW.minusSeconds(500), expiry
        );
        if (!questionLast) {
            return new ClarificationRuntimeContext(List.of(first, answer));
        }
        ClarificationRuntimeMessage second = new ClarificationRuntimeMessage(
                3, ClarificationRuntimeMessage.Role.ASSISTANT_QUESTION,
                "Second question", NOW.minusSeconds(400), expiry
        );
        return new ClarificationRuntimeContext(List.of(first, answer, second));
    }
}
