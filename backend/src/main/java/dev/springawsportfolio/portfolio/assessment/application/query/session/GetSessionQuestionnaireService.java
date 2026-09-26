package dev.springawsportfolio.portfolio.assessment.application.query.session;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.AnswerOption;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Objects;

@Service
public class GetSessionQuestionnaireService {

    private final AssessmentSessionRepository
            sessionRepository;

    private final AssessmentDefinitionRepository
            definitionRepository;

    private final AssessmentDefinitionVersionRepository
            versionRepository;

    public GetSessionQuestionnaireService(
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

    @Transactional(readOnly = true)
    public GetSessionQuestionnaireResult execute(
            UserId actorUserId,
            AssessmentSessionId sessionId
    ) {
        Objects.requireNonNull(
                actorUserId,
                "actorUserId must not be null"
        );

        Objects.requireNonNull(
                sessionId,
                "sessionId must not be null"
        );

        AssessmentSession session =
                sessionRepository
                        .findOwnedById(
                                sessionId,
                                actorUserId
                        )
                        .orElseThrow(
                                AssessmentSessionNotFoundException::new
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

        /*
         * The questionnaire must always come from the
         * exact DefinitionVersion bound to this Session.
         *
         * Never use findAvailableByDefinitionId() here.
         */
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

        var questionnaire =
                version
                        .specification()
                        .questionnaire();

        var questions =
                questionnaire
                        .questions()
                        .stream()
                        .sorted(
                                Comparator.comparingInt(
                                        QuestionDefinition::position
                                )
                        )
                        .map(question ->
                                new GetSessionQuestionnaireResult
                                        .QuestionResult(
                                        question
                                                .questionId()
                                                .value(),
                                        question.position(),
                                        question.prompt()
                                )
                        )
                        .toList();

        var answerScale =
                questionnaire
                        .answerScale()
                        .stream()
                        .sorted(
                                Comparator.comparingInt(
                                        AnswerOption::value
                                )
                        )
                        .map(option ->
                                new GetSessionQuestionnaireResult
                                        .AnswerOptionResult(
                                        option.value(),
                                        option.label()
                                )
                        )
                        .toList();

        var answers =
                session
                        .questionnaireResponse()
                        .answers()
                        .stream()
                        .map(answer ->
                                new GetSessionQuestionnaireResult
                                        .QuestionAnswerResult(
                                        answer
                                                .questionId()
                                                .value(),
                                        answer.value()
                                )
                        )
                        .toList();

        return new GetSessionQuestionnaireResult(
                definition.code(),
                version.versionCode(),
                questions,
                answerScale,
                answers,
                session.questionnaireSubmittedAt()
        );
    }
}
