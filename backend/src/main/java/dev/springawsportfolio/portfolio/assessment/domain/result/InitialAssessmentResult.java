package dev.springawsportfolio.portfolio.assessment.domain.result;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record InitialAssessmentResult(
        List<InitialDimensionResult> dimensions
) {

    public InitialAssessmentResult {
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
                InitialDimensionResult dimension
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
                        "duplicate initial result for dimension: "
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

    public boolean hasAmbiguousDimensions() {
        return dimensions
                .stream()
                .anyMatch(
                        InitialDimensionResult::ambiguous
                );
    }

    public List<DimensionCode> ambiguousDimensions() {
        return dimensions
                .stream()
                .filter(
                        InitialDimensionResult::ambiguous
                )
                .map(
                        InitialDimensionResult::dimension
                )
                .toList();
    }
}
