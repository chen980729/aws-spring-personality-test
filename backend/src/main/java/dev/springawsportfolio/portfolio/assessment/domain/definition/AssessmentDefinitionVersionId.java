package dev.springawsportfolio.portfolio.assessment.domain.definition;

import java.util.Objects;
import java.util.UUID;

public record AssessmentDefinitionVersionId(UUID value) {

    public AssessmentDefinitionVersionId {
        Objects.requireNonNull(
                value,
                "AssessmentDefinitionVersionId value must not be null"
        );
    }

    public static AssessmentDefinitionVersionId newId() {
        return new AssessmentDefinitionVersionId(UUID.randomUUID());
    }
}
