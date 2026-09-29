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
            name = "active_execution_token"
    )
    private UUID activeExecutionToken;

    @Column(
            name = "result_outcome"
    )
    private String resultOutcome;

    @Column(
            name = "suggested_pole"
    )
    private String suggestedPole;

    @Column(
            name = "result_confidence"
    )
    private String resultConfidence;

    @Column(
            name = "result_summary"
    )
    private String resultSummary;

    @Column(
            name = "ai_provider"
    )
    private String aiProvider;

    @Column(
            name = "ai_model_identifier"
    )
    private String aiModelIdentifier;

    @Column(
            name = "clarification_policy_revision"
    )
    private String clarificationPolicyRevision;

    @Column(
            name = "started_at"
    )
    private Instant startedAt;

    @Column(
            name = "accepted_at"
    )
    private Instant acceptedAt;

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

        applyMutableState(
                clarification
        );
    }

    public void updateFrom(
            DimensionClarification clarification
    ) {
        Objects.requireNonNull(
                clarification,
                "clarification must not be null"
        );

        if (
                !id.equals(
                        clarification.id().value()
                )
        ) {
            throw new IllegalArgumentException(
                    "cannot change DimensionClarification id"
            );
        }

        if (
                !sessionId.equals(
                        clarification.sessionId().value()
                )
        ) {
            throw new IllegalArgumentException(
                    "cannot change DimensionClarification session"
            );
        }

        if (
                !dimensionCode.equals(
                        clarification.dimension().value()
                )
        ) {
            throw new IllegalArgumentException(
                    "cannot change DimensionClarification dimension"
            );
        }

        applyMutableState(
                clarification
        );
    }

    private void applyMutableState(
            DimensionClarification clarification
    ) {
        this.status =
                clarification.status().name();

        this.activeExecutionToken =
                clarification.activeExecutionToken() == null
                        ? null
                        : clarification
                        .activeExecutionToken()
                        .value();

        if (clarification.result() == null) {
            this.resultOutcome = null;
            this.suggestedPole = null;
            this.resultConfidence = null;
            this.resultSummary = null;
        } else {
            this.resultOutcome =
                    clarification
                            .result()
                            .resolution()
                            .name();

            this.suggestedPole =
                    clarification
                            .result()
                            .suggestedPole()
                            == null
                            ? null
                            : clarification
                            .result()
                            .suggestedPole()
                            .value();

            this.resultConfidence =
                    clarification
                            .result()
                            .confidence()
                            .name();

            this.resultSummary =
                    clarification
                            .result()
                            .reasoningSummary();
        }

        if (clarification.aiProvenance() == null) {
            this.aiProvider = null;
            this.aiModelIdentifier = null;
            this.clarificationPolicyRevision = null;
        } else {
            this.aiProvider =
                    clarification
                            .aiProvenance()
                            .provider();

            this.aiModelIdentifier =
                    clarification
                            .aiProvenance()
                            .modelIdentifier();

            this.clarificationPolicyRevision =
                    clarification
                            .aiProvenance()
                            .clarificationPolicyRevision();
        }

        this.startedAt =
                clarification.startedAt();

        this.acceptedAt =
                clarification.acceptedAt();

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

    public UUID getActiveExecutionToken() {
        return activeExecutionToken;
    }

    public String getResultOutcome() {
        return resultOutcome;
    }

    public String getSuggestedPole() {
        return suggestedPole;
    }

    public String getResultConfidence() {
        return resultConfidence;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public String getAiModelIdentifier() {
        return aiModelIdentifier;
    }

    public String getClarificationPolicyRevision() {
        return clarificationPolicyRevision;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
