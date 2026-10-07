package dev.springawsportfolio.portfolio.assessment.infrastructure.ai.bedrock;

import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiTechnicalException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;

import java.util.Objects;

public final class BedrockConverseTransport {

    private final BedrockRuntimeClient client;

    public BedrockConverseTransport(
            BedrockRuntimeClient client
    ) {
        this.client =
                Objects.requireNonNull(
                        client,
                        "client must not be null"
                );
    }

    public ConverseResponse converse(
            ConverseRequest request
    ) {
        Objects.requireNonNull(
                request,
                "request must not be null"
        );

        try {
            return client.converse(
                    request
            );
        } catch (SdkException exception) {
            throw new ClarificationAiTechnicalException(
                    "Amazon Bedrock Converse call failed",
                    exception
            );
        }
    }
}
