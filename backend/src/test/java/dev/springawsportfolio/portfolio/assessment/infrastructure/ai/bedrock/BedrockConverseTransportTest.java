package dev.springawsportfolio.portfolio.assessment.infrastructure.ai.bedrock;

import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiTechnicalException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BedrockConverseTransportTest {

    @Test
    void returnsSuccessfulConverseResponse() {
        BedrockRuntimeClient client =
                mock(
                        BedrockRuntimeClient.class
                );

        ConverseRequest request =
                ConverseRequest
                        .builder()
                        .modelId(
                                "test-model"
                        )
                        .build();

        ConverseResponse response =
                ConverseResponse
                        .builder()
                        .build();

        when(
                client.converse(
                        request
                )
        ).thenReturn(
                response
        );

        BedrockConverseTransport transport =
                new BedrockConverseTransport(
                        client
                );

        assertSame(
                response,
                transport.converse(
                        request
                )
        );
    }

    @Test
    void mapsAwsSdkFailureToTechnicalFailure() {
        BedrockRuntimeClient client =
                mock(
                        BedrockRuntimeClient.class
                );

        ConverseRequest request =
                ConverseRequest
                        .builder()
                        .modelId(
                                "test-model"
                        )
                        .build();

        SdkClientException sdkFailure =
                SdkClientException
                        .builder()
                        .message(
                                "network failure"
                        )
                        .build();

        when(
                client.converse(
                        request
                )
        ).thenThrow(
                sdkFailure
        );

        BedrockConverseTransport transport =
                new BedrockConverseTransport(
                        client
                );

        ClarificationAiTechnicalException exception =
                assertThrows(
                        ClarificationAiTechnicalException.class,
                        () -> transport.converse(
                                request
                        )
                );

        assertSame(
                sdkFailure,
                exception.getCause()
        );
    }

    @Test
    void doesNotMaskProgrammingFailuresAsProviderFailures() {
        BedrockRuntimeClient client =
                mock(
                        BedrockRuntimeClient.class
                );

        ConverseRequest request =
                ConverseRequest
                        .builder()
                        .modelId(
                                "test-model"
                        )
                        .build();

        IllegalStateException programmingFailure =
                new IllegalStateException(
                        "programming bug"
                );

        when(
                client.converse(
                        request
                )
        ).thenThrow(
                programmingFailure
        );

        BedrockConverseTransport transport =
                new BedrockConverseTransport(
                        client
                );

        assertSame(
                programmingFailure,
                assertThrows(
                        IllegalStateException.class,
                        () -> transport.converse(
                                request
                        )
                )
        );
    }
}
