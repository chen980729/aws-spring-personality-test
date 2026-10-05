package dev.springawsportfolio.portfolio.assessment.application.exception;

public class TieBreakInteractionUnavailableException
        extends RuntimeException {

    public TieBreakInteractionUnavailableException(
            String message
    ) {
        super(message);
    }
}
