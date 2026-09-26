package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import java.util.Objects;

public record PoleCode(String value) {

    public PoleCode {
        Objects.requireNonNull(
                value,
                "PoleCode value must not be null"
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "PoleCode value must not be blank"
            );
        }
    }
}