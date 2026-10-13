package dev.springawsportfolio.portfolio.assessment.infrastructure.config;

import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationAiCallCommitService;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationAiCallPreparationService;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationExecutionRecoveryService;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationRuntimeOrchestrator;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.RetryDimensionClarificationService;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.StartDimensionClarificationService;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationAiPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ClarificationRuntimeOrchestrationConfiguration {

    @Bean
    @ConditionalOnBean(ClarificationAiPort.class)
    ClarificationRuntimeOrchestrator clarificationRuntimeOrchestrator(
            StartDimensionClarificationService startService,
            RetryDimensionClarificationService retryService,
            ClarificationAiCallPreparationService preparationService,
            ClarificationAiCallCommitService commitService,
            ClarificationExecutionRecoveryService recoveryService,
            ClarificationAiPort aiPort
    ) {
        return new ClarificationRuntimeOrchestrator(
                startService,
                retryService,
                preparationService,
                commitService,
                recoveryService,
                aiPort
        );
    }
}
