package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.mapper;

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
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.json.AssessmentSpecificationJson;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.json.AssessmentSpecificationJson.AmbiguityPolicyJson;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.json.AssessmentSpecificationJson.AnswerOptionJson;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.json.AssessmentSpecificationJson.ClarificationPolicyJson;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.json.AssessmentSpecificationJson.DimensionJson;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.json.AssessmentSpecificationJson.FinalizationPolicyJson;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.json.AssessmentSpecificationJson.QuestionJson;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.json.AssessmentSpecificationJson.QuestionnaireJson;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.json.AssessmentSpecificationJson.ScoringPolicyJson;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Objects;

@Component
public final class AssessmentSpecificationPersistenceMapper {

    private final JsonMapper jsonMapper;

    public AssessmentSpecificationPersistenceMapper(
            JsonMapper jsonMapper
    ) {
        this.jsonMapper = Objects.requireNonNull(
                jsonMapper,
                "jsonMapper must not be null"
        );
    }

    public AssessmentSpecification toDomain(
            JsonNode specificationNode
    ) {
        Objects.requireNonNull(
                specificationNode,
                "specificationNode must not be null"
        );

        AssessmentSpecificationJson json =
                jsonMapper.convertValue(
                        specificationNode,
                        AssessmentSpecificationJson.class
                );

        Objects.requireNonNull(
                json,
                "specification JSON must not be null"
        );

        ScoringPolicy scoringPolicy =
                mapScoringPolicy(
                        required(
                                json.scoringPolicy(),
                                "scoringPolicy"
                        )
                );

        validatePersistedMaximumAbsoluteRawScore(
                json.scoringPolicy(),
                scoringPolicy
        );

        return new AssessmentSpecification(
                mapDimensions(
                        required(
                                json.dimensions(),
                                "dimensions"
                        )
                ),
                mapQuestionnaire(
                        required(
                                json.questionnaire(),
                                "questionnaire"
                        )
                ),
                scoringPolicy,
                mapAmbiguityPolicy(
                        required(
                                json.ambiguityPolicy(),
                                "ambiguityPolicy"
                        )
                ),
                mapClarificationPolicy(
                        required(
                                json.clarificationPolicy(),
                                "clarificationPolicy"
                        )
                ),
                mapFinalizationPolicy(
                        required(
                                json.finalizationPolicy(),
                                "finalizationPolicy"
                        )
                )
        );
    }

    private List<DimensionDefinition> mapDimensions(
            List<DimensionJson> dimensions
    ) {
        return dimensions.stream()
                .map(this::mapDimension)
                .toList();
    }

    private DimensionDefinition mapDimension(
            DimensionJson json
    ) {
        required(json, "dimension");

        return new DimensionDefinition(
                new DimensionCode(
                        required(
                                json.code(),
                                "dimension.code"
                        )
                ),
                required(
                        json.position(),
                        "dimension.position"
                ),
                new PoleCode(
                        required(
                                json.poleA(),
                                "dimension.poleA"
                        )
                ),
                new PoleCode(
                        required(
                                json.poleB(),
                                "dimension.poleB"
                        )
                )
        );
    }

    private QuestionnaireDefinition mapQuestionnaire(
            QuestionnaireJson json
    ) {
        List<AnswerOption> answerScale =
                required(
                        json.answerScale(),
                        "questionnaire.answerScale"
                )
                        .stream()
                        .map(this::mapAnswerOption)
                        .toList();

        List<QuestionDefinition> questions =
                required(
                        json.questions(),
                        "questionnaire.questions"
                )
                        .stream()
                        .map(this::mapQuestion)
                        .toList();

        return new QuestionnaireDefinition(
                answerScale,
                questions
        );
    }

    private AnswerOption mapAnswerOption(
            AnswerOptionJson json
    ) {
        required(json, "answerOption");

        return new AnswerOption(
                required(
                        json.value(),
                        "answerOption.value"
                ),
                required(
                        json.label(),
                        "answerOption.label"
                )
        );
    }

    private QuestionDefinition mapQuestion(
            QuestionJson json
    ) {
        required(json, "question");

        return new QuestionDefinition(
                new QuestionId(
                        required(
                                json.questionId(),
                                "question.questionId"
                        )
                ),
                required(
                        json.position(),
                        "question.position"
                ),
                required(
                        json.prompt(),
                        "question.prompt"
                ),
                new DimensionCode(
                        required(
                                json.dimension(),
                                "question.dimension"
                        )
                ),
                new PoleCode(
                        required(
                                json.keyedPole(),
                                "question.keyedPole"
                        )
                )
        );
    }

    private ScoringPolicy mapScoringPolicy(
            ScoringPolicyJson json
    ) {
        return new ScoringPolicy(
                enumValue(
                        ScoringPolicyType.class,
                        json.type(),
                        "scoringPolicy.type"
                ),
                required(
                        json.revision(),
                        "scoringPolicy.revision"
                ),
                required(
                        json.answerCenter(),
                        "scoringPolicy.answerCenter"
                ),
                required(
                        json.minimumAnswer(),
                        "scoringPolicy.minimumAnswer"
                ),
                required(
                        json.maximumAnswer(),
                        "scoringPolicy.maximumAnswer"
                ),
                required(
                        json.itemsPerDimension(),
                        "scoringPolicy.itemsPerDimension"
                )
        );
    }

    private AmbiguityPolicy mapAmbiguityPolicy(
            AmbiguityPolicyJson json
    ) {
        return new AmbiguityPolicy(
                enumValue(
                        AmbiguityPolicyType.class,
                        json.type(),
                        "ambiguityPolicy.type"
                ),
                required(
                        json.revision(),
                        "ambiguityPolicy.revision"
                ),
                required(
                        json.inclusiveThreshold(),
                        "ambiguityPolicy.inclusiveThreshold"
                )
        );
    }

    private ClarificationPolicy mapClarificationPolicy(
            ClarificationPolicyJson json
    ) {
        return new ClarificationPolicy(
                enumValue(
                        ClarificationPolicyType.class,
                        json.type(),
                        "clarificationPolicy.type"
                ),
                required(
                        json.revision(),
                        "clarificationPolicy.revision"
                )
        );
    }

    private FinalizationPolicy mapFinalizationPolicy(
            FinalizationPolicyJson json
    ) {
        return new FinalizationPolicy(
                enumValue(
                        FinalizationPolicyType.class,
                        json.type(),
                        "finalizationPolicy.type"
                ),
                required(
                        json.revision(),
                        "finalizationPolicy.revision"
                )
        );
    }

    private void validatePersistedMaximumAbsoluteRawScore(
            ScoringPolicyJson json,
            ScoringPolicy policy
    ) {
        Integer persistedValue =
                json.maximumAbsoluteRawScore();

        if (persistedValue == null) {
            return;
        }

        int derivedValue =
                policy.maximumAbsoluteRawScore();

        if (persistedValue != derivedValue) {
            throw new IllegalArgumentException(
                    "scoringPolicy.maximumAbsoluteRawScore "
                            + "does not match derived value: "
                            + derivedValue
            );
        }
    }

    private static <T> T required(
            T value,
            String fieldName
    ) {
        return Objects.requireNonNull(
                value,
                fieldName + " must not be null"
        );
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> enumType,
            String value,
            String fieldName
    ) {
        String requiredValue =
                required(
                        value,
                        fieldName
                );

        try {
            return Enum.valueOf(
                    enumType,
                    requiredValue
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "unsupported "
                            + fieldName
                            + ": "
                            + requiredValue,
                    exception
            );
        }
    }
}
