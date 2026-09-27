package dev.springawsportfolio.portfolio.assessment.application.session;

import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
                AssessmentSessionWorkflowSnapshot.empty()
        );
    }

    public static AssessmentSessionResult from(
            AssessmentDefinition definition,
            AssessmentDefinitionVersion version,
            AssessmentSession session,
            AssessmentSessionWorkflowSnapshot workflowSnapshot
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
                workflowSnapshot,
                "workflowSnapshot must not be null"
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

        List<ClarificationStateResult> clarificationResults =
                createClarificationResults(
                        version,
                        workflowSnapshot
                );

        List<TieBreakStateResult> tieBreakResults =
                createTieBreakResults(
                        version,
                        workflowSnapshot
                );

        WorkflowResult workflow =
                createWorkflow(
                        session,
                        clarificationResults,
                        tieBreakResults
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
                tieBreakResults,
                session.finalResult(),
                workflow,
                session.createdAt(),
                session.completedAt(),
                session.abandonedAt()
        );
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
                            findDimension(
                                    version,
                                    result
                                            .dimension()
                                            .value()
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

    private static List<ClarificationStateResult>
    createClarificationResults(
            AssessmentDefinitionVersion version,
            AssessmentSessionWorkflowSnapshot workflowSnapshot
    ) {
        return workflowSnapshot
                .clarifications()
                .stream()
                .sorted(
                        Comparator.comparingInt(
                                clarification ->
                                        dimensionPosition(
                                                version,
                                                clarification
                                                        .dimensionCode()
                                        )
                        )
                )
                .map(clarification ->
                        new ClarificationStateResult(
                                clarification.dimensionCode(),
                                clarification.status(),
                                clarification.resolution() == null
                                        ? null
                                        : new ClarificationResultData(
                                                clarification.resolution(),
                                                clarification.suggestedPole(),
                                                clarification.confidence(),
                                                clarification.reasoningSummary()
                                        ),
                                clarification.startedAt(),
                                clarification.acceptedAt()
                        )
                )
                .toList();
    }

    private static List<TieBreakStateResult>
    createTieBreakResults(
            AssessmentDefinitionVersion version,
            AssessmentSessionWorkflowSnapshot workflowSnapshot
    ) {
        return workflowSnapshot
                .tieBreaks()
                .stream()
                .sorted(
                        Comparator.comparingInt(
                                tieBreak ->
                                        dimensionPosition(
                                                version,
                                                tieBreak.dimensionCode()
                                        )
                        )
                )
                .map(tieBreak ->
                        new TieBreakStateResult(
                                tieBreak.dimensionCode(),
                                tieBreak.selectedPole(),
                                tieBreak.decidedAt()
                        )
                )
                .toList();
    }

    private static WorkflowResult createWorkflow(
            AssessmentSession session,
            List<ClarificationStateResult> clarifications,
            List<TieBreakStateResult> tieBreaks
    ) {
        if (!session.isActive()) {
            return new WorkflowResult(
                    List.of(),
                    List.of(),
                    List.of(),
                    session.status()
                            == AssessmentSessionStatus.COMPLETED
            );
        }

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

        List<String> tieBreakRequiredDimensions =
                createTieBreakRequiredDimensions(
                        session,
                        clarifications,
                        tieBreaks
                );

        return new WorkflowResult(
                pendingDimensions,
                retryableDimensions,
                tieBreakRequiredDimensions,
                session.status()
                        == AssessmentSessionStatus.COMPLETED
        );
    }

    private static List<String>
    createTieBreakRequiredDimensions(
            AssessmentSession session,
            List<ClarificationStateResult> clarifications,
            List<TieBreakStateResult> tieBreaks
    ) {
        if (session.initialResult() == null) {
            return List.of();
        }

        Set<String> decidedDimensions =
                tieBreaks
                        .stream()
                        .map(
                                TieBreakStateResult::dimensionCode
                        )
                        .collect(
                                Collectors.toSet()
                        );

        return session
                .initialResult()
                .dimensions()
                .stream()
                .filter(result ->
                        result.ambiguous()
                                && result.exactTie()
                )
                .map(result ->
                        result
                                .dimension()
                                .value()
                )
                .filter(dimensionCode ->
                        !decidedDimensions.contains(
                                dimensionCode
                        )
                )
                .filter(dimensionCode ->
                        clarificationRequiresTieBreak(
                                dimensionCode,
                                clarifications
                        )
                )
                .toList();
    }

    private static boolean clarificationRequiresTieBreak(
            String dimensionCode,
            List<ClarificationStateResult> clarifications
    ) {
        return clarifications
                .stream()
                .filter(clarification ->
                        clarification
                                .dimensionCode()
                                .equals(
                                        dimensionCode
                                )
                )
                .findFirst()
                .map(clarification ->
                        clarification.status()
                                == DimensionClarificationStatus.SKIPPED
                                || (
                                clarification.status()
                                        == DimensionClarificationStatus.CLARIFIED
                                        && clarification.result() != null
                                        && clarification
                                        .result()
                                        .resolution()
                                        == AssessmentSessionWorkflowSnapshot
                                        .ClarificationResolution
                                        .UNCLEAR
                        )
                )
                .orElse(false);
    }

    private static int dimensionPosition(
            AssessmentDefinitionVersion version,
            String dimensionCode
    ) {
        return findDimension(
                version,
                dimensionCode
        ).position();
    }

    private static DimensionDefinition findDimension(
            AssessmentDefinitionVersion version,
            String dimensionCode
    ) {
        return version
                .specification()
                .dimensions()
                .stream()
                .filter(candidate ->
                        candidate
                                .code()
                                .value()
                                .equals(
                                        dimensionCode
                                )
                )
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "missing dimension definition: "
                                                + dimensionCode
                                )
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
            ClarificationResultData result,
            Instant startedAt,
            Instant acceptedAt
    ) {
    }

    public record ClarificationResultData(
            AssessmentSessionWorkflowSnapshot.ClarificationResolution resolution,
            String suggestedPole,
            String confidence,
            String reasoningSummary
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
