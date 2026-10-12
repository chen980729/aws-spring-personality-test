package dev.springawsportfolio.portfolio.assessment.application.clarification.prompt;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;

import java.util.Map;
import java.util.Objects;

public record ClarificationPromptPolicy(
        String revision,
        String baseInstructions,
        String finalTurnInstructions,
        Map<DimensionCode, DimensionSemantics> dimensions
) {

    public ClarificationPromptPolicy {
        requireNonBlank(
                revision,
                "revision"
        );
        requireNonBlank(
                baseInstructions,
                "baseInstructions"
        );
        requireNonBlank(
                finalTurnInstructions,
                "finalTurnInstructions"
        );

        Objects.requireNonNull(
                dimensions,
                "dimensions must not be null"
        );

        if (dimensions.isEmpty()) {
            throw new IllegalArgumentException(
                    "dimensions must not be empty"
            );
        }

        dimensions =
                Map.copyOf(
                        dimensions
                );
    }

    public DimensionSemantics requireDimension(
            DimensionCode code
    ) {
        Objects.requireNonNull(
                code,
                "code must not be null"
        );

        DimensionSemantics semantics =
                dimensions.get(
                        code
                );

        if (semantics == null) {
            throw new IllegalArgumentException(
                    "prompt policy does not define dimension: "
                            + code.value()
            );
        }

        return semantics;
    }

    public record DimensionSemantics(
            String label,
            PoleSemantics poleA,
            PoleSemantics poleB
    ) {

        public DimensionSemantics {
            requireNonBlank(
                    label,
                    "label"
            );
            Objects.requireNonNull(
                    poleA,
                    "poleA must not be null"
            );
            Objects.requireNonNull(
                    poleB,
                    "poleB must not be null"
            );

            if (
                    poleA.code()
                            .equals(
                                    poleB.code()
                            )
            ) {
                throw new IllegalArgumentException(
                        "dimension prompt poles must be different"
                );
            }
        }

        public PoleSemantics requirePole(
                PoleCode code
        ) {
            Objects.requireNonNull(
                    code,
                    "code must not be null"
            );

            if (
                    poleA.code()
                            .equals(
                                    code
                            )
            ) {
                return poleA;
            }

            if (
                    poleB.code()
                            .equals(
                                    code
                            )
            ) {
                return poleB;
            }

            throw new IllegalArgumentException(
                    "prompt policy does not define pole: "
                            + code.value()
            );
        }
    }

    public record PoleSemantics(
            PoleCode code,
            String label,
            String neutralDescription
    ) {

        public PoleSemantics {
            Objects.requireNonNull(
                    code,
                    "code must not be null"
            );
            requireNonBlank(
                    label,
                    "label"
            );
            requireNonBlank(
                    neutralDescription,
                    "neutralDescription"
            );
        }
    }

    private static void requireNonBlank(
            String value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " must not be null"
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }
    }
}
