package dev.springawsportfolio.portfolio.assessment.application.query.session;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetAssessmentSessionServiceTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-27T00:00:00Z"
            );

    private AssessmentSessionRepository
            sessionRepository;

    private AssessmentDefinitionRepository
            definitionRepository;

    private AssessmentDefinitionVersionRepository
            versionRepository;

    private GetAssessmentSessionService service;

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
                new GetAssessmentSessionService(
                        sessionRepository,
                        definitionRepository,
                        versionRepository
                );

        actorUserId =
                new UserId(
                        UUID.randomUUID()
                );
    }

    @Test
    void returnsOwnedSessionUsingExactBoundVersion() {
        AssessmentDefinitionId definitionId =
                AssessmentDefinitionId.newId();

        AssessmentDefinitionVersionId versionId =
                AssessmentDefinitionVersionId.newId();

        AssessmentSession session =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        actorUserId,
                        definitionId,
                        versionId,
                        NOW
                );

        AssessmentDefinition definition =
                mock(
                        AssessmentDefinition.class
                );

        AssessmentDefinitionVersion version =
                mock(
                        AssessmentDefinitionVersion.class
                );

        when(
                sessionRepository.findOwnedById(
                        session.id(),
                        actorUserId
                )
        )
                .thenReturn(
                        Optional.of(session)
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
                versionRepository.findById(
                        versionId
                )
        )
                .thenReturn(
                        Optional.of(version)
                );

        when(
                version.definitionId()
        )
                .thenReturn(
                        definitionId
                );

        when(
                version.versionCode()
        )
                .thenReturn(
                        "1.0"
                );

        var result =
                service.execute(
                        actorUserId,
                        session.id()
                );

        assertEquals(
                session.id().value(),
                result.id()
        );

        assertEquals(
                "1.0",
                result.assessmentVersion()
        );

        verify(
                versionRepository,
                never()
        )
                .findAvailableByDefinitionId(
                        any()
                );
    }

    @Test
    void returnsNotFoundWhenSessionIsNotOwnedByActor() {
        AssessmentSessionId sessionId =
                AssessmentSessionId.newId();

        when(
                sessionRepository.findOwnedById(
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
                                actorUserId,
                                sessionId
                        )
        );

        verify(
                definitionRepository,
                never()
        )
                .findById(
                        any()
                );

        verify(
                versionRepository,
                never()
        )
                .findById(
                        any()
                );
    }
}