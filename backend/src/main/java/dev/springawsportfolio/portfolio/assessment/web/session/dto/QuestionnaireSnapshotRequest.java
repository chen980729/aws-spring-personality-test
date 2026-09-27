package dev.springawsportfolio.portfolio.assessment.web.session.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuestionnaireSnapshotRequest(

        @NotNull
        List<@NotNull @Valid QuestionAnswerRequest> answers

) {

    public record QuestionAnswerRequest(

            @NotBlank
            String questionId,

            @NotNull
            Integer value

    ) {
    }
}
