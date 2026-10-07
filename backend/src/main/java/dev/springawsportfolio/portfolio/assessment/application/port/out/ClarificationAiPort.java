package dev.springawsportfolio.portfolio.assessment.application.port.out;

public interface ClarificationAiPort {

    ClarificationAiTurn nextTurn(
            ClarificationAiRequest request
    );
}
