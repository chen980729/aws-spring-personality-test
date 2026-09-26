package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import java.util.Objects;

public record DimensionCode(String value) {

    public DimensionCode {
        Objects.requireNonNull(
                value,
                "DimensionCode value must not be null"
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "DimensionCode value must not be blank"
            );
        }
    }
}
