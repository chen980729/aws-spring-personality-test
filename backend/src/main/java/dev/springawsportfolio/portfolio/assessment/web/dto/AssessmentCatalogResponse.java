package dev.springawsportfolio.portfolio.assessment.web.dto;

import java.util.List;
import java.util.Objects;

public record AssessmentCatalogResponse(
        List<AssessmentSummaryResponse> items
) {

    public AssessmentCatalogResponse {
        Objects.requireNonNull(
                items,
                "items must not be null"
        );

        items = List.copyOf(items);
    }

    public record AssessmentSummaryResponse(
            String code,
            String name,
            String availableVersion,
            int questionCount
    ) {
    }
}
