package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence;

import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationExecutionCompletionResult;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationExecutionTicket;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.CompleteClarificationExecutionService;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.SkipDimensionClarificationCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.SkipDimensionClarificationService;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.SkipRemainingClarificationsCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.SkipRemainingClarificationsService;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.StartDimensionClarificationCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.StartDimensionClarificationService;
import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireService;
import dev.springawsportfolio.portfolio.assessment.application.command.tiebreak.SubmitDimensionTieBreakCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.tiebreak.SubmitDimensionTieBreakService;
import dev.springawsportfolio.portfolio.assessment.application.command.tiebreak.TieBreakSelection;
import dev.springawsportfolio.portfolio.assessment.application.exception.ClarificationNotAllowedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.InvalidDimensionTieBreakException;
import dev.springawsportfolio.portfolio.assessment.application.exception.TieBreakAlreadyDecidedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.TieBreakNotRequiredException;
import dev.springawsportfolio.portfolio.assessment.application.query.tiebreak.DimensionTieBreakInteractionResult;
import dev.springawsportfolio.portfolio.assessment.application.query.session.GetAssessmentSessionService;
import dev.springawsportfolio.portfolio.assessment.application.query.tiebreak.GetDimensionTieBreakInteractionService;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.AIProvenance;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationConfidence;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResolution;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResult;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.TieBreakOptionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.TieBreakQuestionId;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionTieBreakRepository;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDecisionSource;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import dev.springawsportfolio.portfolio.assessment.domain.tiebreak.DimensionTieBreak;
import dev.springawsportfolio.portfolio.assessment.web.session.AssessmentSessionWebMapper;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.AssessmentSessionResponse;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@Testcontainers
class AssessmentContextualTieBreakIntegrationTest {

    private static final AssessmentDefinitionVersionId
            VERSION_1_1_ID =
            new AssessmentDefinitionVersionId(
                    UUID.fromString(
                            "b7a63f1e-60f4-4b5e-a0e3-1c1b1a110001"
                    )
            );

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    SubmitQuestionnaireService
            submitQuestionnaireService;

    @Autowired
    SkipDimensionClarificationService
            skipDimensionClarificationService;

    @Autowired
    StartDimensionClarificationService
            startDimensionClarificationService;

    @Autowired
    CompleteClarificationExecutionService
            completeClarificationExecutionService;

    @Autowired
    SkipRemainingClarificationsService
            skipRemainingClarificationsService;

    @Autowired
    GetDimensionTieBreakInteractionService
            getDimensionTieBreakInteractionService;

    @Autowired
    GetAssessmentSessionService
            getAssessmentSessionService;

    @Autowired
    SubmitDimensionTieBreakService
            submitDimensionTieBreakService;

    @Autowired
    AssessmentDefinitionRepository
            definitionRepository;

    @Autowired
    AssessmentDefinitionVersionRepository
            versionRepository;

    @Autowired
    AssessmentSessionRepository
            sessionRepository;

    @Autowired
    DimensionTieBreakRepository
            tieBreakRepository;

