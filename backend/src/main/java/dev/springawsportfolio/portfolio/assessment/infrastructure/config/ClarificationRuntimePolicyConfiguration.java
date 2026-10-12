package dev.springawsportfolio.portfolio.assessment.infrastructure.config;

import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimePolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        ClarificationRuntimeProperties.class
)
public class ClarificationRuntimePolicyConfiguration {

    @Bean
    ClarificationRuntimePolicy clarificationRuntimePolicy(
            ClarificationRuntimeProperties properties
    ) {
        return new ClarificationRuntimePolicy(
                properties.maximumModelTurns(),
                properties.contextTtl()
        );
    }
}
