package dev.springawsportfolio.portfolio.assessment.application.session;

import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AssessmentSessionWorkflowSnapshot(
        List<ClarificationSnapshot> clarifications,
        List<TieBreakSnapshot> tieBreaks
) {

    public AssessmentSessionWorkflowSnapshot {
        Objects.requireNonNull(
                clarifications,
                "clarifications must not be null"
        );

        Objects.requireNonNull(
                tieBreaks,
                "tieBreaks must not be null"
        );

        clarifications =
                List.copyOf(
                        clarifications
                );

        tieBreaks =
                List.copyOf(
                        tieBreaks
                );
    }

    public static AssessmentSessionWorkflowSnapshot empty() {
        return new AssessmentSessionWorkflowSnapshot(
                List.of(),
                List.of()
        );
    }

    public static AssessmentSessionWorkflowSnapshot
    fromPendingClarifications(
            List<DimensionClarification> clarifications
    ) {
        Objects.requireNonNull(
                clarifications,
                "clarifications must not be null"
        );

        List<ClarificationSnapshot> snapshots =
                clarifications
                        .stream()
                        .map(clarification -> {
                            if (
                                    clarification.status()
                                            != DimensionClarificationStatus.PENDING
                            ) {
                                throw new IllegalArgumentException(
                                        "only PENDING clarifications can be "
                                                + "converted without persisted result data"
                                );
                            }

                            return new ClarificationSnapshot(
                                    clarification
                                            .dimension()
                                            .value(),
                                    clarification.status(),
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null
                            );
                        })
                        .toList();

        return new AssessmentSessionWorkflowSnapshot(
                snapshots,
                List.of()
        );
    }

    public enum ClarificationResolution {
        RESOLVED,
        UNCLEAR
    }

    public record ClarificationSnapshot(
            String dimensionCode,
            DimensionClarificationStatus status,
            ClarificationResolution resolution,
            String suggestedPole,
            String confidence,
            String reasoningSummary,
            Instant startedAt,
            Instant acceptedAt
    ) {

        public ClarificationSnapshot {
            Objects.requireNonNull(
                    dimensionCode,
                    "dimensionCode must not be null"
            );

            if (dimensionCode.isBlank()) {
                throw new IllegalArgumentException(
                        "dimensionCode must not be blank"
                );
            }

            Objects.requireNonNull(
                    status,
                    "status must not be null"
            );

            if (status == DimensionClarificationStatus.CLARIFIED) {
                Objects.requireNonNull(
                        resolution,
                        "CLARIFIED clarification must have resolution"
                );

                Objects.requireNonNull(
                        confidence,
                        "CLARIFIED clarification must have confidence"
                );

                Objects.requireNonNull(
                        acceptedAt,
                        "CLARIFIED clarification must have acceptedAt"
                );

                if (
                        resolution == ClarificationResolution.RESOLVED
                                && (
                                suggestedPole == null
                                        || suggestedPole.isBlank()
                        )
                ) {
                    throw new IllegalArgumentException(
                            "RESOLVED clarification must have suggestedPole"
                    );
                }

                if (
                        resolution == ClarificationResolution.UNCLEAR
                                && suggestedPole != null
                ) {
                    throw new IllegalArgumentException(
                            "UNCLEAR clarification must not have suggestedPole"
                    );
                }
            } else if (
                    resolution != null
                            || suggestedPole != null
                            || confidence != null
                            || reasoningSummary != null
                            || acceptedAt != null
            ) {
                throw new IllegalArgumentException(
                        "non-CLARIFIED clarification must not have "
                                + "accepted result data"
                );
            }
        }
    }

    public record TieBreakSnapshot(
            String dimensionCode,
            String selectedPole,
            Instant decidedAt
    ) {

        public TieBreakSnapshot {
            Objects.requireNonNull(
                    dimensionCode,
                    "dimensionCode must not be null"
            );

            if (dimensionCode.isBlank()) {
                throw new IllegalArgumentException(
                        "dimensionCode must not be blank"
                );
            }

            Objects.requireNonNull(
                    selectedPole,
                    "selectedPole must not be null"
            );

            if (selectedPole.isBlank()) {
                throw new IllegalArgumentException(
                        "selectedPole must not be blank"
                );
            }

            Objects.requireNonNull(
                    decidedAt,
                    "decidedAt must not be null"
            );
        }
    }
}