    @Autowired
    AssessmentSessionWebMapper sessionWebMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void contextualInteractionUsesBoundVersionAndPersistsOptionProvenance() {
        UserId userId =
                createUser();

        AssessmentDefinition definition =
                definitionRepository
                        .findByCode(
                                "SIXTEEN_PERSONALITY"
                        )
                        .orElseThrow();

        AssessmentDefinitionVersion version =
                versionRepository
                        .findById(
                                VERSION_1_1_ID
                        )
                        .orElseThrow();

        AssessmentSession session =
                createSession(
                        userId,
                        definition,
                        version
                );

        submitQuestionnaireService.execute(
                new SubmitQuestionnaireCommand(
                        userId,
                        session.id(),
                        neutralAnswers(
                                version
                        )
                )
        );

        DimensionDefinition dimension =
                version
                        .specification()
                        .dimensions()
                        .getFirst();

        skipDimensionClarificationService.execute(
                new SkipDimensionClarificationCommand(
                        userId,
                        session.id(),
                        dimension.code()
                )
        );

        DimensionTieBreakInteractionResult interaction =
                getDimensionTieBreakInteractionService.execute(
                        userId,
                        session.id(),
                        dimension.code()
                );

        DimensionTieBreakInteractionResult.ContextualQuestion
                contextual =
                assertInstanceOf(
                        DimensionTieBreakInteractionResult
                                .ContextualQuestion.class,
                        interaction
                );

        assertEquals(
                "TB-EI-1",
                contextual.questionId()
        );

        assertEquals(
                2,
                contextual.options().size()
        );

        assertEquals(
                "TB-EI-02",
                contextual
                        .options()
                        .get(1)
                        .optionId()
        );

        AssessmentSessionResult afterTieBreak =
                submitDimensionTieBreakService.execute(
                        new SubmitDimensionTieBreakCommand(
                                userId,
                                session.id(),
                                dimension.code(),
                                new TieBreakSelection
                                        .ContextualOptionSelection(
                                        new TieBreakQuestionId(
                                                "TB-EI-1"
                                        ),
                                        new TieBreakOptionId(
                                                "TB-EI-02"
                                        )
                                )
                        )
                );

        AssessmentSessionResult.TieBreakStateResult
                state =
                afterTieBreak
                        .tieBreaks()
                        .getFirst();

        assertTrue(
                state.contextual()
        );

        assertEquals(
                "TB-EI-1",
                state.questionId()
        );

        assertEquals(
                "TB-EI-02",
                state.selectedOptionId()
        );

        assertEquals(
                "I",
                state.selectedPole()
        );

        AssessmentSessionResponse response =
                sessionWebMapper.toSessionResponse(
                        afterTieBreak
                );

        AssessmentSessionResponse.ContextualTieBreakStateResponse
                contextualState =
                assertInstanceOf(
                        AssessmentSessionResponse
                                .ContextualTieBreakStateResponse.class,
                        response
                                .tieBreaks()
                                .getFirst()
                );

        assertEquals(
                "TB-EI-1",
                contextualState.questionId()
        );

        assertEquals(
                "TB-EI-02",
                contextualState.selectedOptionId()
        );

        DimensionTieBreak persisted =
                tieBreakRepository
                        .findBySessionIdAndDimension(
                                session.id(),
                                dimension.code()
                        )
                        .orElseThrow();

        assertEquals(
                "TB-EI-1",
                persisted
                        .questionId()
                        .value()
        );

        assertEquals(
                "TB-EI-02",
                persisted
                        .selectedOptionId()
                        .value()
        );

        assertEquals(
                "I",
                persisted
                        .resolvedPole()
                        .value()
        );

        assertThrows(
                TieBreakNotRequiredException.class,
                () ->
                        getDimensionTieBreakInteractionService.execute(
                                userId,
                                session.id(),
                                dimension.code()
                        )
        );
    }

    @Test
    void unclearClarificationStillRequiresContextualTieBreakForVersionOnePointOne() {
        UserId userId =
                createUser();

        AssessmentDefinition definition =
                definitionRepository
                        .findByCode(
                                "SIXTEEN_PERSONALITY"
                        )
                        .orElseThrow();

        AssessmentDefinitionVersion version =
                versionRepository
                        .findById(
                                VERSION_1_1_ID
                        )
                        .orElseThrow();

        AssessmentSession session =
                createSession(
                        userId,
                        definition,
                        version
                );

        submitQuestionnaireService.execute(
                new SubmitQuestionnaireCommand(
                        userId,
                        session.id(),
                        neutralAnswers(
                                version
                        )
                )
        );

        DimensionDefinition dimension =
                version
                        .specification()
                        .dimensions()
                        .getFirst();

        ClarificationExecutionTicket ticket =
                startDimensionClarificationService.execute(
                        new StartDimensionClarificationCommand(
                                userId,
                                session.id(),
                                dimension.code()
                        )
                );

        ClarificationExecutionCompletionResult completion =
                completeClarificationExecutionService.accept(
                        ticket,
                        new ClarificationResult(
                                ClarificationResolution.UNCLEAR,
                                null,
                                ClarificationConfidence.LOW,
                                "Acceptance sweep remained balanced."
                        ),
                        new AIProvenance(
                                "test-provider",
                                "test-model",
                                version
                                        .specification()
                                        .clarificationPolicy()
                                        .revision()
                        )
                );

        assertTrue(
                completion.accepted()
        );

        assertTrue(
                completion
                        .session()
                        .workflow()
                        .tieBreakRequiredDimensions()
                        .contains(
                                dimension
                                        .code()
                                        .value()
                        )
        );

        DimensionTieBreakInteractionResult.ContextualQuestion
                interaction =
                assertInstanceOf(
                        DimensionTieBreakInteractionResult
                                .ContextualQuestion.class,
                        getDimensionTieBreakInteractionService
                                .execute(
                                        userId,
                                        session.id(),
                                        dimension.code()
                                )
                );

        assertEquals(
                "TB-EI-1",
                interaction.questionId()
        );

        assertEquals(
                List.of(
                        "TB-EI-01",
                        "TB-EI-02"
                ),
                interaction
                        .options()
                        .stream()
                        .map(
                                DimensionTieBreakInteractionResult
                                        .Option::optionId
                        )
                        .toList()
        );
    }

