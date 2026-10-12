package dev.springawsportfolio.portfolio.assessment.infrastructure.ai.bedrock;

import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiRequest;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiTechnicalException;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiTurn;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationConfidence;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResolution;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.StopReason;
import software.amazon.awssdk.services.bedrockruntime.model.ToolUseBlock;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class BedrockClarificationResponseParser {

    private static final String PROVIDER =
            "AMAZON_BEDROCK";

    private static final Set<String> ASK_FOLLOWUP_FIELDS =
            Set.of(
                    "question"
            );

    private static final Set<String> JUDGEMENT_FIELDS =
            Set.of(
                    "outcome",
                    "suggestedPole",
                    "confidence",
                    "reasoningSummary"
            );

    private final BedrockClarificationProperties properties;

    public BedrockClarificationResponseParser(
            BedrockClarificationProperties properties
    ) {
        this.properties =
                Objects.requireNonNull(
                        properties,
                        "properties must not be null"
                );
    }

    public ClarificationAiTurn parse(
            ConverseResponse response,
            ClarificationAiRequest request
    ) {
        Objects.requireNonNull(
                response,
                "response must not be null"
        );

        Objects.requireNonNull(
                request,
                "request must not be null"
        );

        if (
                response.stopReason()
                        != StopReason.TOOL_USE
        ) {
            throw invalid(
                    "Bedrock response did not stop for tool use: "
                            + response.stopReasonAsString()
            );
        }

        if (
                response.output() == null
                        || response
                        .output()
                        .message() == null
        ) {
            throw invalid(
                    "Bedrock response is missing output message"
            );
        }

        Message message =
                response
                        .output()
                        .message();

        if (
                message.role()
                        != ConversationRole.ASSISTANT
        ) {
            throw invalid(
                    "Bedrock output message must be assistant role"
            );
        }

        List<ToolUseBlock> toolUses =
                message
                        .content()
                        .stream()
                        .map(
                                ContentBlock::toolUse
                        )
                        .filter(
                                Objects::nonNull
                        )
                        .toList();

        if (toolUses.size() != 1) {
            throw invalid(
                    "Bedrock response must contain exactly one tool use"
            );
        }

        ToolUseBlock toolUse =
                toolUses.getFirst();

        return switch (toolUse.name()) {
            case BedrockClarificationToolContract.ASK_FOLLOWUP ->
                    parseFollowUp(
                            toolUse.input(),
                            request
                    );

            case BedrockClarificationToolContract.SUBMIT_JUDGEMENT ->
                    parseJudgement(
                            toolUse.input(),
                            request
                    );

            default ->
                    throw invalid(
                            "Bedrock requested unsupported tool: "
                                    + toolUse.name()
                    );
        };
    }

    private ClarificationAiTurn parseFollowUp(
            Document input,
            ClarificationAiRequest request
    ) {
        if (request.finalModelTurn()) {
            throw invalid(
                    "follow-up tool is not allowed on final model turn"
            );
        }

        Map<String, Document> fields =
                requireObject(
                        input,
                        BedrockClarificationToolContract.ASK_FOLLOWUP
                );

        requireOnlyFields(
                fields,
                ASK_FOLLOWUP_FIELDS,
                BedrockClarificationToolContract.ASK_FOLLOWUP
        );

        return new ClarificationAiTurn.FollowUpQuestion(
                requireNonBlankString(
                        fields,
                        "question"
                )
        );
    }

    private ClarificationAiTurn parseJudgement(
            Document input,
            ClarificationAiRequest request
    ) {
        Map<String, Document> fields =
                requireObject(
                        input,
                        BedrockClarificationToolContract.SUBMIT_JUDGEMENT
                );

        requireOnlyFields(
                fields,
                JUDGEMENT_FIELDS,
                BedrockClarificationToolContract.SUBMIT_JUDGEMENT
        );

        ClarificationResolution resolution =
                parseResolution(
                        requireNonBlankString(
                                fields,
                                "outcome"
                        )
                );

        ClarificationConfidence confidence =
                parseConfidence(
                        requireNonBlankString(
                                fields,
                                "confidence"
                        )
                );

        String reasoningSummary =
                optionalNonBlankString(
                        fields,
                        "reasoningSummary"
                );

        PoleCode suggestedPole =
                null;

        if (
                resolution
                        == ClarificationResolution.RESOLVED
        ) {
            suggestedPole =
                    new PoleCode(
                            requireNonBlankString(
                                    fields,
                                    "suggestedPole"
                            )
                    );

            if (
                    !request
                            .targetDimension()
                            .containsPole(
                                    suggestedPole
                            )
            ) {
                throw invalid(
                        "suggestedPole does not belong to target dimension"
                );
            }
        } else if (
                fields.containsKey(
                        "suggestedPole"
                )
        ) {
            throw invalid(
                    "UNCLEAR judgement must not include suggestedPole"
            );
        }

        return new ClarificationAiTurn.Judgement(
                resolution,
                suggestedPole,
                confidence,
                reasoningSummary,
                new ClarificationAiTurn.ModelIdentity(
                        PROVIDER,
                        properties.modelId()
                )
        );
    }

    private static Map<String, Document> requireObject(
            Document input,
            String toolName
    ) {
        if (
                input == null
                        || !input.isMap()
        ) {
            throw invalid(
                    toolName
                            + " input must be a JSON object"
            );
        }

        return input.asMap();
    }

    private static void requireOnlyFields(
            Map<String, Document> fields,
            Set<String> allowedFields,
            String toolName
    ) {
        for (String field : fields.keySet()) {
            if (!allowedFields.contains(field)) {
                throw invalid(
                        toolName
                                + " contains unsupported field: "
                                + field
                );
            }
        }
    }

    private static String requireNonBlankString(
            Map<String, Document> fields,
            String fieldName
    ) {
        Document value =
                fields.get(
                        fieldName
                );

        if (
                value == null
                        || !value.isString()
        ) {
            throw invalid(
                    fieldName
                            + " must be a string"
            );
        }

        String text =
                value
                        .asString()
                        .trim();

        if (text.isEmpty()) {
            throw invalid(
                    fieldName
                            + " must not be blank"
            );
        }

        return text;
    }

    private static String optionalNonBlankString(
            Map<String, Document> fields,
            String fieldName
    ) {
        if (!fields.containsKey(fieldName)) {
            return null;
        }

        return requireNonBlankString(
                fields,
                fieldName
        );
    }

    private static ClarificationResolution parseResolution(
            String value
    ) {
        return switch (value) {
            case "RESOLVED" ->
                    ClarificationResolution.RESOLVED;
            case "UNCLEAR" ->
                    ClarificationResolution.UNCLEAR;
            default ->
                    throw invalid(
                            "unsupported outcome: "
                                    + value
                    );
        };
    }

    private static ClarificationConfidence parseConfidence(
            String value
    ) {
        return switch (value) {
            case "LOW" ->
                    ClarificationConfidence.LOW;
            case "MEDIUM" ->
                    ClarificationConfidence.MEDIUM;
            case "HIGH" ->
                    ClarificationConfidence.HIGH;
            default ->
                    throw invalid(
                            "unsupported confidence: "
                                    + value
                    );
        };
    }

    private static ClarificationAiTechnicalException invalid(
            String message
    ) {
        return new ClarificationAiTechnicalException(
                message
        );
    }
}
