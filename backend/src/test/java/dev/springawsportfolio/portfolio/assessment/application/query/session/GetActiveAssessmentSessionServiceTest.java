package dev.springawsportfolio.portfolio.assessment.application.query.session;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.port.out.AssessmentSessionWorkflowQuery;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
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

class GetActiveAssessmentSessionServiceTest {

    private static final String ASSESSMENT_CODE =
            "TEST_ASSESSMENT";

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-26T00:00:00Z"
            );

    private AssessmentDefinitionRepository
            definitionRepository;

    private AssessmentDefinitionVersionRepository
            versionRepository;

    private AssessmentSessionRepository
            sessionRepository;

    private AssessmentSessionWorkflowQuery
            workflowQuery;

    private GetActiveAssessmentSessionService service;

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

        service =
                new GetActiveAssessmentSessionService(
                        definitionRepository,
                        versionRepository,
                        sessionRepository,
                        workflowQuery
                );

        actorUserId =
                new UserId(
                        UUID.randomUUID()
                );
    }

    @Test
    void returnsActiveSessionUsingItsBoundVersion() {
        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersion boundVersion =
                version(
                        definition.id(),
                        "1.0",
                        AssessmentDefinitionVersionStatus.RETIRED
                );

        AssessmentSession session =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        actorUserId,
                        definition.id(),
                        boundVersion.id(),
                        NOW.minusSeconds(3600)
                );

        session.replaceQuestionnaireResponse(
                new QuestionnaireResponse(
                        List.of(
                                new Answer(
                                        new QuestionId("Q1"),
                                        5
                                )
                        )
                )
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
                        Optional.of(session)
                );

        when(
                versionRepository.findById(
                        boundVersion.id()
                )
        )
                .thenReturn(
                        Optional.of(boundVersion)
                );

        AssessmentSessionResult result =
                service.execute(
                        actorUserId,
                        ASSESSMENT_CODE
                );

        assertEquals(
                session.id().value(),
                result.id()
        );

        assertEquals(
                "1.0",
                result.assessmentVersion()
        );

        assertFalse(
                result.questionnaireSubmitted()
        );

        assertEquals(
                1,
                result.answers().size()
        );

        assertEquals(
                "Q1",
                result.answers().getFirst().questionId()
        );

        assertEquals(
                5,
                result.answers().getFirst().value()
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
    void throwsWhenNoActiveSessionExists() {
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

        assertThrows(
                AssessmentSessionNotFoundException.class,
                () ->
                        service.execute(
                                actorUserId,
                                ASSESSMENT_CODE
                        )
        );
    }

    @Test
    void throwsWhenBoundVersionCannotBeLoaded() {
        AssessmentDefinition definition =
                definition();

        AssessmentDefinitionVersionId versionId =
                AssessmentDefinitionVersionId.newId();

        AssessmentSession session =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        actorUserId,
                        definition.id(),
                        versionId,
                        NOW
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
                        Optional.of(session)
                );

        when(
                versionRepository.findById(
                        versionId
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
                                ASSESSMENT_CODE
                        )
        );
    }

    private AssessmentDefinition definition() {
        return AssessmentDefinition.restore(
                AssessmentDefinitionId.newId(),
                ASSESSMENT_CODE,
                "Test Assessment",
                NOW.minusSeconds(86400)
        );
    }

    private AssessmentDefinitionVersion version(
            AssessmentDefinitionId definitionId,
            String versionCode,
            AssessmentDefinitionVersionStatus status
    ) {
        return AssessmentDefinitionVersion.restore(
                AssessmentDefinitionVersionId.newId(),
                definitionId,
                versionCode,
                status,
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
