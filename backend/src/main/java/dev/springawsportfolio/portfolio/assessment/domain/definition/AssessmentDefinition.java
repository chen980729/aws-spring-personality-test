package dev.springawsportfolio.portfolio.assessment.domain.definition;

import java.time.Instant;
import java.util.Objects;

public final class AssessmentDefinition {

    private final AssessmentDefinitionId id;
    private final String code;
    private final String name;
    private final Instant createdAt;

    private AssessmentDefinition(
            AssessmentDefinitionId id,
            String code,
            String name,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(
                id,
                "id must not be null"
        );
        this.code = requireNotBlank(code, "code");
        this.name = requireNotBlank(name, "name");
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "createdAt must not be null"
        );
    }

    public static AssessmentDefinition create(
            AssessmentDefinitionId id,
            String code,
            String name,
            Instant createdAt
    ) {
        return new AssessmentDefinition(
                id,
                code,
                name,
                createdAt
        );
    }

    public static AssessmentDefinition restore(
            AssessmentDefinitionId id,
            String code,
            String name,
            Instant createdAt
    ) {
        return new AssessmentDefinition(
                id,
                code,
                name,
                createdAt
        );
    }

    public AssessmentDefinitionId id() {
        return id;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public Instant createdAt() {
        return createdAt;
    }

    private static String requireNotBlank(
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

        return value;
    }
}
