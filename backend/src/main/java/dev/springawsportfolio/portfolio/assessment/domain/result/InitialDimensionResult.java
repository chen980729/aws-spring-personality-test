package dev.springawsportfolio.portfolio.assessment.domain.result;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;

import java.util.Objects;

public record InitialDimensionResult(
        DimensionCode dimension,
        int rawScore,
        PoleCode questionnairePreference,
        boolean ambiguous
) {

    public InitialDimensionResult {
        Objects.requireNonNull(
                dimension,
                "dimension must not be null"
        );

        if (
                rawScore == 0
                        && questionnairePreference != null
        ) {
            throw new IllegalArgumentException(
                    "exact tie must not have "
                            + "a questionnairePreference"
            );
        }

        if (
                rawScore != 0
                        && questionnairePreference == null
        ) {
            throw new IllegalArgumentException(
                    "non-zero rawScore must have "
                            + "a questionnairePreference"
            );
        }
    }

    public boolean exactTie() {
        return rawScore == 0;
    }
}
