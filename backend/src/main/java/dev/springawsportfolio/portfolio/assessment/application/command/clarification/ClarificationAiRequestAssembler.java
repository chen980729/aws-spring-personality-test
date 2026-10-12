package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeContext;
import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeMessage;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiRequest;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.AnswerOption;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.Answer;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.QuestionnaireResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ClarificationAiRequestAssembler {

    public ClarificationAiRequest assemble(
            AssessmentSession session,
            AssessmentDefinitionVersion version,
            DimensionCode dimensionCode,
            ClarificationRuntimeContext context,
            int maximumModelTurns
    ) {
        Objects.requireNonNull(
                session,
                "session must not be null"
        );

        Objects.requireNonNull(
                version,
                "version must not be null"
        );

        Objects.requireNonNull(
                dimensionCode,
                "dimensionCode must not be null"
        );

        Objects.requireNonNull(
                context,
                "context must not be null"
        );

        if (maximumModelTurns <= 0) {
            throw new IllegalArgumentException(
                    "maximumModelTurns must be positive"
            );
        }

        DimensionDefinition dimension =
                version
                        .specification()
                        .dimensions()
                        .stream()
                        .filter(candidate ->
                                candidate
                                        .code()
                                        .equals(
                                                dimensionCode
                                        )
                        )
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "bound definition version is missing dimension: "
                                                        + dimensionCode.value()
                                        )
                        );

        QuestionnaireResponse response =
                Objects.requireNonNull(
                        session.questionnaireResponse(),
                        "clarification session must have questionnaire response"
                );

        Map<?, Answer> answersByQuestionId =
                response
                        .answers()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Answer::questionId,
                                        Function.identity()
                                )
                        );

        Map<Integer, String> labelsByValue =
                version
                        .specification()
                        .questionnaire()
                        .answerScale()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        AnswerOption::value,
                                        AnswerOption::label
                                )
                        );

        List<ClarificationAiRequest.QuestionnaireEvidence>
                questionnaireEvidence =
                version
                        .specification()
                        .questionnaire()
                        .questions()
                        .stream()
                        .filter(question ->
                                question
                                        .dimension()
                                        .equals(
                                                dimensionCode
                                        )
                        )
                        .sorted(
                                Comparator.comparingInt(
                                        QuestionDefinition::position
                                )
                        )
                        .map(question ->
                                evidence(
                                        question,
                                        answersByQuestionId,
                                        labelsByValue
                                )
                        )
                        .toList();

        if (questionnaireEvidence.isEmpty()) {
            throw new IllegalStateException(
                    "target dimension has no questionnaire evidence"
            );
        }

        if (
                !context.isEmpty()
                        && context
                        .messages()
                        .getLast()
                        .role()
                        != ClarificationRuntimeMessage.Role.USER_ANSWER
        ) {
            throw new IllegalStateException(
                    "AI request context must be empty or end with USER_ANSWER"
            );
        }

        int currentModelTurn =
                context.latestSequence() / 2 + 1;

        if (currentModelTurn > maximumModelTurns) {
            throw new IllegalStateException(
                    "runtime context exceeds maximum model turns"
            );
        }

        List<ClarificationAiRequest.ConversationMessage>
                conversation =
                context
                        .messages()
                        .stream()
                        .map(
                                ClarificationAiRequestAssembler
                                        ::conversationMessage
                        )
                        .toList();

        return new ClarificationAiRequest(
                new ClarificationAiRequest.TargetDimension(
                        dimension.code(),
                        dimension.poleA(),
                        dimension.poleB()
                ),
                questionnaireEvidence,
                conversation,
                currentModelTurn,
                maximumModelTurns,
                version
                        .specification()
                        .clarificationPolicy()
                        .revision()
        );
    }

    private static ClarificationAiRequest.QuestionnaireEvidence evidence(
            QuestionDefinition question,
            Map<?, Answer> answersByQuestionId,
            Map<Integer, String> labelsByValue
    ) {
        Answer answer =
                Objects.requireNonNull(
                        answersByQuestionId.get(
                                question.questionId()
                        ),
                        "submitted questionnaire is missing answer for "
                                + question.questionId().value()
                );

        String answerLabel =
                Objects.requireNonNull(
                        labelsByValue.get(
                                answer.value()
                        ),
                        "submitted questionnaire answer is outside answer scale: "
                                + answer.value()
                );

        return new ClarificationAiRequest.QuestionnaireEvidence(
                question.prompt(),
                answerLabel,
                question.keyedPole()
        );
    }

    private static ClarificationAiRequest.ConversationMessage
    conversationMessage(
            ClarificationRuntimeMessage message
    ) {
        ClarificationAiRequest.ConversationRole role =
                switch (message.role()) {
                    case ASSISTANT_QUESTION ->
                            ClarificationAiRequest
                                    .ConversationRole
                                    .ASSISTANT_QUESTION;
                    case USER_ANSWER ->
                            ClarificationAiRequest
                                    .ConversationRole
                                    .USER_ANSWER;
                };

        return new ClarificationAiRequest.ConversationMessage(
                role,
                message.text()
        );
    }
}
