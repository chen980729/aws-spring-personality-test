package dev.springawsportfolio.portfolio.assessment.infrastructure.config;

import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationRuntimeContextStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class ClarificationRuntimeSchedulingConfiguration {

    @Bean
    ClarificationRuntimeCleanupJob clarificationRuntimeCleanupJob(
            ClarificationRuntimeContextStore runtimeContextStore,
            Clock clock
    ) {
        return new ClarificationRuntimeCleanupJob(
                runtimeContextStore,
                clock
        );
    }
}
