package dev.springawsportfolio.portfolio.assessment.application.session;

import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AssessmentSessionResult(
        UUID id,
        String assessmentCode,
        String assessmentVersion,
        AssessmentSessionStatus status,
        List<QuestionAnswerResult> answers,
        Instant questionnaireSubmittedAt,
        InitialAssessmentResult initialResult,
        List<DimensionEvidenceResult> dimensionEvidence,
        List<ClarificationStateResult> clarifications,
        List<TieBreakStateResult> tieBreaks,
        FinalAssessmentResult finalResult,
        WorkflowResult workflow,
        Instant createdAt,
        Instant completedAt,
        Instant abandonedAt
) {

    public AssessmentSessionResult {
        Objects.requireNonNull(
                id,
                "id must not be null"
        );

        Objects.requireNonNull(
                assessmentCode,
                "assessmentCode must not be null"
        );

        Objects.requireNonNull(
                assessmentVersion,
                "assessmentVersion must not be null"
        );

        Objects.requireNonNull(
                status,
                "status must not be null"
        );

        Objects.requireNonNull(
                answers,
                "answers must not be null"
        );

        Objects.requireNonNull(
                dimensionEvidence,
                "dimensionEvidence must not be null"
        );

        Objects.requireNonNull(
                clarifications,
                "clarifications must not be null"
        );

        Objects.requireNonNull(
                tieBreaks,
                "tieBreaks must not be null"
        );

        Objects.requireNonNull(
                workflow,
                "workflow must not be null"
        );

        Objects.requireNonNull(
                createdAt,
                "createdAt must not be null"
        );

        answers =
                List.copyOf(answers);

        dimensionEvidence =
                List.copyOf(dimensionEvidence);

        clarifications =
                List.copyOf(clarifications);

        tieBreaks =
                List.copyOf(tieBreaks);
    }

    /*
     * Backward-compatible constructor used by the existing
     * IN_PROGRESS-focused tests.
     */
    public AssessmentSessionResult(
            UUID id,
            String assessmentCode,
            String assessmentVersion,
            AssessmentSessionStatus status,
            List<QuestionAnswerResult> answers,
            Instant questionnaireSubmittedAt,
            Instant createdAt,
            Instant completedAt,
            Instant abandonedAt
    ) {
        this(
                id,
                assessmentCode,
                assessmentVersion,
                status,
                answers,
                questionnaireSubmittedAt,
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                new WorkflowResult(
                        List.of(),
                        List.of(),
                        List.of(),
                        status == AssessmentSessionStatus.COMPLETED
                ),
                createdAt,
                completedAt,
                abandonedAt
        );
    }

    public boolean questionnaireSubmitted() {
        return questionnaireSubmittedAt != null;
    }

    public static AssessmentSessionResult from(
            AssessmentDefinition definition,
            AssessmentDefinitionVersion version,
            AssessmentSession session
    ) {
        return from(
                definition,
                version,
                session,
                deriveCurrentClarifications(
                        session
                )
        );
    }

    public static AssessmentSessionResult from(
            AssessmentDefinition definition,
            AssessmentDefinitionVersion version,
            AssessmentSession session,
            List<DimensionClarification> clarifications
    ) {
        Objects.requireNonNull(
                definition,
                "definition must not be null"
        );

        Objects.requireNonNull(
                version,
                "version must not be null"
        );

        Objects.requireNonNull(
                session,
                "session must not be null"
        );

        Objects.requireNonNull(
                clarifications,
                "clarifications must not be null"
        );

        List<QuestionAnswerResult> answers =
                session
                        .questionnaireResponse()
                        .answers()
                        .stream()
                        .map(answer ->
                                new QuestionAnswerResult(
                                        answer
                                                .questionId()
                                                .value(),
                                        answer.value()
                                )
                        )
                        .toList();

        List<DimensionEvidenceResult> evidence =
                createDimensionEvidence(
                        version,
                        session.initialResult()
                );

        List<ClarificationStateResult>
                clarificationResults =
                clarifications
                        .stream()
                        .map(clarification ->
                                new ClarificationStateResult(
                                        clarification
                                                .dimension()
                                                .value(),
                                        clarification.status(),
                                        null,
                                        null
                                )
                        )
                        .toList();

        WorkflowResult workflow =
                createWorkflow(
                        session,
                        clarificationResults
                );

        return new AssessmentSessionResult(
                session.id().value(),
                definition.code(),
                version.versionCode(),
                session.status(),
                answers,
                session.questionnaireSubmittedAt(),
                session.initialResult(),
                evidence,
                clarificationResults,
                List.of(),
                session.finalResult(),
                workflow,
                session.createdAt(),
                session.completedAt(),
                session.abandonedAt()
        );
    }

    private static List<DimensionClarification>
    deriveCurrentClarifications(
            AssessmentSession session
    ) {
        if (
                session.initialResult() == null
                        || session.status()
                        != AssessmentSessionStatus
                        .AWAITING_CLARIFICATION
        ) {
            return List.of();
        }

        return session
                .initialResult()
                .ambiguousDimensions()
                .stream()
                .map(dimension ->
                        DimensionClarification.pending(
                                dev.springawsportfolio.portfolio
                                        .assessment.domain.clarification
                                        .DimensionClarificationId.newId(),
                                session.id(),
                                dimension,
                                session.questionnaireSubmittedAt()
                        )
                )
                .toList();
    }

    private static List<DimensionEvidenceResult>
    createDimensionEvidence(
            AssessmentDefinitionVersion version,
            InitialAssessmentResult initialResult
    ) {
        if (initialResult == null) {
            return List.of();
        }

        int maximumAbsoluteRawScore =
                version
                        .specification()
                        .scoringPolicy()
                        .maximumAbsoluteRawScore();

        if (maximumAbsoluteRawScore <= 0) {
            throw new IllegalStateException(
                    "maximumAbsoluteRawScore must be positive"
            );
        }

        return initialResult
                .dimensions()
                .stream()
                .map(result -> {
                    DimensionDefinition dimension =
                            version
                                    .specification()
                                    .dimensions()
                                    .stream()
                                    .filter(candidate ->
                                            candidate
                                                    .code()
                                                    .equals(
                                                            result.dimension()
                                                    )
                                    )
                                    .findFirst()
                                    .orElseThrow(
                                            () ->
                                                    new IllegalStateException(
                                                            "missing dimension definition: "
                                                                    + result
                                                                    .dimension()
                                                                    .value()
                                                    )
                                    );

                    double poleAPercentage =
                            50.0
                                    + (
                                    (double) result.rawScore()
                                            / maximumAbsoluteRawScore
                            ) * 50.0;

                    double poleBPercentage =
                            100.0
                                    - poleAPercentage;

                    return new DimensionEvidenceResult(
                            result.dimension().value(),
                            dimension.poleA().value(),
                            poleAPercentage,
                            dimension.poleB().value(),
                            poleBPercentage
                    );
                })
                .toList();
    }

    private static WorkflowResult createWorkflow(
            AssessmentSession session,
            List<ClarificationStateResult> clarifications
    ) {
        List<String> pendingDimensions =
                clarifications
                        .stream()
                        .filter(clarification ->
                                clarification.status()
                                        == DimensionClarificationStatus.PENDING
                        )
                        .map(
                                ClarificationStateResult::dimensionCode
                        )
                        .toList();

        List<String> retryableDimensions =
                clarifications
                        .stream()
                        .filter(clarification ->
                                clarification.status()
                                        == DimensionClarificationStatus
                                        .FAILED_RETRYABLE
                        )
                        .map(
                                ClarificationStateResult::dimensionCode
                        )
                        .toList();

        return new WorkflowResult(
                pendingDimensions,
                retryableDimensions,
                List.of(),
                session.status()
                        == AssessmentSessionStatus.COMPLETED
        );
    }

    public record QuestionAnswerResult(
            String questionId,
            int value
    ) {

        public QuestionAnswerResult {
            Objects.requireNonNull(
                    questionId,
                    "questionId must not be null"
            );
        }
    }

    public record DimensionEvidenceResult(
            String dimensionCode,
            String poleA,
            double poleAPercentage,
            String poleB,
            double poleBPercentage
    ) {
    }

    public record ClarificationStateResult(
            String dimensionCode,
            DimensionClarificationStatus status,
            Instant startedAt,
            Instant acceptedAt
    ) {
    }

    public record TieBreakStateResult(
            String dimensionCode,
            String selectedPole,
            Instant decidedAt
    ) {
    }

    public record WorkflowResult(
            List<String> pendingClarificationDimensions,
            List<String> retryableClarificationDimensions,
            List<String> tieBreakRequiredDimensions,
            boolean completed
    ) {

        public WorkflowResult {
            pendingClarificationDimensions =
                    List.copyOf(
                            pendingClarificationDimensions
                    );

            retryableClarificationDimensions =
                    List.copyOf(
                            retryableClarificationDimensions
                    );

            tieBreakRequiredDimensions =
                    List.copyOf(
                            tieBreakRequiredDimensions
                    );
        }
    }
}