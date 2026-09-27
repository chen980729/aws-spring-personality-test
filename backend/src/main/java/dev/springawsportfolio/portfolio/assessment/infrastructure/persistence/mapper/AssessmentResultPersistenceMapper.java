package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.mapper;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDecisionSource;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDimensionConclusion;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.json.AssessmentResultJson;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Objects;

@Component
public final class AssessmentResultPersistenceMapper {

    private final JsonMapper jsonMapper;

    public AssessmentResultPersistenceMapper(
            JsonMapper jsonMapper
    ) {
        this.jsonMapper =
                Objects.requireNonNull(
                        jsonMapper,
                        "jsonMapper must not be null"
                );
    }

    public JsonNode toInitialJsonNode(
            InitialAssessmentResult result
    ) {
        if (result == null) {
            return null;
        }

        List<AssessmentResultJson.InitialDimensionJson>
                dimensions =
                result
                        .dimensions()
                        .stream()
                        .map(dimension ->
                                new AssessmentResultJson
                                        .InitialDimensionJson(
                                        dimension
                                                .dimension()
                                                .value(),
                                        dimension.rawScore(),
                                        dimension
                                                .questionnairePreference()
                                                == null
                                                ? null
                                                : dimension
                                                .questionnairePreference()
                                                .value(),
                                        dimension.ambiguous()
                                )
                        )
                        .toList();

        return jsonMapper.convertValue(
                new AssessmentResultJson
                        .InitialResultJson(
                        dimensions
                ),
                JsonNode.class
        );
    }

    public InitialAssessmentResult toInitialDomain(
            JsonNode node
    ) {
        if (node == null || node.isNull()) {
            return null;
        }

        AssessmentResultJson.InitialResultJson json =
                jsonMapper.convertValue(
                        node,
                        AssessmentResultJson
                                .InitialResultJson.class
                );

        Objects.requireNonNull(
                json,
                "initial result JSON must not be null"
        );

        List<AssessmentResultJson.InitialDimensionJson>
                persistedDimensions =
                Objects.requireNonNull(
                        json.dimensions(),
                        "initial result dimensions must not be null"
                );

        List<InitialDimensionResult> dimensions =
                persistedDimensions
                        .stream()
                        .map(this::toInitialDimension)
                        .toList();

        return new InitialAssessmentResult(
                dimensions
        );
    }

    public JsonNode toFinalJsonNode(
            FinalAssessmentResult result
    ) {
        if (result == null) {
            return null;
        }

        List<AssessmentResultJson.FinalDimensionJson>
                dimensions =
                result
                        .dimensions()
                        .stream()
                        .map(dimension ->
                                new AssessmentResultJson
                                        .FinalDimensionJson(
                                        dimension
                                                .dimension()
                                                .value(),
                                        dimension
                                                .finalPreference()
                                                .value(),
                                        dimension
                                                .decisionSource()
                                                .name()
                                )
                        )
                        .toList();

        return jsonMapper.convertValue(
                new AssessmentResultJson.FinalResultJson(
                        result.finalType(),
                        dimensions
                ),
                JsonNode.class
        );
    }

    public FinalAssessmentResult toFinalDomain(
            JsonNode node
    ) {
        if (node == null || node.isNull()) {
            return null;
        }

        AssessmentResultJson.FinalResultJson json =
                jsonMapper.convertValue(
                        node,
                        AssessmentResultJson
                                .FinalResultJson.class
                );

        Objects.requireNonNull(
                json,
                "final result JSON must not be null"
        );

        List<AssessmentResultJson.FinalDimensionJson>
                persistedDimensions =
                Objects.requireNonNull(
                        json.dimensions(),
                        "final result dimensions must not be null"
                );

        List<FinalDimensionConclusion> dimensions =
                persistedDimensions
                        .stream()
                        .map(this::toFinalDimension)
                        .toList();

        return new FinalAssessmentResult(
                Objects.requireNonNull(
                        json.finalType(),
                        "finalType must not be null"
                ),
                dimensions
        );
    }

    private InitialDimensionResult toInitialDimension(
            AssessmentResultJson.InitialDimensionJson json
    ) {
        Objects.requireNonNull(
                json,
                "initial dimension JSON must not be null"
        );

        String dimension =
                Objects.requireNonNull(
                        json.dimension(),
                        "initial dimension code must not be null"
                );

        PoleCode questionnairePreference =
                json.questionnairePreference() == null
                        ? null
                        : new PoleCode(
                        json.questionnairePreference()
                );

        return new InitialDimensionResult(
                new DimensionCode(
                        dimension
                ),
                json.rawScore(),
                questionnairePreference,
                json.ambiguous()
        );
    }

    private FinalDimensionConclusion toFinalDimension(
            AssessmentResultJson.FinalDimensionJson json
    ) {
        Objects.requireNonNull(
                json,
                "final dimension JSON must not be null"
        );

        return new FinalDimensionConclusion(
                new DimensionCode(
                        Objects.requireNonNull(
                                json.dimension(),
                                "final dimension code must not be null"
                        )
                ),
                new PoleCode(
                        Objects.requireNonNull(
                                json.finalPreference(),
                                "finalPreference must not be null"
                        )
                ),
                FinalDecisionSource.valueOf(
                        Objects.requireNonNull(
                                json.decisionSource(),
                                "decisionSource must not be null"
                        )
                )
        );
    }
}