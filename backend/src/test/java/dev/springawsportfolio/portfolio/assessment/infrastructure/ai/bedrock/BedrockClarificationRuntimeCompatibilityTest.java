package dev.springawsportfolio.portfolio.assessment.infrastructure.ai.bedrock;

import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimePolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BedrockClarificationRuntimeCompatibilityTest {

    @Test
    void acceptsLeaseLongerThanApiTimeoutPlusMargin() {
        assertDoesNotThrow(
                () ->
                        new BedrockClarificationRuntimeCompatibility(
                                properties(
                                        Duration.ofSeconds(
                                                20
                                        )
                                ),
                                runtimePolicy(
                                        Duration.ofSeconds(
                                                60
                                        )
                                )
                        )
        );
    }

    @Test
    void rejectsLeaseEqualToApiTimeoutPlusMargin() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new BedrockClarificationRuntimeCompatibility(
                                properties(
                                        Duration.ofSeconds(
                                                20
                                        )
                                ),
                                runtimePolicy(
                                        Duration.ofSeconds(
                                                25
                                        )
                                )
                        )
        );
    }

    @Test
    void rejectsLeaseShorterThanApiTimeoutPlusMargin() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new BedrockClarificationRuntimeCompatibility(
                                properties(
                                        Duration.ofSeconds(
                                                58
                                        )
                                ),
                                runtimePolicy(
                                        Duration.ofSeconds(
                                                60
                                        )
                                )
                        )
        );
    }

    private BedrockClarificationProperties properties(
            Duration apiCallTimeout
    ) {
        return new BedrockClarificationProperties(
                true,
                "ap-northeast-1",
                "test-model",
                apiCallTimeout,
                apiCallTimeout.minusSeconds(
                        1
                ),
                1024,
                false
        );
    }

    private ClarificationRuntimePolicy runtimePolicy(
            Duration lease
    ) {
        return new ClarificationRuntimePolicy(
                3,
                Duration.ofHours(
                        1
                ),
                lease
        );
    }
}
