package dev.springawsportfolio.portfolio.assessment.web.session.dto;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AssessmentHistoryResponse(
        List<AssessmentHistoryItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public AssessmentHistoryResponse {
        Objects.requireNonNull(
                items,
                "items must not be null"
        );

        items =
                List.copyOf(
                        items
                );
    }

    public record AssessmentHistoryItemResponse(
            UUID sessionId,
            String assessmentCode,
            String assessmentVersion,
            String finalType,
            Instant completedAt
    ) {
    }
}
