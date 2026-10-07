package dev.springawsportfolio.portfolio.assessment.application.port.out;

import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationConfidence;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResolution;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;

import java.util.Objects;

public sealed interface ClarificationAiTurn
        permits ClarificationAiTurn.FollowUpQuestion,
                ClarificationAiTurn.Judgement {

    record FollowUpQuestion(
            String text
    ) implements ClarificationAiTurn {

        public FollowUpQuestion {
            requireNonBlank(
                    text,
                    "text"
            );
        }
    }

    record Judgement(
            ClarificationResolution resolution,
            PoleCode suggestedPole,
            ClarificationConfidence confidence,
            String reasoningSummary,
            ModelIdentity modelIdentity
    ) implements ClarificationAiTurn {

        public Judgement {
            Objects.requireNonNull(
                    resolution,
                    "resolution must not be null"
            );
            Objects.requireNonNull(
                    confidence,
                    "confidence must not be null"
            );
            Objects.requireNonNull(
                    modelIdentity,
                    "modelIdentity must not be null"
            );

            if (
                    resolution
                            == ClarificationResolution.RESOLVED
                            && suggestedPole == null
            ) {
                throw new IllegalArgumentException(
                        "RESOLVED judgement must have suggestedPole"
                );
            }

            if (
                    resolution
                            == ClarificationResolution.UNCLEAR
                            && suggestedPole != null
            ) {
                throw new IllegalArgumentException(
                        "UNCLEAR judgement must not have suggestedPole"
                );
            }

            if (
                    reasoningSummary != null
                            && reasoningSummary.isBlank()
            ) {
                throw new IllegalArgumentException(
                        "reasoningSummary must be null or non-blank"
                );
            }
        }
    }

    record ModelIdentity(
            String provider,
            String modelIdentifier
    ) {

        public ModelIdentity {
            requireNonBlank(
                    provider,
                    "provider"
            );
            requireNonBlank(
                    modelIdentifier,
                    "modelIdentifier"
            );
        }
    }

    private static void requireNonBlank(
            String value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " must not be null"
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }
    }
}
