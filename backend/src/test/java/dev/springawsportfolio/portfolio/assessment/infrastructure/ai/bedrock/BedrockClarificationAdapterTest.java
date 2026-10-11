package dev.springawsportfolio.portfolio.assessment.infrastructure.ai.bedrock;

import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiRequest;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiTurn;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.StopReason;
import software.amazon.awssdk.services.bedrockruntime.model.ToolUseBlock;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BedrockClarificationAdapterTest {

    @Test
    void mapsProviderNeutralRequestThroughConverseAndBack() {
        BedrockClarificationProperties properties =
                new BedrockClarificationProperties(
                        true,
                        "ap-northeast-1",
                        "test-model",
                        Duration.ofSeconds(20),
                        Duration.ofSeconds(18),
                        512,
                        false
                );

        BedrockConverseTransport transport =
                mock(
                        BedrockConverseTransport.class
                );

        when(
                transport.converse(
                        any(
                                ConverseRequest.class
                        )
                )
        ).thenReturn(
                followUpResponse()
        );

        BedrockClarificationAdapter adapter =
                new BedrockClarificationAdapter(
                        transport,
                        new BedrockClarificationRequestFactory(
                                properties,
                                new BedrockClarificationToolContract(
                                        false
                                )
                        ),
                        new BedrockClarificationResponseParser(
                                properties
                        )
                );

        ClarificationAiTurn result =
                adapter.nextTurn(
                        request()
                );

        ClarificationAiTurn.FollowUpQuestion followUp =
                assertInstanceOf(
                        ClarificationAiTurn.FollowUpQuestion.class,
                        result
                );

        assertEquals(
                "How do you usually recharge?",
                followUp.text()
        );

        ArgumentCaptor<ConverseRequest> captor =
                ArgumentCaptor.forClass(
                        ConverseRequest.class
                );

        verify(
                transport
        ).converse(
                captor.capture()
        );

        assertEquals(
                "test-model",
                captor
                        .getValue()
                        .modelId()
        );

        assertEquals(
                2,
                captor
                        .getValue()
                        .toolConfig()
                        .tools()
                        .size()
        );
    }

    private ConverseResponse followUpResponse() {
        ToolUseBlock toolUse =
                ToolUseBlock
                        .builder()
                        .toolUseId(
                                "tool-1"
                        )
                        .name(
                                BedrockClarificationToolContract.ASK_FOLLOWUP
                        )
                        .input(
                                Document.fromMap(
                                        Map.of(
                                                "question",
                                                Document.fromString(
                                                        "How do you usually recharge?"
                                                )
                                        )
                                )
                        )
                        .build();

        Message message =
                Message
                        .builder()
                        .role(
                                ConversationRole.ASSISTANT
                        )
                        .content(
                                ContentBlock.fromToolUse(
                                        toolUse
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

    private ClarificationAiRequest request() {
        return new ClarificationAiRequest(
                new ClarificationAiRequest.TargetDimension(
                        new DimensionCode(
                                "EI"
                        ),
                        new PoleCode(
                                "E"
                        ),
                        new PoleCode(
                                "I"
                        )
                ),
                List.of(
                        new ClarificationAiRequest.QuestionnaireEvidence(
                                "I feel energized after social interaction.",
                                "Agree",
                                new PoleCode(
                                        "E"
                                )
                        )
                ),
                List.of(),
                1,
                3,
                "v1"
        );
    }
}
