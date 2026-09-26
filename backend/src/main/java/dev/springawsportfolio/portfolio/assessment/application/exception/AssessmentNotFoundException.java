package dev.springawsportfolio.portfolio.assessment.application.exception;

public final class AssessmentNotFoundException
        extends RuntimeException {

    public AssessmentNotFoundException(
            String assessmentCode
    ) {
        super(
                "assessment not found: "
                        + assessmentCode
        );
    }
}