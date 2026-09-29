package dev.springawsportfolio.portfolio.assessment.domain.clarification;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;

import java.time.Instant;
import java.util.Objects;

public final class DimensionClarification {

    private final DimensionClarificationId id;

    private final AssessmentSessionId sessionId;

    private final DimensionCode dimension;

    private DimensionClarificationStatus status;

    private ClarificationExecutionToken activeExecutionToken;

    private ClarificationResult result;

    private AIProvenance aiProvenance;

    private Instant startedAt;

    private Instant acceptedAt;

    private Instant updatedAt;

    private DimensionClarification(
            DimensionClarificationId id,
            AssessmentSessionId sessionId,
            DimensionCode dimension,
            DimensionClarificationStatus status,
            ClarificationExecutionToken activeExecutionToken,
            ClarificationResult result,
            AIProvenance aiProvenance,
            Instant startedAt,
            Instant acceptedAt,
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

        validateLifecycle(
                status,
                activeExecutionToken,
                result,
                aiProvenance,
                startedAt,
                acceptedAt,
                updatedAt
        );

        this.activeExecutionToken =
                activeExecutionToken;

        this.result =
                result;

        this.aiProvenance =
                aiProvenance;

        this.startedAt =
                startedAt;

        this.acceptedAt =
                acceptedAt;
    }

    public static DimensionClarification pending(
            DimensionClarificationId id,
            AssessmentSessionId sessionId,
            DimensionCode dimension,
            Instant createdAt
    ) {
        return new DimensionClarification(
                id,
                sessionId,
                dimension,
                DimensionClarificationStatus.PENDING,
                null,
                null,
                null,
                null,
                null,
                createdAt
        );
    }

    public static DimensionClarification restore(
            DimensionClarificationId id,
            AssessmentSessionId sessionId,
            DimensionCode dimension,
            DimensionClarificationStatus status,
            ClarificationExecutionToken activeExecutionToken,
            ClarificationResult result,
            AIProvenance aiProvenance,
            Instant startedAt,
            Instant acceptedAt,
            Instant updatedAt
    ) {
        return new DimensionClarification(
                id,
                sessionId,
                dimension,
                status,
                activeExecutionToken,
                result,
                aiProvenance,
                startedAt,
                acceptedAt,
                updatedAt
        );
    }

    public ClarificationExecutionToken start(
            Instant startedAt
    ) {
        Objects.requireNonNull(
                startedAt,
                "startedAt must not be null"
        );

        if (status != DimensionClarificationStatus.PENDING) {
            throw new IllegalStateException(
                    "only a PENDING clarification can be started"
            );
        }

        requireNotBeforeUpdatedAt(
                startedAt
        );

        ClarificationExecutionToken executionToken =
                ClarificationExecutionToken.newToken();

        status =
                DimensionClarificationStatus.IN_PROGRESS;

        activeExecutionToken =
                executionToken;

        this.startedAt =
                startedAt;

        this.updatedAt =
                startedAt;

        return executionToken;
    }

    public void acceptResult(
            ClarificationExecutionToken executionToken,
            ClarificationResult result,
            AIProvenance aiProvenance,
            Instant acceptedAt
    ) {
        Objects.requireNonNull(
                executionToken,
                "executionToken must not be null"
        );

        Objects.requireNonNull(
                result,
                "result must not be null"
        );

        Objects.requireNonNull(
                aiProvenance,
                "aiProvenance must not be null"
        );

        Objects.requireNonNull(
                acceptedAt,
                "acceptedAt must not be null"
        );

        requireMatchingActiveExecution(
                executionToken
        );

        requireNotBeforeUpdatedAt(
                acceptedAt
        );

        status =
                DimensionClarificationStatus.CLARIFIED;

        activeExecutionToken =
                null;

        this.result =
                result;

        this.aiProvenance =
                aiProvenance;

        this.acceptedAt =
                acceptedAt;

        this.updatedAt =
                acceptedAt;
    }

    public void failRetryable(
            ClarificationExecutionToken executionToken,
            Instant failedAt
    ) {
        Objects.requireNonNull(
                executionToken,
                "executionToken must not be null"
        );

        Objects.requireNonNull(
                failedAt,
                "failedAt must not be null"
        );

        requireMatchingActiveExecution(
                executionToken
        );

        requireNotBeforeUpdatedAt(
                failedAt
        );

        status =
                DimensionClarificationStatus.FAILED_RETRYABLE;

        activeExecutionToken =
                null;

        updatedAt =
                failedAt;
    }

    public ClarificationExecutionToken retry(
            Instant retriedAt
    ) {
        Objects.requireNonNull(
                retriedAt,
                "retriedAt must not be null"
        );

        if (
                status
                        != DimensionClarificationStatus.FAILED_RETRYABLE
        ) {
            throw new IllegalStateException(
                    "only a FAILED_RETRYABLE clarification can be retried"
            );
        }

        requireNotBeforeUpdatedAt(
                retriedAt
        );

        ClarificationExecutionToken executionToken =
                ClarificationExecutionToken.newToken();

        status =
                DimensionClarificationStatus.IN_PROGRESS;

        activeExecutionToken =
                executionToken;

        updatedAt =
                retriedAt;

        return executionToken;
    }

