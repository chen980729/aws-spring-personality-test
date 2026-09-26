package dev.springawsportfolio.portfolio.assessment.domain.definition;

import java.util.Objects;
import java.util.UUID;

public record AssessmentDefinitionId(UUID value) {

    public AssessmentDefinitionId {
        Objects.requireNonNull(
                value,
                "AssessmentDefinitionId value must not be null"
        );
    }

    public static AssessmentDefinitionId newId() {
        return new AssessmentDefinitionId(UUID.randomUUID());
    }
}