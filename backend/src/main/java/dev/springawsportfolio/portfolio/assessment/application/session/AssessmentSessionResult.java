package dev.springawsportfolio.portfolio.assessment.application.session;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
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
                createdAt,
                "createdAt must not be null"
        );

        answers = List.copyOf(answers);
    }

    public boolean questionnaireSubmitted() {
        return questionnaireSubmittedAt != null;
    }

    public static AssessmentSessionResult from(
            AssessmentDefinition definition,
            AssessmentDefinitionVersion version,
            AssessmentSession session
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

        List<QuestionAnswerResult> answers =
                session
                        .questionnaireResponse()
                        .answers()
                        .stream()
                        .map(answer ->
                                new QuestionAnswerResult(
                                        answer.questionId().value(),
                                        answer.value()
                                )
                        )
                        .toList();

        return new AssessmentSessionResult(
                session.id().value(),
                definition.code(),
                version.versionCode(),
                session.status(),
                answers,
                session.questionnaireSubmittedAt(),
                session.createdAt(),
                session.completedAt(),
                session.abandonedAt()
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
}