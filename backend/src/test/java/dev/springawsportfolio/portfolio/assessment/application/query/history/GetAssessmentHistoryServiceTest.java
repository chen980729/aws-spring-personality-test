package dev.springawsportfolio.portfolio.assessment.application.query.history;

import dev.springawsportfolio.portfolio.assessment.application.exception.InvalidAssessmentHistoryPageException;
import dev.springawsportfolio.portfolio.assessment.application.port.out.AssessmentHistoryQuery;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetAssessmentHistoryServiceTest {

    private AssessmentHistoryQuery historyQuery;

    private GetAssessmentHistoryService service;

    private UserId actorUserId;

    @BeforeEach
    void setUp() {
        historyQuery =
                mock(
                        AssessmentHistoryQuery.class
                );

        service =
                new GetAssessmentHistoryService(
                        historyQuery
                );

        actorUserId =
                new UserId(
                        UUID.randomUUID()
                );
    }

    @Test
    void returnsCompletedHistoryFromReadPort() {
        AssessmentHistoryPage expected =
                new AssessmentHistoryPage(
                        List.of(),
                        1,
                        20,
                        0,
                        0
                );

        when(
                historyQuery.findCompletedByOwner(
                        actorUserId,
                        1,
                        20
                )
        )
                .thenReturn(
                        expected
                );

        AssessmentHistoryPage actual =
                service.execute(
                        actorUserId,
                        1,
                        20
                );

        assertEquals(
                expected,
                actual
        );

        verify(
                historyQuery
        )
                .findCompletedByOwner(
                        actorUserId,
                        1,
                        20
                );
    }

    @Test
    void rejectsPageBelowOne() {
        assertThrows(
                InvalidAssessmentHistoryPageException.class,
                () ->
                        service.execute(
                                actorUserId,
                                0,
                                20
                        )
        );

        verify(
                historyQuery,
                never()
        )
                .findCompletedByOwner(
                        actorUserId,
                        0,
                        20
                );
    }

    @Test
    void rejectsSizeBelowOne() {
        assertThrows(
                InvalidAssessmentHistoryPageException.class,
                () ->
                        service.execute(
                                actorUserId,
                                1,
                                0
                        )
        );

        verify(
                historyQuery,
                never()
        )
                .findCompletedByOwner(
                        actorUserId,
                        1,
                        0
                );
    }

    @Test
    void rejectsSizeAboveMaximum() {
        assertThrows(
                InvalidAssessmentHistoryPageException.class,
                () ->
                        service.execute(
                                actorUserId,
                                1,
                                101
                        )
        );

        verify(
                historyQuery,
                never()
        )
                .findCompletedByOwner(
                        actorUserId,
                        1,
                        101
                );
    }
}
