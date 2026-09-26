package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.mapper;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.Answer;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.QuestionnaireResponse;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.json.QuestionnaireResponseJson;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Objects;

@Component
public final class QuestionnaireResponsePersistenceMapper {

    private final JsonMapper jsonMapper;

    public QuestionnaireResponsePersistenceMapper(
            JsonMapper jsonMapper
    ) {
        this.jsonMapper = Objects.requireNonNull(
                jsonMapper,
                "jsonMapper must not be null"
        );
    }

    public QuestionnaireResponse toDomain(
            JsonNode node
    ) {
        if (node == null || node.isNull()) {
            return QuestionnaireResponse.empty();
        }

        QuestionnaireResponseJson json =
                jsonMapper.convertValue(
                        node,
                        QuestionnaireResponseJson.class
                );

        Objects.requireNonNull(
                json,
                "questionnaire response JSON must not be null"
        );

        List<QuestionnaireResponseJson.AnswerJson>
                persistedAnswers =
                Objects.requireNonNull(
                        json.answers(),
                        "questionnaire response answers "
                                + "must not be null"
                );

        List<Answer> answers =
                persistedAnswers
                        .stream()
                        .map(this::mapAnswer)
                        .toList();

        return new QuestionnaireResponse(
                answers
        );
    }

    private Answer mapAnswer(
            QuestionnaireResponseJson.AnswerJson json
    ) {
        Objects.requireNonNull(
                json,
                "answer JSON must not be null"
        );

        String questionId =
                Objects.requireNonNull(
                        json.questionId(),
                        "answer.questionId must not be null"
                );

        Integer value =
                Objects.requireNonNull(
                        json.value(),
                        "answer.value must not be null"
                );

        return new Answer(
                new QuestionId(questionId),
                value
        );
    }
}
