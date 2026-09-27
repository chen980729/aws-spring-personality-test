package dev.springawsportfolio.portfolio.assessment.domain.clarification;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;

import java.time.Instant;
import java.util.Objects;

public final class DimensionClarification {

    private final DimensionClarificationId id;

    private final AssessmentSessionId sessionId;

    private final DimensionCode dimension;

    private final DimensionClarificationStatus status;

    private final Instant updatedAt;

    private DimensionClarification(
            DimensionClarificationId id,
            AssessmentSessionId sessionId,
            DimensionCode dimension,
            DimensionClarificationStatus status,
            Instant updatedAt
    ) {
        this.id =
                Objects.requireNonNull(
                        id,
                        "id must not be null"
                );

        this.sessionId =
                Objects.requireNonNull(
                        sessionId,
                        "sessionId must not be null"
                );

        this.dimension =
                Objects.requireNonNull(
                        dimension,
                        "dimension must not be null"
                );

        this.status =
                Objects.requireNonNull(
                        status,
                        "status must not be null"
                );

        this.updatedAt =
                Objects.requireNonNull(
                        updatedAt,
                        "updatedAt must not be null"
                );
    }

    public static DimensionClarification pending(
            DimensionClarificationId id,
            AssessmentSessionId sessionId,
            DimensionCode dimension,
            Instant updatedAt
    ) {
        return new DimensionClarification(
                id,
                sessionId,
                dimension,
                DimensionClarificationStatus.PENDING,
                updatedAt
        );
    }

    public DimensionClarificationId id() {
        return id;
    }

    public AssessmentSessionId sessionId() {
        return sessionId;
    }

    public DimensionCode dimension() {
        return dimension;
    }

    public DimensionClarificationStatus status() {
        return status;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
