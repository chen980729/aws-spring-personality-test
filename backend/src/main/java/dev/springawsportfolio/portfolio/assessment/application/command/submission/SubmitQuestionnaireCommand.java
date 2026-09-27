package dev.springawsportfolio.portfolio.assessment.application.command.submission;

import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.identity.api.UserId;

import java.util.List;
import java.util.Objects;

public record SubmitQuestionnaireCommand(
        UserId actorUserId,
        AssessmentSessionId sessionId,
        List<AnswerInput> answers
) {

    public SubmitQuestionnaireCommand {
        Objects.requireNonNull(
                actorUserId,
                "actorUserId must not be null"
        );

        Objects.requireNonNull(
                sessionId,
                "sessionId must not be null"
        );

        Objects.requireNonNull(
                answers,
                "answers must not be null"
        );

        answers =
                List.copyOf(
                        answers
                );
    }

    public record AnswerInput(
            String questionId,
            int value
    ) {
    }
}
