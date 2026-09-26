package dev.springawsportfolio.portfolio.assessment.application.query.session;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetSessionQuestionnaireServiceTest {

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

    private GetSessionQuestionnaireService service;

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
                new GetSessionQuestionnaireService(
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
    void returnsQuestionnaireFromExactBoundVersionAndSavedResponse() {
        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion boundVersion =
                boundVersion(
                        definition.id()
                );

        AssessmentSession session =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        actorUserId,
                        definition.id(),
                        boundVersion.id(),
                        NOW
                );

        session.replaceQuestionnaireResponse(
                new QuestionnaireResponse(
                        List.of(
                                new Answer(
                                        new QuestionId("Q2"),
                                        4
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
                definitionRepository.findById(
                        definition.id()
                )
        )
                .thenReturn(
                        Optional.of(definition)
                );

        when(
                versionRepository.findById(
                        boundVersion.id()
                )
        )
                .thenReturn(
                        Optional.of(boundVersion)
                );

        GetSessionQuestionnaireResult result =
                service.execute(
                        actorUserId,
                        session.id()
                );

        assertEquals(
                "TEST_ASSESSMENT",
                result.assessmentCode()
        );

        assertEquals(
                "1.0",
                result.assessmentVersion()
        );

        assertFalse(
                result.submitted()
        );

        assertEquals(
                2,
                result.questions().size()
        );

        assertEquals(
                "Q1",
                result
                        .questions()
                        .get(0)
                        .questionId()
        );

        assertEquals(
                1,
                result
                        .questions()
                        .get(0)
                        .position()
        );

        assertEquals(
                "Question 1 from v1",
                result
                        .questions()
                        .get(0)
                        .prompt()
        );

        assertEquals(
                "Q2",
                result
                        .questions()
                        .get(1)
                        .questionId()
        );

        assertEquals(
                1,
                result
                        .answerScale()
                        .get(0)
                        .value()
        );

        assertEquals(
                5,
                result
                        .answerScale()
                        .get(4)
                        .value()
        );

        assertEquals(
                1,
                result.answers().size()
        );

        assertEquals(
                "Q2",
                result
                        .answers()
                        .getFirst()
                        .questionId()
        );

        assertEquals(
                4,
                result
                        .answers()
                        .getFirst()
                        .value()
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
    void throwsNotFoundWhenSessionIsNotOwnedByActor() {
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

    @Test
    void throwsWhenBoundDefinitionCannotBeLoaded() {
        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion boundVersion =
                boundVersion(
                        definition.id()
                );

        AssessmentSession session =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        actorUserId,
                        definition.id(),
                        boundVersion.id(),
                        NOW
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
                        definition.id()
                )
        )
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        service.execute(
                                actorUserId,
                                session.id()
                        )
        );
    }

    @Test
    void rejectsInconsistentSessionDefinitionVersionBinding() {
        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionId differentDefinitionId =
                AssessmentDefinitionId.newId();

        AssessmentDefinitionVersion wrongVersion =
                boundVersion(
                        differentDefinitionId
                );

        AssessmentSession session =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        actorUserId,
                        definition.id(),
                        wrongVersion.id(),
                        NOW
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
                        definition.id()
                )
        )
                .thenReturn(
                        Optional.of(definition)
                );

        when(
                versionRepository.findById(
                        wrongVersion.id()
                )
        )
                .thenReturn(
                        Optional.of(wrongVersion)
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        service.execute(
                                actorUserId,
                                session.id()
                        )
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

    private AssessmentDefinitionVersion boundVersion(
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
                                                5,
                                                "Strongly Agree"
                                        ),
                                        new AnswerOption(
                                                3,
                                                "Neutral"
                                        ),
                                        new AnswerOption(
                                                1,
                                                "Strongly Disagree"
                                        ),
                                        new AnswerOption(
                                                4,
                                                "Agree"
                                        ),
                                        new AnswerOption(
                                                2,
                                                "Disagree"
                                        )
                                ),
                                List.of(
                                        new QuestionDefinition(
                                                new QuestionId("Q2"),
                                                2,
                                                "Question 2 from v1",
                                                dimension,
                                                poleY
                                        ),
                                        new QuestionDefinition(
                                                new QuestionId("Q1"),
                                                1,
                                                "Question 1 from v1",
                                                dimension,
                                                poleX
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
