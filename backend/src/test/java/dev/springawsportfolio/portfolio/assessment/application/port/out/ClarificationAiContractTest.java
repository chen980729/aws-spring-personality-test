package dev.springawsportfolio.portfolio.assessment.application.port.out;

import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationConfidence;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResolution;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClarificationAiContractTest {

    private static final DimensionCode EI =
            new DimensionCode("EI");

    private static final PoleCode E =
            new PoleCode("E");

    private static final PoleCode I =
            new PoleCode("I");

    private static final ClarificationAiRequest.TargetDimension
            TARGET_DIMENSION =
            new ClarificationAiRequest.TargetDimension(
                    EI,
                    E,
                    I
            );

    private static final ClarificationAiRequest.QuestionnaireEvidence
            EVIDENCE =
            new ClarificationAiRequest.QuestionnaireEvidence(
                    "I feel energized after social interaction.",
                    "Agree",
                    E
            );

    private static final ClarificationAiTurn.ModelIdentity
            MODEL_IDENTITY =
            new ClarificationAiTurn.ModelIdentity(
                    "TEST_PROVIDER",
                    "test-model"
            );

    @Test
    void requestAcceptsFirstModelTurnWithoutConversation() {
        ClarificationAiRequest request =
                new ClarificationAiRequest(
                        TARGET_DIMENSION,
                        List.of(EVIDENCE),
                        List.of(),
                        1,
                        3,
                        "v1"
                );

        assertEquals(
                1,
                request.currentModelTurn()
        );
        assertTrue(
                !request.finalModelTurn()
        );
    }

    @Test
    void requestAcceptsAnsweredFollowUpHistory() {
        ClarificationAiRequest request =
                new ClarificationAiRequest(
                        TARGET_DIMENSION,
                        List.of(EVIDENCE),
                        List.of(
                                new ClarificationAiRequest
                                        .ConversationMessage(
                                        ClarificationAiRequest
                                                .ConversationRole
                                                .ASSISTANT_QUESTION,
                                        "How do you usually recharge?"
                                ),
                                new ClarificationAiRequest
                                        .ConversationMessage(
                                        ClarificationAiRequest
                                                .ConversationRole
                                                .USER_ANSWER,
                                        "Usually by spending time alone."
                                )
                        ),
                        2,
                        3,
                        "v1"
                );

        assertEquals(
                2,
                request.conversation().size()
        );
    }

    @Test
    void requestDetectsFinalModelTurn() {
        ClarificationAiRequest request =
                new ClarificationAiRequest(
                        TARGET_DIMENSION,
                        List.of(EVIDENCE),
                        List.of(
                                new ClarificationAiRequest
                                        .ConversationMessage(
                                        ClarificationAiRequest
                                                .ConversationRole
                                                .ASSISTANT_QUESTION,
                                        "First follow-up?"
                                ),
                                new ClarificationAiRequest
                                        .ConversationMessage(
                                        ClarificationAiRequest
                                                .ConversationRole
                                                .USER_ANSWER,
                                        "First answer."
                                ),
                                new ClarificationAiRequest
                                        .ConversationMessage(
                                        ClarificationAiRequest
                                                .ConversationRole
                                                .ASSISTANT_QUESTION,
                                        "Second follow-up?"
                                ),
                                new ClarificationAiRequest
                                        .ConversationMessage(
                                        ClarificationAiRequest
                                                .ConversationRole
                                                .USER_ANSWER,
                                        "Second answer."
                                )
                        ),
                        3,
                        3,
                        "v1"
                );

        assertTrue(
                request.finalModelTurn()
        );
    }

    @Test
    void requestRejectsEmptyQuestionnaireEvidence() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ClarificationAiRequest(
                        TARGET_DIMENSION,
                        List.of(),
                        List.of(),
                        1,
                        3,
                        "v1"
                )
        );
    }

    @Test
    void requestRejectsEvidenceOutsideTargetDimension() {
        ClarificationAiRequest.QuestionnaireEvidence invalid =
                new ClarificationAiRequest.QuestionnaireEvidence(
                        "I prefer abstract ideas.",
                        "Agree",
                        new PoleCode("N")
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ClarificationAiRequest(
                        TARGET_DIMENSION,
                        List.of(invalid),
                        List.of(),
                        1,
                        3,
                        "v1"
                )
        );
    }

    @Test
    void requestRejectsConversationThatDoesNotMatchTurn() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ClarificationAiRequest(
                        TARGET_DIMENSION,
                        List.of(EVIDENCE),
                        List.of(),
                        2,
                        3,
                        "v1"
                )
        );
    }

    @Test
    void requestRejectsConversationWithWrongRoleOrder() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ClarificationAiRequest(
                        TARGET_DIMENSION,
                        List.of(EVIDENCE),
                        List.of(
                                new ClarificationAiRequest
                                        .ConversationMessage(
                                        ClarificationAiRequest
                                                .ConversationRole
                                                .USER_ANSWER,
                                        "Answer before a question."
                                ),
                                new ClarificationAiRequest
                                        .ConversationMessage(
                                        ClarificationAiRequest
                                                .ConversationRole
                                                .ASSISTANT_QUESTION,
                                        "Question after the answer."
                                )
                        ),
                        2,
                        3,
                        "v1"
                )
        );
    }

    @Test
    void requestRejectsInvalidTurnRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ClarificationAiRequest(
                        TARGET_DIMENSION,
                        List.of(EVIDENCE),
                        List.of(),
                        0,
                        3,
                        "v1"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ClarificationAiRequest(
                        TARGET_DIMENSION,
                        List.of(EVIDENCE),
                        List.of(),
                        4,
                        3,
                        "v1"
                )
        );
    }

    @Test
    void requestDefensivelyCopiesCollections() {
        List<ClarificationAiRequest.QuestionnaireEvidence>
                mutableEvidence =
                new ArrayList<>();

        mutableEvidence.add(
                EVIDENCE
        );

        ClarificationAiRequest request =
                new ClarificationAiRequest(
                        TARGET_DIMENSION,
                        mutableEvidence,
                        List.of(),
                        1,
                        3,
                        "v1"
                );

        mutableEvidence.clear();

        assertEquals(
                1,
                request.questionnaireEvidence().size()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> request.questionnaireEvidence()
                        .add(EVIDENCE)
        );
    }

    @Test
    void followUpQuestionRejectsBlankText() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ClarificationAiTurn
                        .FollowUpQuestion(" ")
        );
    }

    @Test
    void resolvedJudgementRequiresSuggestedPole() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ClarificationAiTurn.Judgement(
                        ClarificationResolution.RESOLVED,
                        null,
                        ClarificationConfidence.MEDIUM,
                        "Evidence leans toward E.",
                        MODEL_IDENTITY
                )
        );
    }

    @Test
    void unclearJudgementForbidsSuggestedPole() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ClarificationAiTurn.Judgement(
                        ClarificationResolution.UNCLEAR,
                        E,
                        ClarificationConfidence.LOW,
                        "Evidence remains mixed.",
                        MODEL_IDENTITY
                )
        );
    }

    @Test
    void judgementRequiresConfidence() {
        assertThrows(
                NullPointerException.class,
                () -> new ClarificationAiTurn.Judgement(
                        ClarificationResolution.RESOLVED,
                        E,
                        null,
                        "Evidence leans toward E.",
                        MODEL_IDENTITY
                )
        );
    }

    @Test
    void judgementRejectsBlankReasoningSummary() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ClarificationAiTurn.Judgement(
                        ClarificationResolution.RESOLVED,
                        E,
                        ClarificationConfidence.HIGH,
                        " ",
                        MODEL_IDENTITY
                )
        );
    }

    @Test
    void modelIdentityRejectsBlankValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ClarificationAiTurn.ModelIdentity(
                        " ",
                        "test-model"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ClarificationAiTurn.ModelIdentity(
                        "TEST_PROVIDER",
                        " "
                )
        );
    }
}
