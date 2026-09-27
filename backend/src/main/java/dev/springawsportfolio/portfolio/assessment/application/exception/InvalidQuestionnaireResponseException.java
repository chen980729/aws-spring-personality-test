package dev.springawsportfolio.portfolio.assessment.application.exception;

public final class InvalidQuestionnaireResponseException
        extends RuntimeException {

    public InvalidQuestionnaireResponseException(
            String reason
    ) {
        super(reason);
    }
}
