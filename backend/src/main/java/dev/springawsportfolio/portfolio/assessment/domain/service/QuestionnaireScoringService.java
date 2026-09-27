package dev.springawsportfolio.portfolio.assessment.domain.service;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.AmbiguityPolicyType;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.AnswerOption;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.AssessmentSpecification;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.ScoringPolicyType;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.Answer;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.QuestionnaireResponse;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class QuestionnaireScoringService {

    public InitialAssessmentResult score(
            AssessmentSpecification specification,
            QuestionnaireResponse response
    ) {
        Objects.requireNonNull(
                specification,
                "specification must not be null"
        );

        Objects.requireNonNull(
                response,
                "response must not be null"
        );

        validateSupportedPolicies(
                specification
        );

        Map<QuestionId, Integer> answersByQuestionId =
                validateCompleteResponse(
                        specification,
                        response
                );

        List<InitialDimensionResult> dimensionResults =
                specification
                        .dimensions()
                        .stream()
                        .sorted(
                                Comparator.comparingInt(
                                        DimensionDefinition::position
                                )
                        )
                        .map(dimension ->
                                scoreDimension(
                                        dimension,
                                        specification,
                                        answersByQuestionId
                                )
                        )
                        .toList();

        return new InitialAssessmentResult(
                dimensionResults
        );
    }

    private InitialDimensionResult scoreDimension(
            DimensionDefinition dimension,
            AssessmentSpecification specification,
            Map<QuestionId, Integer> answersByQuestionId
    ) {
        int rawScore = 0;

        for (
                QuestionDefinition question
                : specification
                .questionnaire()
                .questions()
        ) {
            if (
                    !question
                            .dimension()
                            .equals(
                                    dimension.code()
                            )
            ) {
                continue;
            }

            int answerValue =
                    answersByQuestionId.get(
                            question.questionId()
                    );

            int centeredScore =
                    answerValue
                            - specification
                            .scoringPolicy()
                            .answerCenter();

            if (
                    question
                            .keyedPole()
                            .equals(
                                    dimension.poleA()
                            )
            ) {
                rawScore += centeredScore;

            } else if (
                    question
                            .keyedPole()
                            .equals(
                                    dimension.poleB()
                            )
            ) {
                rawScore -= centeredScore;

            } else {
                throw new IllegalStateException(
                        "question keyedPole does not belong "
                                + "to its dimension: "
                                + question
                                .questionId()
                                .value()
                );
            }
        }

        if (
                Math.abs(rawScore)
                        > specification
                        .scoringPolicy()
                        .maximumAbsoluteRawScore()
        ) {
            throw new IllegalStateException(
                    "calculated raw score exceeds "
                            + "the scoring policy maximum"
            );
        }

        var questionnairePreference =
                rawScore > 0
                        ? dimension.poleA()
                        : rawScore < 0
                        ? dimension.poleB()
                        : null;

        boolean ambiguous =
                Math.abs(rawScore)
                        <= specification
                        .ambiguityPolicy()
                        .inclusiveThreshold();

        return new InitialDimensionResult(
                dimension.code(),
                rawScore,
                questionnairePreference,
                ambiguous
        );
    }

    private Map<QuestionId, Integer> validateCompleteResponse(
            AssessmentSpecification specification,
            QuestionnaireResponse response
    ) {
        var questionnaire =
                specification.questionnaire();

        if (
                response.answers().size()
                        != questionnaire.questions().size()
        ) {
            throw new IllegalArgumentException(
                    "questionnaire response is incomplete"
            );
        }

        Set<Integer> allowedValues =
                questionnaire
                        .answerScale()
                        .stream()
                        .map(
                                AnswerOption::value
                        )
                        .collect(
                                HashSet::new,
                                HashSet::add,
                                HashSet::addAll
                        );

        Map<QuestionId, QuestionDefinition>
                questionsById =
                new HashMap<>();

        for (
                QuestionDefinition question
                : questionnaire.questions()
        ) {
            questionsById.put(
                    question.questionId(),
                    question
            );
        }

        Map<QuestionId, Integer> answersByQuestionId =
                new HashMap<>();

        for (
                Answer answer
                : response.answers()
        ) {
            QuestionDefinition question =
                    questionsById.get(
                            answer.questionId()
                    );

            if (question == null) {
                throw new IllegalArgumentException(
                        "unknown question in questionnaire response: "
                                + answer
                                .questionId()
                                .value()
                );
            }

            if (
                    !allowedValues.contains(
                            answer.value()
                    )
            ) {
                throw new IllegalArgumentException(
                        "invalid answer value for question: "
                                + answer
                                .questionId()
                                .value()
                );
            }

            Integer previous =
                    answersByQuestionId.put(
                            answer.questionId(),
                            answer.value()
                    );

            if (previous != null) {
                throw new IllegalArgumentException(
                        "duplicate answer for question: "
                                + answer
                                .questionId()
                                .value()
                );
            }
        }

        for (
                QuestionDefinition question
                : questionnaire.questions()
        ) {
            if (
                    !answersByQuestionId.containsKey(
                            question.questionId()
                    )
            ) {
                throw new IllegalArgumentException(
                        "missing answer for question: "
                                + question
                                .questionId()
                                .value()
                );
            }
        }

        return Map.copyOf(
                answersByQuestionId
        );
    }

    private void validateSupportedPolicies(
            AssessmentSpecification specification
    ) {
        if (
                specification
                        .scoringPolicy()
                        .type()
                        != ScoringPolicyType
                        .CENTERED_BALANCED_KEYING
        ) {
            throw new UnsupportedOperationException(
                    "unsupported scoring policy: "
                            + specification
                            .scoringPolicy()
                            .type()
            );
        }

        if (
                specification
                        .ambiguityPolicy()
                        .type()
                        != AmbiguityPolicyType
                        .ABSOLUTE_RAW_SCORE_THRESHOLD
        ) {
            throw new UnsupportedOperationException(
                    "unsupported ambiguity policy: "
                            + specification
                            .ambiguityPolicy()
                            .type()
            );
        }
    }
}