    @Test
    void contextualTieBreaksCompleteSessionWithContextualDecisionSources() {
        UserId userId =
                createUser();

        AssessmentDefinition definition =
                definitionRepository
                        .findByCode(
                                "SIXTEEN_PERSONALITY"
                        )
                        .orElseThrow();

        AssessmentDefinitionVersion available =
                versionRepository
                        .findAvailableByDefinitionId(
                                definition.id()
                        )
                        .orElseThrow();

        assertEquals(
                "1.0",
                available.versionCode()
        );

        AssessmentDefinitionVersion version =
                versionRepository
                        .findById(
                                VERSION_1_1_ID
                        )
                        .orElseThrow();

        AssessmentSession session =
                createSession(
                        userId,
                        definition,
                        version
                );

        submitQuestionnaireService.execute(
                new SubmitQuestionnaireCommand(
                        userId,
                        session.id(),
                        neutralAnswers(
                                version
                        )
                )
        );

        skipRemainingClarificationsService.execute(
                new SkipRemainingClarificationsCommand(
                        userId,
                        session.id()
                )
        );

        AssessmentSessionResult latest =
                null;

        for (
                var question
                : version
                .specification()
                .tieBreakQuestions()
        ) {
            var option =
                    question
                            .options()
                            .getFirst();

            latest =
                    submitDimensionTieBreakService.execute(
                            new SubmitDimensionTieBreakCommand(
                                    userId,
                                    session.id(),
                                    question.dimension(),
                                    new TieBreakSelection
                                            .ContextualOptionSelection(
                                            question.questionId(),
                                            option.optionId()
                                    )
                            )
                    );
        }

        AssessmentSessionResult completed =
                java.util.Objects.requireNonNull(
                        latest
                );

        assertEquals(
                AssessmentSessionStatus.COMPLETED,
                completed.status()
        );

        assertEquals(
                "ESTJ",
                completed
                        .finalResult()
                        .finalType()
        );

        assertEquals(
                4,
                completed
                        .finalResult()
                        .dimensions()
                        .size()
        );

        assertTrue(
                completed
                        .finalResult()
                        .dimensions()
                        .stream()
                        .allMatch(conclusion ->
                                conclusion.decisionSource()
                                        == FinalDecisionSource
                                        .TIE_BREAK_QUESTION
                        )
        );

        assertTrue(
                completed
                        .initialResult()
                        .dimensions()
                        .stream()
                        .allMatch(dimension ->
                                dimension.rawScore() == 0
                                        && dimension
                                        .questionnairePreference()
                                        == null
                        )
        );

        assertTrue(
                completed
                        .dimensionEvidence()
                        .stream()
                        .allMatch(evidence ->
                                evidence.poleAPercentage() == 50.0
                                        && evidence.poleBPercentage() == 50.0
                        )
        );

        AssessmentSession reloaded =
                sessionRepository
                        .findOwnedById(
                                session.id(),
                                userId
                        )
                        .orElseThrow();

        assertEquals(
                AssessmentSessionStatus.COMPLETED,
                reloaded.status()
        );

        assertEquals(
                "ESTJ",
                reloaded
                        .finalResult()
                        .finalType()
        );

        assertTrue(
                reloaded
                        .finalResult()
                        .dimensions()
                        .stream()
                        .allMatch(conclusion ->
                                conclusion.decisionSource()
                                        == FinalDecisionSource
                                        .TIE_BREAK_QUESTION
                        )
        );

        AssessmentSessionResult historicalDetail =
                getAssessmentSessionService.execute(
                        userId,
                        session.id()
                );

        assertEquals(
                4,
                historicalDetail
                        .tieBreaks()
                        .size()
        );

        assertTrue(
                historicalDetail
                        .tieBreaks()
                        .stream()
                        .allMatch(
                                AssessmentSessionResult
                                        .TieBreakStateResult::contextual
                        )
        );

        assertTrue(
                historicalDetail
                        .finalResult()
                        .dimensions()
                        .stream()
                        .allMatch(conclusion ->
                                conclusion.decisionSource()
                                        == FinalDecisionSource
                                        .TIE_BREAK_QUESTION
                        )
        );

        AssessmentSessionResponse historicalResponse =
                sessionWebMapper.toSessionResponse(
                        historicalDetail
                );

        assertTrue(
                historicalResponse
                        .tieBreaks()
                        .stream()
                        .allMatch(state ->
                                state
                                        instanceof AssessmentSessionResponse
                                        .ContextualTieBreakStateResponse
                        )
        );

        var lastQuestion =
                version
                        .specification()
                        .tieBreakQuestions()
                        .getLast();

        DimensionTieBreak beforeRetry =
                tieBreakRepository
                        .findBySessionIdAndDimension(
                                session.id(),
                                lastQuestion.dimension()
                        )
                        .orElseThrow();

        Instant originalDecidedAt =
                beforeRetry.decidedAt();

        AssessmentSessionResult retry =
                submitDimensionTieBreakService.execute(
                        new SubmitDimensionTieBreakCommand(
                                userId,
                                session.id(),
                                lastQuestion.dimension(),
                                new TieBreakSelection
                                        .ContextualOptionSelection(
                                        lastQuestion.questionId(),
                                        lastQuestion
                                                .options()
                                                .getFirst()
                                                .optionId()
                                )
                        )
                );

        assertEquals(
                AssessmentSessionStatus.COMPLETED,
                retry.status()
        );

        assertEquals(
                4,
                tieBreakRepository
                        .findBySessionId(
                                session.id()
                        )
                        .size()
        );

        DimensionTieBreak afterRetry =
                tieBreakRepository
                        .findBySessionIdAndDimension(
                                session.id(),
                                lastQuestion.dimension()
                        )
                        .orElseThrow();

        assertEquals(
                originalDecidedAt,
                afterRetry.decidedAt()
        );

        assertThrows(
                TieBreakAlreadyDecidedException.class,
                () ->
                        submitDimensionTieBreakService.execute(
                                new SubmitDimensionTieBreakCommand(
                                        userId,
                                        session.id(),
                                        lastQuestion.dimension(),
                                        new TieBreakSelection
                                                .ContextualOptionSelection(
                                                lastQuestion.questionId(),
                                                lastQuestion
                                                        .options()
                                                        .get(1)
                                                        .optionId()
                                        )
                                )
                        )
        );

        DimensionTieBreak afterConflict =
                tieBreakRepository
                        .findBySessionIdAndDimension(
                                session.id(),
                                lastQuestion.dimension()
                        )
                        .orElseThrow();

        assertEquals(
                originalDecidedAt,
                afterConflict.decidedAt()
        );

        assertEquals(
                lastQuestion
                        .options()
                        .getFirst()
                        .optionId(),
                afterConflict.selectedOptionId()
        );
    }