    public void skip(
            Instant skippedAt
    ) {
        Objects.requireNonNull(
                skippedAt,
                "skippedAt must not be null"
        );

        if (
                status != DimensionClarificationStatus.PENDING
                        && status != DimensionClarificationStatus.IN_PROGRESS
                        && status != DimensionClarificationStatus.FAILED_RETRYABLE
        ) {
            throw new IllegalStateException(
                    "only a PENDING, IN_PROGRESS, or FAILED_RETRYABLE "
                            + "clarification can be skipped"
            );
        }

        requireNotBeforeUpdatedAt(
                skippedAt
        );

        status =
                DimensionClarificationStatus.SKIPPED;

        activeExecutionToken =
                null;

        result =
                null;

        aiProvenance =
                null;

        acceptedAt =
                null;

        updatedAt =
                skippedAt;
    }

    public boolean matchesActiveExecution(
            ClarificationExecutionToken executionToken
    ) {
        Objects.requireNonNull(
                executionToken,
                "executionToken must not be null"
        );

        return status
                == DimensionClarificationStatus.IN_PROGRESS
                && executionToken.equals(
                activeExecutionToken
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

    public ClarificationExecutionToken activeExecutionToken() {
        return activeExecutionToken;
    }

    public ClarificationResult result() {
        return result;
    }

    public AIProvenance aiProvenance() {
        return aiProvenance;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant acceptedAt() {
        return acceptedAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private void requireMatchingActiveExecution(
            ClarificationExecutionToken executionToken
    ) {
        if (!matchesActiveExecution(executionToken)) {
            throw new IllegalStateException(
                    "clarification execution is no longer active"
            );
        }
    }

    private void requireNotBeforeUpdatedAt(
            Instant transitionAt
    ) {
        if (
                transitionAt.isBefore(
                        updatedAt
                )
        ) {
            throw new IllegalArgumentException(
                    "clarification transition time must not be before updatedAt"
            );
        }
    }

    private static void validateLifecycle(
            DimensionClarificationStatus status,
            ClarificationExecutionToken activeExecutionToken,
            ClarificationResult result,
            AIProvenance aiProvenance,
            Instant startedAt,
            Instant acceptedAt,
            Instant updatedAt
    ) {
        if (
                startedAt != null
                        && updatedAt.isBefore(
                        startedAt
                )
        ) {
            throw new IllegalArgumentException(
                    "updatedAt must not be before startedAt"
            );
        }

        if (
                acceptedAt != null
                        && startedAt == null
        ) {
            throw new IllegalArgumentException(
                    "acceptedAt requires startedAt"
            );
        }

        if (
                acceptedAt != null
                        && acceptedAt.isBefore(
                        startedAt
                )
        ) {
            throw new IllegalArgumentException(
                    "acceptedAt must not be before startedAt"
            );
        }

        if (
                acceptedAt != null
                        && updatedAt.isBefore(
                        acceptedAt
                )
        ) {
            throw new IllegalArgumentException(
                    "updatedAt must not be before acceptedAt"
            );
        }

        if (
                status == DimensionClarificationStatus.IN_PROGRESS
                        && activeExecutionToken == null
        ) {
            throw new IllegalArgumentException(
                    "IN_PROGRESS clarification must have activeExecutionToken"
            );
        }

        if (
                status != DimensionClarificationStatus.IN_PROGRESS
                        && activeExecutionToken != null
        ) {
            throw new IllegalArgumentException(
                    "only IN_PROGRESS clarification may have activeExecutionToken"
            );
        }

        switch (status) {
            case PENDING -> {
                if (startedAt != null) {
                    throw new IllegalArgumentException(
                            "PENDING clarification must not have startedAt"
                    );
                }

                requireNoAcceptedResult(
                        result,
                        aiProvenance,
                        acceptedAt
                );
            }

            case IN_PROGRESS -> {
                if (startedAt == null) {
                    throw new IllegalArgumentException(
                            "IN_PROGRESS clarification must have startedAt"
                    );
                }

                requireNoAcceptedResult(
                        result,
                        aiProvenance,
                        acceptedAt
                );
            }

            case FAILED_RETRYABLE -> {
                if (startedAt == null) {
                    throw new IllegalArgumentException(
                            "FAILED_RETRYABLE clarification must have startedAt"
                    );
                }

                requireNoAcceptedResult(
                        result,
                        aiProvenance,
                        acceptedAt
                );
            }

            case SKIPPED ->
                    requireNoAcceptedResult(
                            result,
                            aiProvenance,
                            acceptedAt
                    );

            case CLARIFIED -> {
                if (startedAt == null) {
                    throw new IllegalArgumentException(
                            "CLARIFIED clarification must have startedAt"
                    );
                }

                Objects.requireNonNull(
                        result,
                        "CLARIFIED clarification must have result"
                );

                Objects.requireNonNull(
                        aiProvenance,
                        "CLARIFIED clarification must have aiProvenance"
                );

                Objects.requireNonNull(
                        acceptedAt,
                        "CLARIFIED clarification must have acceptedAt"
                );
            }
        }
    }

    private static void requireNoAcceptedResult(
            ClarificationResult result,
            AIProvenance aiProvenance,
            Instant acceptedAt
    ) {
        if (
                result != null
                        || aiProvenance != null
                        || acceptedAt != null
        ) {
            throw new IllegalArgumentException(
                    "non-CLARIFIED clarification must not have accepted result data"
            );
        }
    }
}
