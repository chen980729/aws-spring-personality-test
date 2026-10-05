package dev.springawsportfolio.portfolio.assessment.application.exception;

public class TieBreakAlreadyDecidedException
        extends RuntimeException {

    public TieBreakAlreadyDecidedException(
            String message
    ) {
        super(message);
    }
}
