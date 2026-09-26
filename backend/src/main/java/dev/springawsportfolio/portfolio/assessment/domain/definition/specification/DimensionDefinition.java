package dev.springawsportfolio.portfolio.assessment.domain.definition.specification;

import java.util.Objects;
import java.util.Set;

public record DimensionDefinition(
        DimensionCode code,
        int position,
        PoleCode poleA,
        PoleCode poleB
) {

    public DimensionDefinition {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(poleA, "poleA must not be null");
        Objects.requireNonNull(poleB, "poleB must not be null");

        if (position <= 0) {
            throw new IllegalArgumentException(
                    "dimension position must be positive"
            );
        }

        if (poleA.equals(poleB)) {
            throw new IllegalArgumentException(
                    "dimension poles must be different"
            );
        }
    }

    public boolean containsPole(PoleCode pole) {
        Objects.requireNonNull(pole, "pole must not be null");

        return poleA.equals(pole) || poleB.equals(pole);
    }

    public Set<PoleCode> poles() {
        return Set.of(poleA, poleB);
    }
}
