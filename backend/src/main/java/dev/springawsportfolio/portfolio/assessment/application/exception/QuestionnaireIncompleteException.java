package dev.springawsportfolio.portfolio.assessment.application.exception;

public final class QuestionnaireIncompleteException
        extends RuntimeException {

    public QuestionnaireIncompleteException() {
        super(
                "questionnaire response is incomplete"
        );
    }
}