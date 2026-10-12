package dev.springawsportfolio.portfolio.assessment.infrastructure.config;

import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationRuntimeContextStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.util.Objects;

public final class ClarificationRuntimeCleanupJob {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    ClarificationRuntimeCleanupJob.class
            );

    private final ClarificationRuntimeContextStore
            runtimeContextStore;

    private final Clock clock;

    public ClarificationRuntimeCleanupJob(
            ClarificationRuntimeContextStore runtimeContextStore,
            Clock clock
    ) {
        this.runtimeContextStore =
                Objects.requireNonNull(
                        runtimeContextStore,
                        "runtimeContextStore must not be null"
                );

        this.clock =
                Objects.requireNonNull(
                        clock,
                        "clock must not be null"
                );
    }

    @Scheduled(
            fixedDelayString =
                    "${app.assessment.clarification.runtime.cleanup-interval-ms:900000}"
    )
    public void deleteExpiredRuntimeContext() {
        int deleted =
                runtimeContextStore.deleteExpired(
                        clock.instant()
                );

        if (deleted > 0) {
            LOGGER.info(
                    "Deleted {} expired clarification runtime message rows",
                    deleted
            );
        }
    }
}
