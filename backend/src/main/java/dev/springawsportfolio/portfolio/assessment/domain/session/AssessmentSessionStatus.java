package dev.springawsportfolio.portfolio.assessment.domain.session;

public enum AssessmentSessionStatus {

    IN_PROGRESS(true),

    AWAITING_CLARIFICATION(true),

    CLARIFICATION_IN_PROGRESS(true),

    COMPLETED(false),

    ABANDONED(false);

    private final boolean active;

    AssessmentSessionStatus(
            boolean active
    ) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
