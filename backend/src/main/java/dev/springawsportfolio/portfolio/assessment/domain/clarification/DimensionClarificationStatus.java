package dev.springawsportfolio.portfolio.assessment.domain.clarification;

public enum DimensionClarificationStatus {

    PENDING,

    IN_PROGRESS,

    CLARIFIED,

    SKIPPED,

    FAILED_RETRYABLE;

    public boolean isTerminal() {
        return this == CLARIFIED
                || this == SKIPPED;
    }
}
