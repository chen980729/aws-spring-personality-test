package dev.springawsportfolio.portfolio.assessment.application.exception;

public final class AssessmentAlreadySubmittedException
        extends RuntimeException {

    public AssessmentAlreadySubmittedException() {
        super("assessment questionnaire has already been submitted");
    }
}
