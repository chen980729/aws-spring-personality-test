package dev.springawsportfolio.portfolio.assessment.domain.definition;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.AssessmentSpecification;

import java.time.Instant;
import java.util.Objects;

public final class AssessmentDefinitionVersion {

    private final AssessmentDefinitionVersionId id;
    private final AssessmentDefinitionId definitionId;
    private final String versionCode;
    private final AssessmentDefinitionVersionStatus status;
    private final AssessmentSpecification specification;
    private final Instant createdAt;
    private final Instant publishedAt;

    private AssessmentDefinitionVersion(
            AssessmentDefinitionVersionId id,
            AssessmentDefinitionId definitionId,
            String versionCode,
            AssessmentDefinitionVersionStatus status,
            AssessmentSpecification specification,
            Instant createdAt,
            Instant publishedAt
    ) {
        this.id = Objects.requireNonNull(
                id,
                "id must not be null"
        );
        this.definitionId = Objects.requireNonNull(
                definitionId,
                "definitionId must not be null"
        );
        this.versionCode = requireNotBlank(
                versionCode,
                "versionCode"
        );
        this.status = Objects.requireNonNull(
                status,
                "status must not be null"
        );
        this.specification = Objects.requireNonNull(
                specification,
                "specification must not be null"
        );
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "createdAt must not be null"
        );

        validatePublication(status, publishedAt);

        this.publishedAt = publishedAt;
    }

    public static AssessmentDefinitionVersion restore(
            AssessmentDefinitionVersionId id,
            AssessmentDefinitionId definitionId,
            String versionCode,
            AssessmentDefinitionVersionStatus status,
            AssessmentSpecification specification,
            Instant createdAt,
            Instant publishedAt
    ) {
        return new AssessmentDefinitionVersion(
                id,
                definitionId,
                versionCode,
                status,
                specification,
                createdAt,
                publishedAt
        );
    }

    public AssessmentDefinitionVersionId id() {
        return id;
    }

    public AssessmentDefinitionId definitionId() {
        return definitionId;
    }

    public String versionCode() {
        return versionCode;
    }

    public AssessmentDefinitionVersionStatus status() {
        return status;
    }

    public AssessmentSpecification specification() {
        return specification;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant publishedAt() {
        return publishedAt;
    }

    private static void validatePublication(
            AssessmentDefinitionVersionStatus status,
            Instant publishedAt
    ) {
        if (
                status == AssessmentDefinitionVersionStatus.DRAFT
                        && publishedAt != null
        ) {
            throw new IllegalArgumentException(
                    "DRAFT version must not have publishedAt"
            );
        }

        if (
                status != AssessmentDefinitionVersionStatus.DRAFT
                        && publishedAt == null
        ) {
            throw new IllegalArgumentException(
                    "Published version must have publishedAt"
            );
        }
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