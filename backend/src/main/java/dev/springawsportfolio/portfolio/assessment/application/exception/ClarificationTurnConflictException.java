package dev.springawsportfolio.portfolio.assessment.application.exception;

public final class ClarificationTurnConflictException
        extends RuntimeException {

    public ClarificationTurnConflictException(
            String reason
    ) {
        super(reason);
    }
}
