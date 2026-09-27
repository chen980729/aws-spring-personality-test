package dev.springawsportfolio.portfolio.assessment.application.command.start;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.port.out.AssessmentSessionWorkflowQuery;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionWorkflowSnapshot;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.AmbiguityPolicy;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.AmbiguityPolicyType;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.AnswerOption;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.AssessmentSpecification;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.ClarificationPolicy;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.ClarificationPolicyType;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.FinalizationPolicy;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.FinalizationPolicyType;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionnaireDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.ScoringPolicy;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.ScoringPolicyType;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StartAssessmentServiceTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-25T08:00:00Z"
            );

    private static final String ASSESSMENT_CODE =
            "SIXTEEN_PERSONALITY";

    private AssessmentDefinitionRepository
            definitionRepository;

    private AssessmentDefinitionVersionRepository
            versionRepository;

    private AssessmentSessionRepository
            sessionRepository;

    private AssessmentSessionWorkflowQuery
            workflowQuery;

    private StartAssessmentService service;

    private UserId actorUserId;

    @BeforeEach
    void setUp() {
        definitionRepository =
                mock(
                        AssessmentDefinitionRepository.class
                );

        versionRepository =
                mock(
                        AssessmentDefinitionVersionRepository.class
                );

        sessionRepository =
                mock(
                        AssessmentSessionRepository.class
                );

        workflowQuery =
                mock(
                        AssessmentSessionWorkflowQuery.class
                );

        when(
                workflowQuery.findBySessionId(
                        any()
                )
        )
                .thenReturn(
                        AssessmentSessionWorkflowSnapshot.empty()
                );

        Clock clock =
                Clock.fixed(
                        NOW,
                        ZoneOffset.UTC
                );

        service =
                new StartAssessmentService(
                        definitionRepository,
                        versionRepository,
                        sessionRepository,
                        workflowQuery,
                        clock
                );

        actorUserId =
                new UserId(
                        UUID.randomUUID()
                );
    }

    @Test
    void createsNewSessionWhenNoActiveSessionExists() {
        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                version(
                        definition.id(),
                        "1.0"
                );

        when(
                definitionRepository.findByCode(
                        ASSESSMENT_CODE
                )
        )
                .thenReturn(
                        Optional.of(definition)
                );

        when(
                sessionRepository.findActive(
                        actorUserId,
                        definition.id()
                )
        )
                .thenReturn(
                        Optional.empty()
                );

        when(
                versionRepository
                        .findAvailableByDefinitionId(
                                definition.id()
                        )
        )
                .thenReturn(
                        Optional.of(version)
                );

        when(
                sessionRepository.tryCreateActive(
                        any(AssessmentSession.class)
                )
        )
                .thenReturn(true);

        StartAssessmentResult result =
                service.execute(
                        actorUserId,
                        ASSESSMENT_CODE
                );

        assertTrue(
                result.created()
        );

        assertEquals(
                ASSESSMENT_CODE,
                result.session().assessmentCode()
        );

        assertEquals(
                "1.0",
                result.session().assessmentVersion()
        );

        assertEquals(
                AssessmentSessionStatus.IN_PROGRESS,
                result.session().status()
        );

        assertEquals(
                NOW,
                result.session().createdAt()
        );

        ArgumentCaptor<AssessmentSession> captor =
                ArgumentCaptor.forClass(
                        AssessmentSession.class
                );

        verify(sessionRepository)
                .tryCreateActive(
                        captor.capture()
                );

        AssessmentSession created =
                captor.getValue();

        assertEquals(
                actorUserId,
                created.ownerUserId()
        );

        assertEquals(
                definition.id(),
                created.definitionId()
        );

        assertEquals(
                version.id(),
                created.definitionVersionId()
        );

        assertEquals(
                NOW,
                created.createdAt()
        );
    }

    @Test
    void resumesExistingSessionWithoutResolvingCurrentAvailableVersion() {
        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion oldVersion =
                version(
                        definition.id(),
                        "1.0"
                );

        AssessmentSession existing =
                AssessmentSession.start(
                        dev.springawsportfolio.portfolio
                                .assessment.domain.session
                                .AssessmentSessionId.newId(),
                        actorUserId,
                        definition.id(),
                        oldVersion.id(),
                        NOW.minusSeconds(3600)
                );

        when(
                definitionRepository.findByCode(
                        ASSESSMENT_CODE
                )
        )
                .thenReturn(
                        Optional.of(definition)
                );

        when(
                sessionRepository.findActive(
                        actorUserId,
                        definition.id()
                )
        )
                .thenReturn(
                        Optional.of(existing)
                );

        when(
                versionRepository.findById(
                        oldVersion.id()
                )
        )
                .thenReturn(
                        Optional.of(oldVersion)
                );

        StartAssessmentResult result =
                service.execute(
                        actorUserId,
                        ASSESSMENT_CODE
                );

        assertFalse(
                result.created()
        );

        assertEquals(
                existing.id().value(),
                result.session().id()
        );

        assertEquals(
                "1.0",
                result.session().assessmentVersion()
        );

        assertEquals(
                existing.createdAt(),
                result.session().createdAt()
        );

        verify(
                versionRepository,
                never()
        )
                .findAvailableByDefinitionId(
                        any()
                );

        verify(
                sessionRepository,
                never()
        )
                .tryCreateActive(
                        any()
                );
    }

    @Test
    void reloadsConcurrentWinnerWhenCandidateCreationLosesRace() {
        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion currentlyAvailable =
                version(
                        definition.id(),
                        "2.0"
                );

        /*
         * Deliberately use a different bound version
         * for the winner.
         *
         * This proves that after losing the race we
         * respect the winner's bound version instead
         * of reusing our current AVAILABLE version.
         */
        AssessmentDefinitionVersion winnerVersion =
                version(
                        definition.id(),
                        "1.0"
                );

        AssessmentSession winner =
                AssessmentSession.start(
                        dev.springawsportfolio.portfolio
                                .assessment.domain.session
                                .AssessmentSessionId.newId(),
                        actorUserId,
                        definition.id(),
                        winnerVersion.id(),
                        NOW.minusSeconds(30)
                );

        when(
                definitionRepository.findByCode(
                        ASSESSMENT_CODE
                )
        )
                .thenReturn(
                        Optional.of(definition)
                );

        when(
                sessionRepository.findActive(
                        actorUserId,
                        definition.id()
                )
        )
                .thenReturn(
                        Optional.empty(),
                        Optional.of(winner)
                );

        when(
                versionRepository
                        .findAvailableByDefinitionId(
                                definition.id()
                        )
        )
                .thenReturn(
                        Optional.of(
                                currentlyAvailable
                        )
                );

        when(
                sessionRepository.tryCreateActive(
                        any(AssessmentSession.class)
                )
        )
                .thenReturn(false);

        when(
                versionRepository.findById(
                        winnerVersion.id()
                )
        )
                .thenReturn(
                        Optional.of(winnerVersion)
                );

        StartAssessmentResult result =
                service.execute(
                        actorUserId,
                        ASSESSMENT_CODE
                );

        assertFalse(
                result.created()
        );

        assertEquals(
                winner.id().value(),
                result.session().id()
        );

        assertEquals(
                "1.0",
                result.session().assessmentVersion()
        );

        assertEquals(
                winner.createdAt(),
                result.session().createdAt()
        );
    }

    @Test
    void throwsWhenAssessmentDoesNotExist() {
        when(
                definitionRepository.findByCode(
                        "UNKNOWN"
                )
        )
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                AssessmentNotFoundException.class,
                () ->
                        service.execute(
                                actorUserId,
                                "UNKNOWN"
                        )
        );

        verify(
                sessionRepository,
                never()
        )
                .findActive(
                        any(),
                        any()
                );
    }

    @Test
    void throwsWhenNoAvailableVersionExistsAndNoSessionCanBeResumed() {
        AssessmentDefinition definition =
                definition();

        when(
                definitionRepository.findByCode(
                        ASSESSMENT_CODE
                )
        )
                .thenReturn(
                        Optional.of(definition)
                );

        when(
                sessionRepository.findActive(
                        actorUserId,
                        definition.id()
                )
        )
                .thenReturn(
                        Optional.empty()
                );

        when(
                versionRepository
                        .findAvailableByDefinitionId(
                                definition.id()
                        )
        )
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                AssessmentNotFoundException.class,
                () ->
                        service.execute(
                                actorUserId,
                                ASSESSMENT_CODE
                        )
        );

        verify(
                sessionRepository,
                never()
        )
                .tryCreateActive(
                        any()
                );
    }

    private AssessmentDefinition definition() {
        return AssessmentDefinition.restore(
                AssessmentDefinitionId.newId(),
                ASSESSMENT_CODE,
                "Personality Type Explorer",
                NOW.minusSeconds(86400)
        );
    }

    private AssessmentDefinitionVersion version(
            AssessmentDefinitionId definitionId,
            String versionCode
    ) {
        return AssessmentDefinitionVersion.restore(
                AssessmentDefinitionVersionId.newId(),
                definitionId,
                versionCode,
                AssessmentDefinitionVersionStatus.AVAILABLE,
                specification(),
                NOW.minusSeconds(86400),
                NOW.minusSeconds(86400)
        );
    }

    private AssessmentSpecification specification() {
        DimensionCode dimension =
                new DimensionCode("XY");

        PoleCode poleX =
                new PoleCode("X");

        PoleCode poleY =
                new PoleCode("Y");

        return new AssessmentSpecification(
                List.of(
                        new DimensionDefinition(
                                dimension,
                                1,
                                poleX,
                                poleY
                        )
                ),
                new QuestionnaireDefinition(
                        List.of(
                                new AnswerOption(
                                        1,
                                        "Strongly Disagree"
                                ),
                                new AnswerOption(
                                        2,
                                        "Disagree"
                                ),
                                new AnswerOption(
                                        3,
                                        "Neutral"
                                ),
                                new AnswerOption(
                                        4,
                                        "Agree"
                                ),
                                new AnswerOption(
                                        5,
                                        "Strongly Agree"
                                )
                        ),
                        List.of(
                                new QuestionDefinition(
                                        new QuestionId("Q1"),
                                        1,
                                        "Question 1",
                                        dimension,
                                        poleX
                                ),
                                new QuestionDefinition(
                                        new QuestionId("Q2"),
                                        2,
                                        "Question 2",
                                        dimension,
                                        poleY
                                )
                        )
                ),
                new ScoringPolicy(
                        ScoringPolicyType
                                .CENTERED_BALANCED_KEYING,
                        "v1",
                        3,
                        1,
                        5,
                        2
                ),
                new AmbiguityPolicy(
                        AmbiguityPolicyType
                                .ABSOLUTE_RAW_SCORE_THRESHOLD,
                        "v1",
                        1
                ),
                new ClarificationPolicy(
                        ClarificationPolicyType
                                .DIMENSION_SCOPED_AI_CLARIFICATION,
                        "v1"
                ),
                new FinalizationPolicy(
                        FinalizationPolicyType
                                .QUESTIONNAIRE_WITH_OPTIONAL_CLARIFICATION,
                        "v1"
                )
        );
    }
}