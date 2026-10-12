package dev.springawsportfolio.portfolio.assessment.infrastructure.ai.bedrock;

import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiPort;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiRequest;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiTurn;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;

import java.util.Objects;

public final class BedrockClarificationAdapter
        implements ClarificationAiPort {

    private final BedrockConverseTransport transport;

    private final BedrockClarificationRequestFactory
            requestFactory;

    private final BedrockClarificationResponseParser
            responseParser;

    public BedrockClarificationAdapter(
            BedrockConverseTransport transport,
            BedrockClarificationRequestFactory requestFactory,
            BedrockClarificationResponseParser responseParser
    ) {
        this.transport =
                Objects.requireNonNull(
                        transport,
                        "transport must not be null"
                );

        this.requestFactory =
                Objects.requireNonNull(
                        requestFactory,
                        "requestFactory must not be null"
                );

        this.responseParser =
                Objects.requireNonNull(
                        responseParser,
                        "responseParser must not be null"
                );
    }

    @Override
    public ClarificationAiTurn nextTurn(
            ClarificationAiRequest request
    ) {
        Objects.requireNonNull(
                request,
                "request must not be null"
        );

        ConverseRequest bedrockRequest =
                requestFactory.create(
                        request
                );

        ConverseResponse bedrockResponse =
                transport.converse(
                        bedrockRequest
                );

        return responseParser.parse(
                bedrockResponse,
                request
        );
    }
}
