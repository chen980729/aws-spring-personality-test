package dev.springawsportfolio.portfolio.assessment.application.exception;

public final class TieBreakNotRequiredException
        extends RuntimeException {

    public TieBreakNotRequiredException(
            String reason
    ) {
        super(reason);
    }
}
