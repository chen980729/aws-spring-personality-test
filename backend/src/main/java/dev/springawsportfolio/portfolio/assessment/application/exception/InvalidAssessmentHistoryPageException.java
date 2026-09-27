package dev.springawsportfolio.portfolio.assessment.application.exception;

public final class InvalidAssessmentHistoryPageException
        extends RuntimeException {

    public InvalidAssessmentHistoryPageException(
            int page,
            int size
    ) {
        super(
                "invalid assessment history pagination: "
                        + "page="
                        + page
                        + ", size="
                        + size
        );
    }
}
