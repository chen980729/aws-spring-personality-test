package dev.springawsportfolio.portfolio.assessment.application.exception;

public final class InvalidDimensionTieBreakException
        extends RuntimeException {

    public InvalidDimensionTieBreakException(
            String reason
    ) {
        super(reason);
    }
}
