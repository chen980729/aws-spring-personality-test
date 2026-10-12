package dev.springawsportfolio.portfolio.assessment.infrastructure.config;

import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationRuntimeContextStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClarificationRuntimeCleanupJobTest {

    @Test
    void deletesRowsExpiredAtCurrentClockTime() {
        Instant now =
                Instant.parse(
                        "2026-10-12T12:00:00Z"
                );

        ClarificationRuntimeContextStore store =
                mock(
                        ClarificationRuntimeContextStore.class
                );

        when(
                store.deleteExpired(
                        now
                )
        ).thenReturn(
                2
        );

        ClarificationRuntimeCleanupJob job =
                new ClarificationRuntimeCleanupJob(
                        store,
                        Clock.fixed(
                                now,
                                ZoneOffset.UTC
                        )
                );

        job.deleteExpiredRuntimeContext();

        verify(
                store
        ).deleteExpired(
                now
        );
    }
}
