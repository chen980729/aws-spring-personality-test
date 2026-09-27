package dev.springawsportfolio.portfolio.assessment.application.command.questionnaire;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentAlreadySubmittedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionAlreadyAbandonedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.InvalidQuestionnaireResponseException;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
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
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.Answer;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.QuestionnaireResponse;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaveQuestionnaireProgressServiceTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-26T00:00:00Z"
            );

    private AssessmentSessionRepository
            sessionRepository;

    private AssessmentDefinitionRepository
            definitionRepository;

    private AssessmentDefinitionVersionRepository
            versionRepository;

    private SaveQuestionnaireProgressService service;

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
                new SaveQuestionnaireProgressService(
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
    void replacesEntireDraftSnapshotUsingBoundVersion() {
        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                version(
                        definition.id()
                );

        AssessmentSession session =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        actorUserId,
                        definition.id(),
                        version.id(),
                        NOW
                );

        session.replaceQuestionnaireResponse(
                new QuestionnaireResponse(
                        List.of(
                                new Answer(
                                        new QuestionId("Q1"),
                                        5
                                ),
                                new Answer(
                                        new QuestionId("Q2"),
                                        3
                                )
                        )
                )
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
                versionRepository.findById(
                        version.id()
                )
        )
                .thenReturn(
                        Optional.of(version)
                );

        when(
                definitionRepository.findById(
                        definition.id()
                )
        )
                .thenReturn(
                        Optional.of(definition)
                );

        SaveQuestionnaireProgressCommand command =
                new SaveQuestionnaireProgressCommand(
                        actorUserId,
                        session.id(),
                        List.of(
                                new SaveQuestionnaireProgressCommand
                                        .AnswerInput(
                                        "Q2",
                                        1
                                )
                        )
                );

        AssessmentSessionResult result =
                service.execute(
                        command
                );

        assertEquals(
                1,
                session
                        .questionnaireResponse()
                        .answers()
                        .size()
        );

        assertEquals(
                "Q2",
                session
                        .questionnaireResponse()
                        .answers()
                        .getFirst()
                        .questionId()
                        .value()
        );

        assertEquals(
                1,
                session
                        .questionnaireResponse()
                        .answers()
                        .getFirst()
                        .value()
        );

        assertEquals(
                1,
                result.answers().size()
        );

        assertEquals(
                "1.0",
                result.assessmentVersion()
        );

        verify(
                sessionRepository
        )
                .update(
                        session
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
    void normalizesAnswerOrderToQuestionnairePosition() {
        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                version(
                        definition.id()
                );

        AssessmentSession session =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        actorUserId,
                        definition.id(),
                        version.id(),
                        NOW
                );

        prepareValidSession(
                definition,
                version,
                session
        );

        SaveQuestionnaireProgressCommand command =
                new SaveQuestionnaireProgressCommand(
                        actorUserId,
                        session.id(),
                        List.of(
                                new SaveQuestionnaireProgressCommand
                                        .AnswerInput(
                                        "Q2",
                                        4
                                ),
                                new SaveQuestionnaireProgressCommand
                                        .AnswerInput(
                                        "Q1",
                                        5
                                )
                        )
                );

        service.execute(
                command
        );

        assertEquals(
                "Q1",
                session
                        .questionnaireResponse()
                        .answers()
                        .get(0)
                        .questionId()
                        .value()
        );

        assertEquals(
                "Q2",
                session
                        .questionnaireResponse()
                        .answers()
                        .get(1)
                        .questionId()
                        .value()
        );
    }

    @Test
    void rejectsUnknownQuestion() {
        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                version(
                        definition.id()
                );

        AssessmentSession session =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        actorUserId,
                        definition.id(),
                        version.id(),
                        NOW
                );

        prepareValidSession(
                definition,
                version,
                session
        );

        SaveQuestionnaireProgressCommand command =
                new SaveQuestionnaireProgressCommand(
                        actorUserId,
                        session.id(),
                        List.of(
                                new SaveQuestionnaireProgressCommand
                                        .AnswerInput(
                                        "UNKNOWN",
                                        5
                                )
                        )
                );

        assertThrows(
                InvalidQuestionnaireResponseException.class,
                () ->
                        service.execute(
                                command
                        )
        );

        verify(
                sessionRepository,
                never()
        )
                .update(
                        any()
                );
    }

    @Test
    void rejectsAnswerValueOutsideBoundDefinitionScale() {
        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                version(
                        definition.id()
                );

        AssessmentSession session =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        actorUserId,
                        definition.id(),
                        version.id(),
                        NOW
                );

        prepareValidSession(
                definition,
                version,
                session
        );

        SaveQuestionnaireProgressCommand command =
                new SaveQuestionnaireProgressCommand(
                        actorUserId,
                        session.id(),
                        List.of(
                                new SaveQuestionnaireProgressCommand
                                        .AnswerInput(
                                        "Q1",
                                        99
                                )
                        )
                );

        assertThrows(
                InvalidQuestionnaireResponseException.class,
                () ->
                        service.execute(
                                command
                        )
        );

        verify(
                sessionRepository,
                never()
        )
                .update(
                        any()
                );
    }

    @Test
    void rejectsDuplicateQuestionAnswers() {
        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                version(
                        definition.id()
                );

        AssessmentSession session =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        actorUserId,
                        definition.id(),
                        version.id(),
                        NOW
                );

        prepareValidSession(
                definition,
                version,
                session
        );

        SaveQuestionnaireProgressCommand command =
                new SaveQuestionnaireProgressCommand(
                        actorUserId,
                        session.id(),
                        List.of(
                                new SaveQuestionnaireProgressCommand
                                        .AnswerInput(
                                        "Q1",
                                        5
                                ),
                                new SaveQuestionnaireProgressCommand
                                        .AnswerInput(
                                        "Q1",
                                        3
                                )
                        )
                );

        assertThrows(
                InvalidQuestionnaireResponseException.class,
                () ->
                        service.execute(
                                command
                        )
        );

        verify(
                sessionRepository,
                never()
        )
                .update(
                        any()
                );
    }

    @Test
    void rejectsSavingAlreadySubmittedSession() {
        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                version(
                        definition.id()
                );

        AssessmentSession session =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        actorUserId,
                        definition.id(),
                        version.id(),
                        NOW
                );

        session.submit(
                completeQuestionnaireResponse(),
                ambiguousInitialResult(),
                null,
                NOW.plusSeconds(30)
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

        SaveQuestionnaireProgressCommand command =
                new SaveQuestionnaireProgressCommand(
                        actorUserId,
                        session.id(),
                        List.of()
                );

        assertThrows(
                AssessmentAlreadySubmittedException.class,
                () ->
                        service.execute(
                                command
                        )
        );

        verify(
                versionRepository,
                never()
        )
                .findById(
                        any()
                );

        verify(
                sessionRepository,
                never()
        )
                .update(
                        any()
                );
    }

    @Test
    void rejectsSavingAbandonedSession() {
        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion version =
                version(
                        definition.id()
                );

        AssessmentSession session =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        actorUserId,
                        definition.id(),
                        version.id(),
                        NOW
                );

        session.abandon(
                NOW.plusSeconds(30)
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

        SaveQuestionnaireProgressCommand command =
                new SaveQuestionnaireProgressCommand(
                        actorUserId,
                        session.id(),
                        List.of()
                );

        assertThrows(
                AssessmentSessionAlreadyAbandonedException.class,
                () ->
                        service.execute(
                                command
                        )
        );

        verify(
                sessionRepository,
                never()
        )
                .update(
                        any()
                );
    }

    @Test
    void returnsNotFoundForSessionNotOwnedByActor() {
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

        SaveQuestionnaireProgressCommand command =
                new SaveQuestionnaireProgressCommand(
                        actorUserId,
                        sessionId,
                        List.of()
                );

        assertThrows(
                AssessmentSessionNotFoundException.class,
                () ->
                        service.execute(
                                command
                        )
        );
    }

    private QuestionnaireResponse completeQuestionnaireResponse() {
        return new QuestionnaireResponse(
                List.of(
                        new Answer(
                                new QuestionId("Q1"),
                                5
                        ),
                        new Answer(
                                new QuestionId("Q2"),
                                5
                        )
                )
        );
    }

    private InitialAssessmentResult ambiguousInitialResult() {
        return new InitialAssessmentResult(
                List.of(
                        new InitialDimensionResult(
                                new DimensionCode("XY"),
                                0,
                                null,
                                true
                        )
                )
        );
    }

    private void prepareValidSession(
            AssessmentDefinition definition,
            AssessmentDefinitionVersion version,
            AssessmentSession session
    ) {
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
                versionRepository.findById(
                        version.id()
                )
        )
                .thenReturn(
                        Optional.of(version)
                );

        when(
                definitionRepository.findById(
                        definition.id()
                )
        )
                .thenReturn(
                        Optional.of(definition)
                );
    }

    private AssessmentDefinition definition() {
        return AssessmentDefinition.restore(
                AssessmentDefinitionId.newId(),
                "TEST_ASSESSMENT",
                "Test Assessment",
                NOW.minusSeconds(86400)
        );
    }

    private AssessmentDefinitionVersion version(
            AssessmentDefinitionId definitionId
    ) {
        DimensionCode dimension =
                new DimensionCode("XY");

        PoleCode poleX =
                new PoleCode("X");

        PoleCode poleY =
                new PoleCode("Y");

        AssessmentSpecification specification =
                new AssessmentSpecification(
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

        return AssessmentDefinitionVersion.restore(
                AssessmentDefinitionVersionId.newId(),
                definitionId,
                "1.0",
                AssessmentDefinitionVersionStatus.RETIRED,
                specification,
                NOW.minusSeconds(86400),
                NOW.minusSeconds(86400)
        );
    }
}
