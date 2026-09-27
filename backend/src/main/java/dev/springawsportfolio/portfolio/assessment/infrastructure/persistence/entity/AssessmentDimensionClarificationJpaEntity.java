package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.entity;

import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "assessment_dimension_clarifications")
public class AssessmentDimensionClarificationJpaEntity {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "session_id",
            nullable = false,
            updatable = false
    )
    private UUID sessionId;

    @Column(
            name = "dimension_code",
            nullable = false,
            updatable = false
    )
    private String dimensionCode;

    @Column(
            name = "status",
            nullable = false
    )
    private String status;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    protected AssessmentDimensionClarificationJpaEntity() {
    }

    public AssessmentDimensionClarificationJpaEntity(
            DimensionClarification clarification
    ) {
        Objects.requireNonNull(
                clarification,
                "clarification must not be null"
        );

        this.id =
                clarification.id().value();

        this.sessionId =
                clarification.sessionId().value();

        this.dimensionCode =
                clarification.dimension().value();

        this.status =
                clarification.status().name();

        this.updatedAt =
                clarification.updatedAt();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getDimensionCode() {
        return dimensionCode;
    }

    public String getStatus() {
        return status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
