package dev.springawsportfolio.portfolio.assessment.application.exception;

public final class AssessmentSessionNotFoundException
        extends RuntimeException {

    public AssessmentSessionNotFoundException() {
        super("assessment session not found");
    }
}