    @Test
    void legacyAvailableVersionStillExposesDirectPoleInteraction() {
        UserId userId =
                createUser();

        AssessmentDefinition definition =
                definitionRepository
                        .findByCode(
                                "SIXTEEN_PERSONALITY"
                        )
                        .orElseThrow();

        AssessmentDefinitionVersion version =
                versionRepository
                        .findAvailableByDefinitionId(
                                definition.id()
                        )
                        .orElseThrow();

        assertEquals(
                "1.0",
                version.versionCode()
        );

        AssessmentSession session =
                createSession(
                        userId,
                        definition,
                        version
                );

        submitQuestionnaireService.execute(
                new SubmitQuestionnaireCommand(
                        userId,
                        session.id(),
                        neutralAnswers(
                                version
                        )
                )
        );

        DimensionDefinition dimension =
                version
                        .specification()
                        .dimensions()
                        .getFirst();

        skipDimensionClarificationService.execute(
                new SkipDimensionClarificationCommand(
                        userId,
                        session.id(),
                        dimension.code()
                )
        );

        DimensionTieBreakInteractionResult.DirectPoleSelection
                interaction =
                assertInstanceOf(
                        DimensionTieBreakInteractionResult
                                .DirectPoleSelection.class,
                        getDimensionTieBreakInteractionService
                                .execute(
                                        userId,
                                        session.id(),
                                        dimension.code()
                                )
                );

        assertEquals(
                List.of(
                        dimension.poleA().value(),
                        dimension.poleB().value()
                ),
                interaction.allowedPoles()
        );
    }

