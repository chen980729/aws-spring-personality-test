package dev.springawsportfolio.portfolio.assessment.infrastructure.ai.bedrock;

import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeMessage;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiRequest;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.model.Tool;
import software.amazon.awssdk.services.bedrockruntime.model.ToolChoice;
import software.amazon.awssdk.services.bedrockruntime.model.ToolConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.ToolInputSchema;
import software.amazon.awssdk.services.bedrockruntime.model.ToolSpecification;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BedrockClarificationToolContract {

    static final String ASK_FOLLOWUP =
            "ask_followup";

    static final String SUBMIT_JUDGEMENT =
            "submit_judgement";

    private final boolean strictToolSchema;

    public BedrockClarificationToolContract(
            boolean strictToolSchema
    ) {
        this.strictToolSchema =
                strictToolSchema;
    }

    public ToolConfiguration create(
            ClarificationAiRequest request
    ) {
        Objects.requireNonNull(
                request,
                "request must not be null"
        );

        List<Tool> tools =
                request.finalModelTurn()
                        ? List.of(
                                submitJudgementTool(
                                        request
                                )
                        )
                        : List.of(
                                askFollowUpTool(),
                                submitJudgementTool(
                                        request
                                )
                        );

        return ToolConfiguration
                .builder()
                .tools(
                        tools
                )
                .toolChoice(
                        ToolChoice.fromAny(
                                ignored -> {
                                }
                        )
                )
                .build();
    }

    private Tool askFollowUpTool() {
        Map<String, Document> questionSchema =
                new LinkedHashMap<>();

        questionSchema.put(
                "type",
                Document.fromString(
                        "string"
                )
        );

        questionSchema.put(
                "minLength",
                Document.fromNumber(
                        1
                )
        );

        questionSchema.put(
                "maxLength",
                Document.fromNumber(
                        ClarificationRuntimeMessage
                                .MAX_ASSISTANT_QUESTION_CODE_POINTS
                )
        );

        Map<String, Document> properties =
                new LinkedHashMap<>();

        properties.put(
                "question",
                Document.fromMap(
                        questionSchema
                )
        );

        return tool(
                ASK_FOLLOWUP,
                "Ask exactly one concise, neutral follow-up "
                        + "question when more evidence is needed.",
                objectSchema(
                        properties,
                        List.of(
                                "question"
                        )
                )
        );
    }

    private Tool submitJudgementTool(
            ClarificationAiRequest request
    ) {
        Map<String, Document> properties =
                new LinkedHashMap<>();

        properties.put(
                "outcome",
                enumStringSchema(
                        List.of(
                                "RESOLVED",
                                "UNCLEAR"
                        )
                )
        );

        properties.put(
                "suggestedPole",
                enumStringSchema(
                        List.of(
                                request
                                        .targetDimension()
                                        .poleA()
                                        .value(),
                                request
                                        .targetDimension()
                                        .poleB()
                                        .value()
                        )
                )
        );

        properties.put(
                "confidence",
                enumStringSchema(
                        List.of(
                                "LOW",
                                "MEDIUM",
                                "HIGH"
                        )
                )
        );

        Map<String, Document> reasoningSchema =
                new LinkedHashMap<>();

        reasoningSchema.put(
                "type",
                Document.fromString(
                        "string"
                )
        );

        reasoningSchema.put(
                "minLength",
                Document.fromNumber(
                        1
                )
        );

        properties.put(
                "reasoningSummary",
                Document.fromMap(
                        reasoningSchema
                )
        );

        return tool(
                SUBMIT_JUDGEMENT,
                "Submit the bounded result for the target "
                        + "dimension. RESOLVED requires one of "
                        + "the target poles. UNCLEAR must not "
                        + "include suggestedPole.",
                objectSchema(
                        properties,
                        List.of(
                                "outcome",
                                "confidence"
                        )
                )
        );
    }

    private Tool tool(
            String name,
            String description,
            Document schema
    ) {
        ToolSpecification.Builder specification =
                ToolSpecification
                        .builder()
                        .name(
                                name
                        )
                        .description(
                                description
                        )
                        .inputSchema(
                                ToolInputSchema.fromJson(
                                        schema
                                )
                        );

        if (strictToolSchema) {
            specification.strict(
                    true
            );
        }

        return Tool.fromToolSpec(
                specification.build()
        );
    }

    private static Document objectSchema(
            Map<String, Document> properties,
            List<String> required
    ) {
        Map<String, Document> schema =
                new LinkedHashMap<>();

        schema.put(
                "type",
                Document.fromString(
                        "object"
                )
        );

        schema.put(
                "properties",
                Document.fromMap(
                        properties
                )
        );

        schema.put(
                "required",
                Document.fromList(
                        required
                                .stream()
                                .map(
                                        Document::fromString
                                )
                                .toList()
                )
        );

        schema.put(
                "additionalProperties",
                Document.fromBoolean(
                        false
                )
        );

        return Document.fromMap(
                schema
        );
    }

    private static Document enumStringSchema(
            List<String> values
    ) {
        Map<String, Document> schema =
                new LinkedHashMap<>();

        schema.put(
                "type",
                Document.fromString(
                        "string"
                )
        );

        schema.put(
                "enum",
                Document.fromList(
                        values
                                .stream()
                                .map(
                                        Document::fromString
                                )
                                .toList()
                )
        );

        return Document.fromMap(
                schema
        );
    }
}
