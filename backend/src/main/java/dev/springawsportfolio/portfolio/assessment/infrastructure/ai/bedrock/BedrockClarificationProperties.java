package dev.springawsportfolio.portfolio.assessment.infrastructure.ai.bedrock;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(
        prefix = "app.assessment.clarification.ai.bedrock"
)
public record BedrockClarificationProperties(
        boolean enabled,
        String region,
        String modelId,
        Duration apiCallTimeout,
        Duration apiCallAttemptTimeout,
        Integer maxTokens,
        boolean strictToolSchema
) {

    private static final String DEFAULT_REGION =
            "ap-northeast-1";

    private static final Duration DEFAULT_API_CALL_TIMEOUT =
            Duration.ofSeconds(20);

    private static final Duration DEFAULT_API_CALL_ATTEMPT_TIMEOUT =
            Duration.ofSeconds(18);

    private static final int DEFAULT_MAX_TOKENS =
            1024;

    public BedrockClarificationProperties {
        region =
                normalizeOrDefault(
                        region,
                        DEFAULT_REGION
                );

        modelId =
                modelId == null
                        ? ""
                        : modelId.trim();

        apiCallTimeout =
                apiCallTimeout == null
                        ? DEFAULT_API_CALL_TIMEOUT
                        : apiCallTimeout;

        apiCallAttemptTimeout =
                apiCallAttemptTimeout == null
                        ? DEFAULT_API_CALL_ATTEMPT_TIMEOUT
                        : apiCallAttemptTimeout;

        maxTokens =
                maxTokens == null
                        ? DEFAULT_MAX_TOKENS
                        : maxTokens;

        if (
                apiCallTimeout.isZero()
                        || apiCallTimeout.isNegative()
        ) {
            throw new IllegalArgumentException(
                    "apiCallTimeout must be positive"
            );
        }

        if (
                apiCallAttemptTimeout.isZero()
                        || apiCallAttemptTimeout.isNegative()
        ) {
            throw new IllegalArgumentException(
                    "apiCallAttemptTimeout must be positive"
            );
        }

        if (
                apiCallAttemptTimeout
                        .compareTo(
                                apiCallTimeout
                        ) >= 0
        ) {
            throw new IllegalArgumentException(
                    "apiCallAttemptTimeout must be shorter "
                            + "than apiCallTimeout"
            );
        }

        if (maxTokens <= 0) {
            throw new IllegalArgumentException(
                    "maxTokens must be positive"
            );
        }

        if (
                enabled
                        && modelId.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "modelId must be configured when "
                            + "Bedrock clarification is enabled"
            );
        }
    }

    private static String normalizeOrDefault(
            String value,
            String defaultValue
    ) {
        if (
                value == null
                        || value.isBlank()
        ) {
            return defaultValue;
        }

        return value.trim();
    }
}
