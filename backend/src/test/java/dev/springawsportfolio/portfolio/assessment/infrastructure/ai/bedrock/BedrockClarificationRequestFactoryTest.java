package dev.springawsportfolio.portfolio.assessment.infrastructure.ai.bedrock;

import dev.springawsportfolio.portfolio.assessment.application.clarification.prompt.ClarificationPromptPolicyRegistry;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiRequest;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiTechnicalException;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockClarificationRequestFactoryTest {

    @Test
    void buildsBoundedFirstTurnWithForcedTools() {
        BedrockClarificationProperties properties =
                properties(
                        false
                );

        BedrockClarificationRequestFactory factory =
                new BedrockClarificationRequestFactory(
                        properties,
                        new BedrockClarificationToolContract(
                                false
                        ),
                        new ClarificationPromptPolicyRegistry()
                );

        ConverseRequest request =
                factory.create(
                        firstTurnRequest()
                );

        assertEquals(
                "test-model",
                request.modelId()
        );

        assertEquals(
                512,
                request
                        .inferenceConfig()
                        .maxTokens()
        );

        assertNotNull(
                request
                        .toolConfig()
                        .toolChoice()
                        .any()
        );

        Set<String> toolNames =
                request
                        .toolConfig()
                        .tools()
                        .stream()
                        .map(
                                tool ->
                                        tool
                                                .toolSpec()
                                                .name()
                        )
                        .collect(
                                Collectors.toSet()
                        );

        assertEquals(
                Set.of(
                        BedrockClarificationToolContract.ASK_FOLLOWUP,
                        BedrockClarificationToolContract.SUBMIT_JUDGEMENT
                ),
                toolNames
        );

        assertEquals(
                1,
                request.messages().size()
        );

        String systemPrompt =
                request
                        .system()
                        .getFirst()
                        .text();

        assertTrue(
                systemPrompt.contains(
                        "untrusted evidence"
                )
        );

        assertTrue(
                systemPrompt.contains(
                        "Target dimension: EI"
                )
        );

        assertTrue(
                systemPrompt.contains(
                        "Clarification policy revision: v1"
                )
        );

        assertTrue(
                systemPrompt.contains(
                        "I feel energized after social interaction."
                )
        );

        assertTrue(
                systemPrompt.contains(
                        "Answer: Agree"
                )
        );

        assertTrue(
                systemPrompt.contains(
                        "Extraversion"
                )
        );

        assertTrue(
                systemPrompt.contains(
                        "Introversion"
                )
        );
    }

    @Test
    void rejectsUnsupportedClarificationPolicyRevision() {
        BedrockClarificationRequestFactory factory =
                new BedrockClarificationRequestFactory(
                        properties(
                                false
                        ),
                        new BedrockClarificationToolContract(
                                false
                        ),
                        new ClarificationPromptPolicyRegistry()
                );

        ClarificationAiRequest unsupported =
                new ClarificationAiRequest(
                        targetDimension(),
                        List.of(
                                evidence()
                        ),
                        List.of(),
                        1,
                        3,
                        "v999"
                );

        assertThrows(
                ClarificationAiTechnicalException.class,
                () -> factory.create(
                        unsupported
                )
        );
    }

    @Test
    void finalTurnExposesOnlyJudgementTool() {
        BedrockClarificationRequestFactory factory =
                new BedrockClarificationRequestFactory(
                        properties(
                                false
                        ),
                        new BedrockClarificationToolContract(
                                false
                        ),
                        new ClarificationPromptPolicyRegistry()
                );

        ConverseRequest request =
                factory.create(
                        finalTurnRequest()
                );

        assertEquals(
                1,
                request
                        .toolConfig()
                        .tools()
                        .size()
        );

        assertEquals(
                BedrockClarificationToolContract.SUBMIT_JUDGEMENT,
                request
                        .toolConfig()
                        .tools()
                        .getFirst()
                        .toolSpec()
                        .name()
        );

        assertEquals(
                5,
                request.messages().size()
        );

        assertTrue(
                request
                        .system()
                        .getFirst()
                        .text()
                        .contains(
                                "final allowed model turn"
                        )
        );
    }

    @Test
    void strictSchemaCanBeEnabledWithoutChangingPortContract() {
        BedrockClarificationRequestFactory factory =
                new BedrockClarificationRequestFactory(
                        properties(
                                true
                        ),
                        new BedrockClarificationToolContract(
                                true
                        ),
                        new ClarificationPromptPolicyRegistry()
                );

        ConverseRequest request =
                factory.create(
                        firstTurnRequest()
                );

        assertTrue(
                request
                        .toolConfig()
                        .tools()
                        .stream()
                        .allMatch(
                                tool ->
                                        Boolean.TRUE.equals(
                                                tool
                                                        .toolSpec()
                                                        .strict()
                                        )
                        )
        );
    }

    private BedrockClarificationProperties properties(
            boolean strictToolSchema
    ) {
        return new BedrockClarificationProperties(
                true,
                "ap-northeast-1",
                "test-model",
                Duration.ofSeconds(20),
                Duration.ofSeconds(18),
                512,
                strictToolSchema
        );
    }

    private ClarificationAiRequest firstTurnRequest() {
        return new ClarificationAiRequest(
                targetDimension(),
                List.of(
                        evidence()
                ),
                List.of(),
                1,
                3,
                "v1"
        );
    }

    private ClarificationAiRequest finalTurnRequest() {
        return new ClarificationAiRequest(
                targetDimension(),
                List.of(
                        evidence()
                ),
                List.of(
                        new ClarificationAiRequest.ConversationMessage(
                                ClarificationAiRequest.ConversationRole
                                        .ASSISTANT_QUESTION,
                                "How do you usually recharge?"
                        ),
                        new ClarificationAiRequest.ConversationMessage(
                                ClarificationAiRequest.ConversationRole
                                        .USER_ANSWER,
                                "Usually by spending time alone."
                        ),
                        new ClarificationAiRequest.ConversationMessage(
                                ClarificationAiRequest.ConversationRole
                                        .ASSISTANT_QUESTION,
                                "What about after a social weekend?"
                        ),
                        new ClarificationAiRequest.ConversationMessage(
                                ClarificationAiRequest.ConversationRole
                                        .USER_ANSWER,
                                "I want a quiet day afterwards."
                        )
                ),
                3,
                3,
                "v1"
        );
    }

    private ClarificationAiRequest.TargetDimension
    targetDimension() {
        return new ClarificationAiRequest.TargetDimension(
                new DimensionCode(
                        "EI"
                ),
                new PoleCode(
                        "E"
                ),
                new PoleCode(
                        "I"
                )
        );
    }

    private ClarificationAiRequest.QuestionnaireEvidence
    evidence() {
        return new ClarificationAiRequest.QuestionnaireEvidence(
                "I feel energized after social interaction.",
                "Agree",
                new PoleCode(
                        "E"
                )
        );
    }
}
