package dev.springawsportfolio.portfolio.assessment.application.exception;

public final class AssessmentSessionAlreadyCompletedException
        extends RuntimeException {

    public AssessmentSessionAlreadyCompletedException() {
        super(
                "completed assessment session cannot be restarted"
        );
    }
}
