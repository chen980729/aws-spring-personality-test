package dev.springawsportfolio.portfolio.assessment.infrastructure.ai.bedrock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        BedrockClarificationProperties.class
)
@ConditionalOnProperty(
        prefix = "app.assessment.clarification.ai.bedrock",
        name = "enabled",
        havingValue = "true"
)
class BedrockRuntimeConfiguration {

    @Bean
    BedrockRuntimeClient bedrockRuntimeClient(
            BedrockClarificationProperties properties
    ) {
        ClientOverrideConfiguration overrideConfiguration =
                ClientOverrideConfiguration
                        .builder()
                        .apiCallTimeout(
                                properties.apiCallTimeout()
                        )
                        .apiCallAttemptTimeout(
                                properties.apiCallAttemptTimeout()
                        )
                        .build();

        return BedrockRuntimeClient
                .builder()
                .region(
                        Region.of(
                                properties.region()
                        )
                )
                .credentialsProvider(
                        DefaultCredentialsProvider.create()
                )
                .overrideConfiguration(
                        overrideConfiguration
                )
                .build();
    }

    @Bean
    BedrockConverseTransport bedrockConverseTransport(
            BedrockRuntimeClient client
    ) {
        return new BedrockConverseTransport(
                client
        );
    }
}
