package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessment_definition_versions")
public class AssessmentDefinitionVersionJpaEntity {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "definition_id",
            nullable = false,
            updatable = false
    )
    private UUID definitionId;

    @Column(
            name = "version_code",
            nullable = false,
            updatable = false
    )
    private String versionCode;

    @Column(
            name = "status",
            nullable = false
    )
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "specification",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private JsonNode specification;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "published_at"
    )
    private Instant publishedAt;

    protected AssessmentDefinitionVersionJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getDefinitionId() {
        return definitionId;
    }

    public String getVersionCode() {
        return versionCode;
    }

    public String getStatus() {
        return status;
    }

    public JsonNode getSpecification() {
        return specification;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}