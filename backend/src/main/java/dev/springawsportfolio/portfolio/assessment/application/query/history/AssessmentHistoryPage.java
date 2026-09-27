package dev.springawsportfolio.portfolio.assessment.application.query.history;

import java.util.List;
import java.util.Objects;

public record AssessmentHistoryPage(
        List<AssessmentHistoryItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public AssessmentHistoryPage {
        Objects.requireNonNull(
                items,
                "items must not be null"
        );

        if (page < 1) {
            throw new IllegalArgumentException(
                    "page must be at least 1"
            );
        }

        if (size < 1) {
            throw new IllegalArgumentException(
                    "size must be at least 1"
            );
        }

        if (totalElements < 0) {
            throw new IllegalArgumentException(
                    "totalElements must not be negative"
            );
        }

        if (totalPages < 0) {
            throw new IllegalArgumentException(
                    "totalPages must not be negative"
            );
        }

        items =
                List.copyOf(
                        items
                );
    }
}
