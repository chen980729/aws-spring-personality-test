package dev.springawsportfolio.portfolio.assessment.infrastructure.ai.bedrock;

import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiRequest;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiTechnicalException;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiTurn;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationConfidence;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResolution;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.StopReason;
import software.amazon.awssdk.services.bedrockruntime.model.ToolUseBlock;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BedrockClarificationResponseParserTest {

    private final BedrockClarificationResponseParser parser =
            new BedrockClarificationResponseParser(
                    properties()
            );

    @Test
    void parsesFollowUpQuestion() {
        ClarificationAiTurn turn =
                parser.parse(
                        response(
                                BedrockClarificationToolContract.ASK_FOLLOWUP,
                                Document.fromMap(
                                        Map.of(
                                                "question",
                                                Document.fromString(
                                                        "How do you recharge?"
                                                )
                                        )
                                )
                        ),
                        firstTurnRequest()
                );

        ClarificationAiTurn.FollowUpQuestion followUp =
                assertInstanceOf(
                        ClarificationAiTurn.FollowUpQuestion.class,
                        turn
                );

        assertEquals(
                "How do you recharge?",
                followUp.text()
        );
    }

    @Test
    void parsesResolvedJudgementAndModelIdentity() {
        ClarificationAiTurn turn =
                parser.parse(
                        response(
                                BedrockClarificationToolContract.SUBMIT_JUDGEMENT,
                                Document.fromMap(
                                        Map.of(
                                                "outcome",
                                                Document.fromString(
                                                        "RESOLVED"
                                                ),
                                                "suggestedPole",
                                                Document.fromString(
                                                        "I"
                                                ),
                                                "confidence",
                                                Document.fromString(
                                                        "HIGH"
                                                ),
                                                "reasoningSummary",
                                                Document.fromString(
                                                        "The clarification consistently favors solitary recharge."
                                                )
                                        )
                                )
                        ),
                        firstTurnRequest()
                );

        ClarificationAiTurn.Judgement judgement =
                assertInstanceOf(
                        ClarificationAiTurn.Judgement.class,
                        turn
                );

        assertEquals(
                ClarificationResolution.RESOLVED,
                judgement.resolution()
        );

        assertEquals(
                new PoleCode(
                        "I"
                ),
                judgement.suggestedPole()
        );

        assertEquals(
                ClarificationConfidence.HIGH,
                judgement.confidence()
        );

        assertEquals(
                "AMAZON_BEDROCK",
                judgement
                        .modelIdentity()
                        .provider()
        );

        assertEquals(
                "test-model",
                judgement
                        .modelIdentity()
                        .modelIdentifier()
        );
    }

    @Test
    void parsesUnclearWithoutSuggestedPole() {
        ClarificationAiTurn turn =
                parser.parse(
                        response(
                                BedrockClarificationToolContract.SUBMIT_JUDGEMENT,
                                Document.fromMap(
                                        Map.of(
                                                "outcome",
                                                Document.fromString(
                                                        "UNCLEAR"
                                                ),
                                                "confidence",
                                                Document.fromString(
                                                        "LOW"
                                                )
                                        )
                                )
                        ),
                        firstTurnRequest()
                );

        ClarificationAiTurn.Judgement judgement =
                assertInstanceOf(
                        ClarificationAiTurn.Judgement.class,
                        turn
                );

        assertEquals(
                ClarificationResolution.UNCLEAR,
                judgement.resolution()
        );

        assertNull(
                judgement.suggestedPole()
        );

        assertNull(
                judgement.reasoningSummary()
        );
    }

    @Test
    void rejectsSuggestedPoleOutsideTargetDimension() {
        assertThrows(
                ClarificationAiTechnicalException.class,
                () -> parser.parse(
                        response(
                                BedrockClarificationToolContract.SUBMIT_JUDGEMENT,
                                Document.fromMap(
                                        Map.of(
                                                "outcome",
                                                Document.fromString(
                                                        "RESOLVED"
                                                ),
                                                "suggestedPole",
                                                Document.fromString(
                                                        "N"
                                                ),
                                                "confidence",
                                                Document.fromString(
                                                        "MEDIUM"
                                                )
                                        )
                                )
                        ),
                        firstTurnRequest()
                )
        );
    }

    @Test
    void rejectsFollowUpOnFinalTurn() {
        assertThrows(
                ClarificationAiTechnicalException.class,
                () -> parser.parse(
                        response(
                                BedrockClarificationToolContract.ASK_FOLLOWUP,
                                Document.fromMap(
                                        Map.of(
                                                "question",
                                                Document.fromString(
                                                        "One more question?"
                                                )
                                        )
                                )
                        ),
                        finalTurnRequest()
                )
        );
    }

    @Test
    void rejectsNonToolUseStopReason() {
        ConverseResponse response =
                ConverseResponse
                        .builder()
                        .stopReason(
                                StopReason.END_TURN
                        )
                        .output(
                                ConverseOutput
                                        .builder()
                                        .message(
                                                Message
                                                        .builder()
                                                        .role(
                                                                ConversationRole.ASSISTANT
                                                        )
                                                        .content(
                                                                ContentBlock.fromText(
                                                                        "free text"
                                                                )
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .build();

        assertThrows(
                ClarificationAiTechnicalException.class,
                () -> parser.parse(
                        response,
                        firstTurnRequest()
                )
        );
    }

    @Test
    void rejectsMultipleToolUses() {
        Message message =
                Message
                        .builder()
                        .role(
                                ConversationRole.ASSISTANT
                        )
                        .content(
                                toolUse(
                                        BedrockClarificationToolContract.ASK_FOLLOWUP,
                                        Document.fromMap(
                                                Map.of(
                                                        "question",
                                                        Document.fromString(
                                                                "First?"
                                                        )
                                                )
                                        ),
                                        "tool-1"
                                ),
                                toolUse(
                                        BedrockClarificationToolContract.ASK_FOLLOWUP,
                                        Document.fromMap(
                                                Map.of(
                                                        "question",
                                                        Document.fromString(
                                                                "Second?"
                                                        )
                                                )
                                        ),
                                        "tool-2"
                                )
                        )
                        .build();

        ConverseResponse response =
                ConverseResponse
                        .builder()
                        .stopReason(
                                StopReason.TOOL_USE
                        )
                        .output(
                                ConverseOutput
                                        .builder()
                                        .message(
                                                message
                                        )
                                        .build()
                        )
                        .build();

        assertThrows(
                ClarificationAiTechnicalException.class,
                () -> parser.parse(
                        response,
                        firstTurnRequest()
                )
        );
    }

    @Test
    void rejectsUnexpectedToolInputField() {
        assertThrows(
                ClarificationAiTechnicalException.class,
                () -> parser.parse(
                        response(
                                BedrockClarificationToolContract.ASK_FOLLOWUP,
                                Document.fromMap(
                                        Map.of(
                                                "question",
                                                Document.fromString(
                                                        "Valid question"
                                                ),
                                                "extra",
                                                Document.fromString(
                                                        "not allowed"
                                                )
                                        )
                                )
                        ),
                        firstTurnRequest()
                )
        );
    }

    private ConverseResponse response(
            String toolName,
            Document input
    ) {
        Message message =
                Message
                        .builder()
                        .role(
                                ConversationRole.ASSISTANT
                        )
                        .content(
                                toolUse(
                                        toolName,
                                        input,
                                        "tool-1"
                                )
                        )
                        .build();

        return ConverseResponse
                .builder()
                .stopReason(
                        StopReason.TOOL_USE
                )
                .output(
                        ConverseOutput
                                .builder()
                                .message(
                                        message
                                )
                                .build()
                )
                .build();
    }

    private ContentBlock toolUse(
            String toolName,
            Document input,
            String toolUseId
    ) {
        return ContentBlock.fromToolUse(
                ToolUseBlock
                        .builder()
                        .toolUseId(
                                toolUseId
                        )
                        .name(
                                toolName
                        )
                        .input(
                                input
                        )
                        .build()
        );
    }

    private BedrockClarificationProperties properties() {
        return new BedrockClarificationProperties(
                true,
                "ap-northeast-1",
                "test-model",
                Duration.ofSeconds(20),
                Duration.ofSeconds(18),
                512,
                false
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
                        conversation(
                                ClarificationAiRequest
                                        .ConversationRole
                                        .ASSISTANT_QUESTION,
                                "First question"
                        ),
                        conversation(
                                ClarificationAiRequest
                                        .ConversationRole
                                        .USER_ANSWER,
                                "First answer"
                        ),
                        conversation(
                                ClarificationAiRequest
                                        .ConversationRole
                                        .ASSISTANT_QUESTION,
                                "Second question"
                        ),
                        conversation(
                                ClarificationAiRequest
                                        .ConversationRole
                                        .USER_ANSWER,
                                "Second answer"
                        )
                ),
                3,
                3,
                "v1"
        );
    }

    private ClarificationAiRequest.ConversationMessage conversation(
            ClarificationAiRequest.ConversationRole role,
            String text
    ) {
        return new ClarificationAiRequest.ConversationMessage(
                role,
                text
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
