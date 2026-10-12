package dev.springawsportfolio.portfolio.assessment.application.port.out;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;

import java.util.List;
import java.util.Objects;

public record ClarificationAiRequest(
        TargetDimension targetDimension,
        List<QuestionnaireEvidence> questionnaireEvidence,
        List<ConversationMessage> conversation,
        int currentModelTurn,
        int maximumModelTurns,
        String clarificationPolicyRevision
) {

    public ClarificationAiRequest {
        Objects.requireNonNull(
                targetDimension,
                "targetDimension must not be null"
        );

        Objects.requireNonNull(
                questionnaireEvidence,
                "questionnaireEvidence must not be null"
        );

        if (questionnaireEvidence.isEmpty()) {
            throw new IllegalArgumentException(
                    "questionnaireEvidence must not be empty"
            );
        }

        questionnaireEvidence.forEach(evidence -> {
            Objects.requireNonNull(
                    evidence,
                    "questionnaireEvidence must not contain null"
            );

            if (!targetDimension.containsPole(evidence.keyedPole())) {
                throw new IllegalArgumentException(
                        "questionnaire evidence keyedPole must belong "
                                + "to target dimension"
                );
            }
        });

        questionnaireEvidence =
                List.copyOf(questionnaireEvidence);

        Objects.requireNonNull(
                conversation,
                "conversation must not be null"
        );

        conversation.forEach(message ->
                Objects.requireNonNull(
                        message,
                        "conversation must not contain null"
                )
        );

        conversation =
                List.copyOf(conversation);

        if (maximumModelTurns <= 0) {
            throw new IllegalArgumentException(
                    "maximumModelTurns must be positive"
            );
        }

        if (
                currentModelTurn <= 0
                        || currentModelTurn > maximumModelTurns
        ) {
            throw new IllegalArgumentException(
                    "currentModelTurn must be between 1 "
                            + "and maximumModelTurns"
            );
        }

        int expectedConversationSize =
                2 * (currentModelTurn - 1);

        if (
                conversation.size()
                        != expectedConversationSize
        ) {
            throw new IllegalArgumentException(
                    "conversation size must match completed "
                            + "follow-up turns"
            );
        }

        for (
                int index = 0;
                index < conversation.size();
                index++
        ) {
            ConversationRole expectedRole =
                    index % 2 == 0
                            ? ConversationRole.ASSISTANT_QUESTION
                            : ConversationRole.USER_ANSWER;

            if (
                    conversation.get(index).role()
                            != expectedRole
            ) {
                throw new IllegalArgumentException(
                        "conversation must alternate "
                                + "ASSISTANT_QUESTION and USER_ANSWER"
                );
            }
        }

        requireNonBlank(
                clarificationPolicyRevision,
                "clarificationPolicyRevision"
        );
    }

    public boolean finalModelTurn() {
        return currentModelTurn
                == maximumModelTurns;
    }

    public record TargetDimension(
            DimensionCode code,
            PoleCode poleA,
            PoleCode poleB
    ) {

        public TargetDimension {
            Objects.requireNonNull(
                    code,
                    "code must not be null"
            );
            Objects.requireNonNull(
                    poleA,
                    "poleA must not be null"
            );
            Objects.requireNonNull(
                    poleB,
                    "poleB must not be null"
            );

            if (poleA.equals(poleB)) {
                throw new IllegalArgumentException(
                        "target dimension poles must be different"
                );
            }
        }

        public boolean containsPole(
                PoleCode pole
        ) {
            Objects.requireNonNull(
                    pole,
                    "pole must not be null"
            );

            return poleA.equals(pole)
                    || poleB.equals(pole);
        }
    }

    public record QuestionnaireEvidence(
            String questionText,
            String answerLabel,
            PoleCode keyedPole
    ) {

        public QuestionnaireEvidence {
            requireNonBlank(
                    questionText,
                    "questionText"
            );
            requireNonBlank(
                    answerLabel,
                    "answerLabel"
            );
            Objects.requireNonNull(
                    keyedPole,
                    "keyedPole must not be null"
            );
        }
    }

    public record ConversationMessage(
            ConversationRole role,
            String text
    ) {

        public ConversationMessage {
            Objects.requireNonNull(
                    role,
                    "role must not be null"
            );
            requireNonBlank(
                    text,
                    "text"
            );
        }
    }

    public enum ConversationRole {
        ASSISTANT_QUESTION,
        USER_ANSWER
    }

    private static void requireNonBlank(
            String value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " must not be null"
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }
    }
}
