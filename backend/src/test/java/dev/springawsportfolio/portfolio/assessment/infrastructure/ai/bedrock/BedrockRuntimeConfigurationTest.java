package dev.springawsportfolio.portfolio.assessment.infrastructure.ai.bedrock;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class BedrockRuntimeConfigurationTest {

    @Test
    void buildsSynchronousRuntimeClientWithoutResolvingCredentials() {
        BedrockClarificationProperties properties =
                new BedrockClarificationProperties(
                        true,
                        "ap-northeast-1",
                        "test-model",
                        Duration.ofSeconds(20),
                        Duration.ofSeconds(18)
                );

        BedrockRuntimeConfiguration configuration =
                new BedrockRuntimeConfiguration();

        try (
                BedrockRuntimeClient client =
                        configuration.bedrockRuntimeClient(
                                properties
                        )
        ) {
            assertNotNull(
                    client
            );
        }
    }
}
