package dev.springawsportfolio.portfolio.assessment.application.command.submission;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentAlreadySubmittedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionAlreadyAbandonedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.InvalidQuestionnaireResponseException;
import dev.springawsportfolio.portfolio.assessment.application.exception.QuestionnaireIncompleteException;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionClarificationRepository;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.service.AssessmentFinalizationService;
import dev.springawsportfolio.portfolio.assessment.domain.service.QuestionnaireScoringService;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSubmissionOutcome;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.Answer;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.QuestionnaireResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class SubmitQuestionnaireService {

    private final AssessmentSessionRepository
            sessionRepository;

    private final AssessmentDefinitionRepository
            definitionRepository;

    private final AssessmentDefinitionVersionRepository
            versionRepository;

    private final DimensionClarificationRepository
            clarificationRepository;

    private final Clock clock;

    private final QuestionnaireScoringService
            scoringService;

    private final AssessmentFinalizationService
            finalizationService;

    public SubmitQuestionnaireService(
            AssessmentSessionRepository sessionRepository,
            AssessmentDefinitionRepository definitionRepository,
            AssessmentDefinitionVersionRepository versionRepository,
            DimensionClarificationRepository clarificationRepository,
            Clock clock
    ) {
        this.sessionRepository =
                sessionRepository;

        this.definitionRepository =
                definitionRepository;

        this.versionRepository =
                versionRepository;

        this.clarificationRepository =
                clarificationRepository;

        this.clock =
                Objects.requireNonNull(
                        clock,
                        "clock must not be null"
                );

        this.scoringService =
                new QuestionnaireScoringService();

        this.finalizationService =
                new AssessmentFinalizationService();
    }

    @Transactional
    public SubmitQuestionnaireResult execute(
            SubmitQuestionnaireCommand command
    ) {
        Objects.requireNonNull(
                command,
                "command must not be null"
        );

        AssessmentSession session =
                sessionRepository
                        .findOwnedById(
                                command.sessionId(),
                                command.actorUserId()
                        )
                        .orElseThrow(
                                AssessmentSessionNotFoundException::new
                        );

        validateSubmissionState(
                session
        );

        AssessmentDefinition definition =
                definitionRepository
                        .findById(
                                session.definitionId()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "bound assessment definition "
                                                        + "does not exist: "
                                                        + session
                                                        .definitionId()
                                                        .value()
                                        )
                        );

        AssessmentDefinitionVersion version =
                versionRepository
                        .findById(
                                session.definitionVersionId()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "bound assessment definition "
                                                        + "version does not exist: "
                                                        + session
                                                        .definitionVersionId()
                                                        .value()
                                        )
                        );

        if (
                !version.definitionId()
                        .equals(
                                session.definitionId()
                        )
        ) {
            throw new IllegalStateException(
                    "session definition/version binding "
                            + "is inconsistent"
            );
        }

        QuestionnaireResponse finalResponse =
                validateAndBuildCompleteResponse(
                        command.answers(),
                        version
                );

        InitialAssessmentResult initialResult =
                scoringService.score(
                        version.specification(),
                        finalResponse
                );

        FinalAssessmentResult immediateFinalResult =
                initialResult.hasAmbiguousDimensions()
                        ? null
                        : finalizationService
                        .finalizeWithoutClarification(
                                version
                                        .specification()
                                        .dimensions(),
                                initialResult
                        );

        Instant submittedAt =
                clock.instant();

        AssessmentSubmissionOutcome outcome =
                session.submit(
                        finalResponse,
                        initialResult,
                        immediateFinalResult,
                        submittedAt
                );

        sessionRepository.update(
                session
        );

        List<DimensionClarification> clarifications =
                createPendingClarifications(
                        session,
                        outcome,
                        submittedAt
                );

        if (!clarifications.isEmpty()) {
            clarificationRepository.addAll(
                    clarifications
            );
        }

        AssessmentSessionResult sessionResult =
                AssessmentSessionResult.from(
                        definition,
                        version,
                        session,
                        clarifications
                );

        return new SubmitQuestionnaireResult(
                sessionResult
        );
    }

    private void validateSubmissionState(
            AssessmentSession session
    ) {
        if (
                session.status()
                        == AssessmentSessionStatus.ABANDONED
        ) {
            throw new AssessmentSessionAlreadyAbandonedException();
        }

        if (session.isQuestionnaireSubmitted()) {
            throw new AssessmentAlreadySubmittedException();
        }

        if (
                session.status()
                        != AssessmentSessionStatus.IN_PROGRESS
        ) {
            throw new IllegalStateException(
                    "only an IN_PROGRESS assessment session "
                            + "can be submitted"
            );
        }
    }

    private QuestionnaireResponse
    validateAndBuildCompleteResponse(
            List<SubmitQuestionnaireCommand.AnswerInput> inputs,
            AssessmentDefinitionVersion version
    ) {
        var questionnaire =
                version
                        .specification()
                        .questionnaire();

        Map<String, QuestionDefinition> questionsById =
                new HashMap<>();

        for (
                QuestionDefinition question
                : questionnaire.questions()
        ) {
            questionsById.put(
                    question.questionId().value(),
                    question
            );
        }

        Set<Integer> allowedValues =
                new HashSet<>();

        questionnaire
                .answerScale()
                .forEach(option ->
                        allowedValues.add(
                                option.value()
                        )
                );

        Set<String> seenQuestionIds =
                new HashSet<>();

        List<Answer> answers =
                new ArrayList<>();

        for (
                SubmitQuestionnaireCommand.AnswerInput input
                : inputs
        ) {
            if (input == null) {
                throw new InvalidQuestionnaireResponseException(
                        "answer must not be null"
                );
            }

            String questionId =
                    input.questionId();

            if (
                    questionId == null
                            || questionId.isBlank()
            ) {
                throw new InvalidQuestionnaireResponseException(
                        "questionId must not be blank"
                );
            }

            if (
                    !seenQuestionIds.add(
                            questionId
                    )
            ) {
                throw new InvalidQuestionnaireResponseException(
                        "duplicate answer for question: "
                                + questionId
                );
            }

            QuestionDefinition question =
                    questionsById.get(
                            questionId
                    );

            if (question == null) {
                throw new InvalidQuestionnaireResponseException(
                        "unknown question: "
                                + questionId
                );
            }

            if (
                    !allowedValues.contains(
                            input.value()
                    )
            ) {
                throw new InvalidQuestionnaireResponseException(
                        "invalid answer value for question: "
                                + questionId
                );
            }

            answers.add(
                    new Answer(
                            question.questionId(),
                            input.value()
                    )
            );
        }

        if (
                seenQuestionIds.size()
                        != questionsById.size()
        ) {
            throw new QuestionnaireIncompleteException();
        }

        answers.sort(
                Comparator.comparingInt(
                        answer ->
                                questionsById
                                        .get(
                                                answer
                                                        .questionId()
                                                        .value()
                                        )
                                        .position()
                )
        );

        return new QuestionnaireResponse(
                answers
        );
    }

    private List<DimensionClarification>
    createPendingClarifications(
            AssessmentSession session,
            AssessmentSubmissionOutcome outcome,
            Instant submittedAt
    ) {
        if (outcome.completed()) {
            return List.of();
        }

        return outcome
                .ambiguousDimensions()
                .stream()
                .map(dimension ->
                        DimensionClarification.pending(
                                DimensionClarificationId.newId(),
                                session.id(),
                                dimension,
                                submittedAt
                        )
                )
                .toList();
    }
}
