package dev.springawsportfolio.portfolio.assessment.application.exception;

public final class AssessmentSessionAlreadyAbandonedException
        extends RuntimeException {

    public AssessmentSessionAlreadyAbandonedException() {
        super("assessment session has already been abandoned");
    }
}
