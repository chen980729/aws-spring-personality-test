package dev.springawsportfolio.portfolio.assessment.application.query.history;

import dev.springawsportfolio.portfolio.assessment.application.exception.InvalidAssessmentHistoryPageException;
import dev.springawsportfolio.portfolio.assessment.application.port.out.AssessmentHistoryQuery;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class GetAssessmentHistoryService {

    public static final int DEFAULT_PAGE =
            1;

    public static final int DEFAULT_SIZE =
            20;

    public static final int MAX_SIZE =
            100;

    private final AssessmentHistoryQuery
            historyQuery;

    public GetAssessmentHistoryService(
            AssessmentHistoryQuery historyQuery
    ) {
        this.historyQuery =
                Objects.requireNonNull(
                        historyQuery,
                        "historyQuery must not be null"
                );
    }

    @Transactional(readOnly = true)
    public AssessmentHistoryPage execute(
            UserId actorUserId,
            int page,
            int size
    ) {
        Objects.requireNonNull(
                actorUserId,
                "actorUserId must not be null"
        );

        validatePagination(
                page,
                size
        );

        return historyQuery
                .findCompletedByOwner(
                        actorUserId,
                        page,
                        size
                );
    }

    private void validatePagination(
            int page,
            int size
    ) {
        if (
                page < 1
                        || size < 1
                        || size > MAX_SIZE
        ) {
            throw new InvalidAssessmentHistoryPageException(
                    page,
                    size
            );
        }
    }
}
