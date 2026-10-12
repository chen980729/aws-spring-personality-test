package dev.springawsportfolio.portfolio.assessment.infrastructure.ai.bedrock;

import dev.springawsportfolio.portfolio.assessment.application.clarification.prompt.ClarificationPromptPolicy;
import dev.springawsportfolio.portfolio.assessment.application.clarification.prompt.ClarificationPromptPolicyRegistry;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BedrockClarificationRequestFactory {

    private static final String INITIAL_USER_MESSAGE =
            "Begin the bounded clarification task using the "
                    + "questionnaire evidence supplied in the "
                    + "system instructions.";

    private static final String NL =
            System.lineSeparator();

    private final BedrockClarificationProperties properties;

    private final BedrockClarificationToolContract
            toolContract;

    private final ClarificationPromptPolicyRegistry
            promptPolicyRegistry;

    public BedrockClarificationRequestFactory(
            BedrockClarificationProperties properties,
            BedrockClarificationToolContract toolContract,
            ClarificationPromptPolicyRegistry promptPolicyRegistry
    ) {
        this.properties =
                Objects.requireNonNull(
                        properties,
                        "properties must not be null"
                );

        this.toolContract =
                Objects.requireNonNull(
                        toolContract,
                        "toolContract must not be null"
                );

        this.promptPolicyRegistry =
                Objects.requireNonNull(
                        promptPolicyRegistry,
                        "promptPolicyRegistry must not be null"
                );
    }

    public ConverseRequest create(
            ClarificationAiRequest request
    ) {
        Objects.requireNonNull(
                request,
                "request must not be null"
        );

        ClarificationPromptPolicy promptPolicy =
                promptPolicyRegistry.require(
                        request.clarificationPolicyRevision()
                );

        return ConverseRequest
                .builder()
                .modelId(
                        properties.modelId()
                )
                .system(
                        SystemContentBlock.fromText(
                                systemPrompt(
                                        request,
                                        promptPolicy
                                )
                        )
                )
                .messages(
                        messages(
                                request
                        )
                )
                .inferenceConfig(
                        inference ->
                                inference.maxTokens(
                                        properties.maxTokens()
                                )
                )
                .toolConfig(
                        toolContract.create(
                                request
                        )
                )
                .build();
    }

    private String systemPrompt(
            ClarificationAiRequest request,
            ClarificationPromptPolicy promptPolicy
    ) {
        ClarificationPromptPolicy.DimensionSemantics
                dimensionSemantics =
                promptPolicy.requireDimension(
                        request
                                .targetDimension()
                                .code()
                );

        ClarificationPromptPolicy.PoleSemantics
                poleASemantics =
                dimensionSemantics.requirePole(
                        request
                                .targetDimension()
                                .poleA()
                );

        ClarificationPromptPolicy.PoleSemantics
                poleBSemantics =
                dimensionSemantics.requirePole(
                        request
                                .targetDimension()
                                .poleB()
                );

        StringBuilder prompt =
                new StringBuilder(
                        promptPolicy.baseInstructions()
                );

        if (request.finalModelTurn()) {
            prompt.append(
                    NL
            ).append(
                    promptPolicy.finalTurnInstructions()
            );
        }

        prompt.append(
                NL
        ).append(
                "Clarification policy revision: "
        ).append(
                promptPolicy.revision()
        );

        prompt.append(
                NL
        ).append(
                "Model turn: "
        ).append(
                request.currentModelTurn()
        ).append(
                " of "
        ).append(
                request.maximumModelTurns()
        );

        prompt.append(
                NL
        ).append(
                "Target dimension: "
        ).append(
                request
                        .targetDimension()
                        .code()
                        .value()
        ).append(
                " — "
        ).append(
                dimensionSemantics.label()
        );

        appendPoleSemantics(
                prompt,
                "Pole A",
                poleASemantics
        );

        appendPoleSemantics(
                prompt,
                "Pole B",
                poleBSemantics
        );

        prompt.append(
                NL
        ).append(
                NL
        ).append(
                "<QuestionnaireEvidence>"
        );

        int index = 1;

        for (
                ClarificationAiRequest.QuestionnaireEvidence evidence
                : request.questionnaireEvidence()
        ) {
            prompt.append(
                    NL
            ).append(
                    "["
            ).append(
                    index++
            ).append(
                    "] Question: "
            ).append(
                    evidence.questionText()
            ).append(
                    NL
            ).append(
                    "Answer: "
            ).append(
                    evidence.answerLabel()
            ).append(
                    NL
            ).append(
                    "Keyed pole: "
            ).append(
                    evidence.keyedPole().value()
            );
        }

        prompt.append(
                NL
        ).append(
                "</QuestionnaireEvidence>"
        );

        return prompt.toString();
    }

    private static void appendPoleSemantics(
            StringBuilder prompt,
            String heading,
            ClarificationPromptPolicy.PoleSemantics semantics
    ) {
        prompt.append(
                NL
        ).append(
                heading
        ).append(
                ": "
        ).append(
                semantics.code().value()
        ).append(
                " — "
        ).append(
                semantics.label()
        ).append(
                ": "
        ).append(
                semantics.neutralDescription()
        );
    }

    private List<Message> messages(
            ClarificationAiRequest request
    ) {
        List<Message> messages =
                new ArrayList<>();

        messages.add(
                textMessage(
                        ConversationRole.USER,
                        INITIAL_USER_MESSAGE
                )
        );

        for (
                ClarificationAiRequest.ConversationMessage message
                : request.conversation()
        ) {
            ConversationRole role =
                    message.role()
                            == ClarificationAiRequest
                            .ConversationRole
                            .ASSISTANT_QUESTION
                            ? ConversationRole.ASSISTANT
                            : ConversationRole.USER;

            messages.add(
                    textMessage(
                            role,
                            message.text()
                    )
            );
        }

        return List.copyOf(
                messages
        );
    }

    private static Message textMessage(
            ConversationRole role,
            String text
    ) {
        return Message
                .builder()
                .role(
                        role
                )
                .content(
                        ContentBlock.fromText(
                                text
                        )
                )
                .build();
    }
}
