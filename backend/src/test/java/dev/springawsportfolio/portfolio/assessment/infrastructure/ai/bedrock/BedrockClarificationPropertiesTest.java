package dev.springawsportfolio.portfolio.assessment.infrastructure.ai.bedrock;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BedrockClarificationPropertiesTest {

    @Test
    void appliesSafeDefaultsWhenDisabled() {
        BedrockClarificationProperties properties =
                new BedrockClarificationProperties(
                        false,
                        null,
                        null,
                        null,
                        null
                );

        assertEquals(
                "ap-northeast-1",
                properties.region()
        );

        assertEquals(
                "",
                properties.modelId()
        );

        assertEquals(
                Duration.ofSeconds(20),
                properties.apiCallTimeout()
        );

        assertEquals(
                Duration.ofSeconds(18),
                properties.apiCallAttemptTimeout()
        );
    }

    @Test
    void requiresModelIdWhenEnabled() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BedrockClarificationProperties(
                        true,
                        "ap-northeast-1",
                        " ",
                        Duration.ofSeconds(20),
                        Duration.ofSeconds(18)
                )
        );
    }

    @Test
    void requiresAttemptTimeoutShorterThanTotalTimeout() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BedrockClarificationProperties(
                        false,
                        "ap-northeast-1",
                        null,
                        Duration.ofSeconds(20),
                        Duration.ofSeconds(20)
                )
        );
    }
}
