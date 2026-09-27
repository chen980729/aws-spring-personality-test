package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.entity;

import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "assessment_sessions")
public class AssessmentSessionJpaEntity {

    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "user_id",
            nullable = false,
            updatable = false
    )
    private UUID userId;

    @Column(
            name = "definition_id",
            nullable = false,
            updatable = false
    )
    private UUID definitionId;

    @Column(
            name = "definition_version_id",
            nullable = false,
            updatable = false
    )
    private UUID definitionVersionId;

    @Column(
            name = "status",
            nullable = false
    )
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "questionnaire_response",
            columnDefinition = "jsonb"
    )
    private JsonNode questionnaireResponse;

    @Column(
            name = "questionnaire_submitted_at"
    )
    private Instant questionnaireSubmittedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "initial_result",
            columnDefinition = "jsonb"
    )
    private JsonNode initialResult;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "final_result",
            columnDefinition = "jsonb"
    )
    private JsonNode finalResult;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "completed_at"
    )
    private Instant completedAt;

    @Column(
            name = "abandoned_at"
    )
    private Instant abandonedAt;

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private long version;

    protected AssessmentSessionJpaEntity() {
    }

    public void updateFrom(
            AssessmentSession session,
            JsonNode questionnaireResponse,
            JsonNode initialResult,
            JsonNode finalResult
    ) {
        Objects.requireNonNull(
                session,
                "session must not be null"
        );

        Objects.requireNonNull(
                questionnaireResponse,
                "questionnaireResponse must not be null"
        );

        if (
                !id.equals(
                        session.id().value()
                )
        ) {
            throw new IllegalArgumentException(
                    "cannot change AssessmentSession id"
            );
        }

        if (
                !userId.equals(
                        session.ownerUserId().value()
                )
        ) {
            throw new IllegalArgumentException(
                    "cannot change AssessmentSession owner"
            );
        }

        if (
                !definitionId.equals(
                        session.definitionId().value()
                )
        ) {
            throw new IllegalArgumentException(
                    "cannot change AssessmentSession definition"
            );
        }

        if (
                !definitionVersionId.equals(
                        session
                                .definitionVersionId()
                                .value()
                )
        ) {
            throw new IllegalArgumentException(
                    "cannot change AssessmentSession "
                            + "definition version"
            );
        }

        this.status =
                session.status().name();

        this.questionnaireResponse =
                questionnaireResponse;

        this.questionnaireSubmittedAt =
                session.questionnaireSubmittedAt();

        this.initialResult =
                initialResult;

        this.finalResult =
                finalResult;

        this.completedAt =
                session.completedAt();

        this.abandonedAt =
                session.abandonedAt();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getDefinitionId() {
        return definitionId;
    }

    public UUID getDefinitionVersionId() {
        return definitionVersionId;
    }

    public String getStatus() {
        return status;
    }

    public JsonNode getQuestionnaireResponse() {
        return questionnaireResponse;
    }

    public Instant getQuestionnaireSubmittedAt() {
        return questionnaireSubmittedAt;
    }

    public JsonNode getInitialResult() {
        return initialResult;
    }

    public JsonNode getFinalResult() {
        return finalResult;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getAbandonedAt() {
        return abandonedAt;
    }

    public long getVersion() {
        return version;
    }
}
