package dev.springawsportfolio.portfolio.assessment.application.command.questionnaire;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentAlreadySubmittedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionAlreadyAbandonedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.InvalidQuestionnaireResponseException;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.Answer;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.QuestionnaireResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class SaveQuestionnaireProgressService {

    private final AssessmentSessionRepository
            sessionRepository;

    private final AssessmentDefinitionRepository
            definitionRepository;

    private final AssessmentDefinitionVersionRepository
            versionRepository;

    public SaveQuestionnaireProgressService(
            AssessmentSessionRepository sessionRepository,
            AssessmentDefinitionRepository definitionRepository,
            AssessmentDefinitionVersionRepository versionRepository
    ) {
        this.sessionRepository =
                sessionRepository;

        this.definitionRepository =
                definitionRepository;

        this.versionRepository =
                versionRepository;
    }

    @Transactional
    public AssessmentSessionResult execute(
            SaveQuestionnaireProgressCommand command
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

        validateMutableState(
                session
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

        QuestionnaireResponse response =
                validateAndBuildResponse(
                        command.answers(),
                        version
                );

        session.replaceQuestionnaireResponse(
                response
        );

        sessionRepository.update(
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

        return AssessmentSessionResult.from(
                definition,
                version,
                session
        );
    }

    private void validateMutableState(
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
                    "questionnaire draft can only be saved "
                            + "for an IN_PROGRESS session"
            );
        }
    }

    private QuestionnaireResponse validateAndBuildResponse(
            List<SaveQuestionnaireProgressCommand.AnswerInput> inputs,
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
                SaveQuestionnaireProgressCommand.AnswerInput input
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

        /*
         * Persist answers in canonical questionnaire order,
         * independent of request ordering.
         */
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
}
