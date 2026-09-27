package dev.springawsportfolio.portfolio.assessment.web.error;

public final class InvalidAssessmentHistoryStatusException
        extends RuntimeException {

    public InvalidAssessmentHistoryStatusException(
            String status
    ) {
        super(
                "unsupported assessment history status: "
                        + status
        );
    }
}
