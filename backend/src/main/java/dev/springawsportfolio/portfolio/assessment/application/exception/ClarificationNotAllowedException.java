package dev.springawsportfolio.portfolio.assessment.application.exception;

public final class ClarificationNotAllowedException
        extends RuntimeException {

    public ClarificationNotAllowedException(
            String reason
    ) {
        super(reason);
    }
}
