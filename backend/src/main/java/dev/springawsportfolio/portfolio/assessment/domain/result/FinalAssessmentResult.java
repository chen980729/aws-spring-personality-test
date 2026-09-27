package dev.springawsportfolio.portfolio.assessment.domain.result;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record FinalAssessmentResult(
        String finalType,
        List<FinalDimensionConclusion> dimensions
) {

    public FinalAssessmentResult {
        Objects.requireNonNull(
                finalType,
                "finalType must not be null"
        );

        if (finalType.isBlank()) {
            throw new IllegalArgumentException(
                    "finalType must not be blank"
            );
        }

        Objects.requireNonNull(
                dimensions,
                "dimensions must not be null"
        );

        if (dimensions.isEmpty()) {
            throw new IllegalArgumentException(
                    "dimensions must not be empty"
            );
        }

        Set<DimensionCode> seenDimensions =
                new HashSet<>();

        for (
                FinalDimensionConclusion dimension
                : dimensions
        ) {
            Objects.requireNonNull(
                    dimension,
                    "dimensions must not contain null"
            );

            if (
                    !seenDimensions.add(
                            dimension.dimension()
                    )
            ) {
                throw new IllegalArgumentException(
                        "duplicate final conclusion for dimension: "
                                + dimension
                                .dimension()
                                .value()
                );
            }
        }

        dimensions =
                List.copyOf(
                        dimensions
                );
    }
}
