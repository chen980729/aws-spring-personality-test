package dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionnaireResponseTest {

    @Test
    void createsEmptySnapshot() {
        QuestionnaireResponse response =
                QuestionnaireResponse.empty();

        assertTrue(
                response.answers().isEmpty()
        );
    }

    @Test
    void acceptsAnswersForDifferentQuestions() {
        QuestionnaireResponse response =
                new QuestionnaireResponse(
                        List.of(
                                new Answer(
                                        new QuestionId("Q1"),
                                        5
                                ),
                                new Answer(
                                        new QuestionId("Q2"),
                                        3
                                )
                        )
                );

        assertEquals(
                2,
                response.answers().size()
        );
    }

    @Test
    void rejectsDuplicateQuestionAnswers() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new QuestionnaireResponse(
                                List.of(
                                        new Answer(
                                                new QuestionId("Q1"),
                                                5
                                        ),
                                        new Answer(
                                                new QuestionId("Q1"),
                                                3
                                        )
                                )
                        )
        );
    }
}