    @Test
    void contextualVersionRejectsLegacyOrMismatchedSelections() {
        UserId userId =
                createUser();

        AssessmentDefinition definition =
                definitionRepository
                        .findByCode(
                                "SIXTEEN_PERSONALITY"
                        )
                        .orElseThrow();

        AssessmentDefinitionVersion version =
                versionRepository
                        .findById(
                                VERSION_1_1_ID
                        )
                        .orElseThrow();

        AssessmentSession session =
                createSession(
                        userId,
                        definition,
                        version
                );

        submitQuestionnaireService.execute(
                new SubmitQuestionnaireCommand(
                        userId,
                        session.id(),
                        neutralAnswers(
                                version
                        )
                )
        );

        DimensionDefinition dimension =
                version
                        .specification()
                        .dimensions()
                        .getFirst();

        skipDimensionClarificationService.execute(
                new SkipDimensionClarificationCommand(
                        userId,
                        session.id(),
                        dimension.code()
                )
        );

        assertThrows(
                InvalidDimensionTieBreakException.class,
                () ->
                        submitDimensionTieBreakService.execute(
                                new SubmitDimensionTieBreakCommand(
                                        userId,
                                        session.id(),
                                        dimension.code(),
                                        new PoleCode(
                                                "I"
                                        )
                                )
                        )
        );

        assertThrows(
                InvalidDimensionTieBreakException.class,
                () ->
                        submitDimensionTieBreakService.execute(
                                new SubmitDimensionTieBreakCommand(
                                        userId,
                                        session.id(),
                                        dimension.code(),
                                        new TieBreakSelection
                                                .ContextualOptionSelection(
                                                new TieBreakQuestionId(
                                                        "TB-SN-1"
                                                ),
                                                new TieBreakOptionId(
                                                        "TB-SN-01"
                                                )
                                        )
                                )
                        )
        );
    }

    private List<SubmitQuestionnaireCommand.AnswerInput> neutralAnswers(
            AssessmentDefinitionVersion version
    ) {
        return version
                .specification()
                .questionnaire()
                .questions()
                .stream()
                .map(question ->
                        new SubmitQuestionnaireCommand.AnswerInput(
                                question
                                        .questionId()
                                        .value(),
                                version
                                        .specification()
                                        .scoringPolicy()
                                        .answerCenter()
                        )
                )
                .toList();
    }

    private AssessmentSession createSession(
            UserId userId,
            AssessmentDefinition definition,
            AssessmentDefinitionVersion version
    ) {
        AssessmentSession session =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        userId,
                        definition.id(),
                        version.id(),
                        Instant.now()
                );

        assertTrue(
                sessionRepository.tryCreateActive(
                        session
                )
        );

        return session;
    }

    private UserId createUser() {
        UUID id =
                UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO user_accounts (
                    id,
                    email,
                    display_name,
                    password_hash,
                    created_at,
                    version
                )
                VALUES (?, ?, ?, ?, ?, 0)
                """,
                id,
                "contextual-tie-break-"
                        + id
                        + "@example.com",
                "Contextual Tie Break User",
                "test-password-hash",
                Timestamp.from(
                        Instant.now()
                )
        );

        return new UserId(
                id
        );
    }
}
