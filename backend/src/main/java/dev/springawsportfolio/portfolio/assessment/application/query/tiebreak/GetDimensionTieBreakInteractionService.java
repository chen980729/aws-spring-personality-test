package dev.springawsportfolio.portfolio.assessment.application.query.tiebreak;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.TieBreakInteractionUnavailableException;
import dev.springawsportfolio.portfolio.assessment.application.exception.TieBreakNotRequiredException;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResolution;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.TieBreakQuestionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionClarificationRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionTieBreakRepository;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class GetDimensionTieBreakInteractionService {

    private final AssessmentSessionRepository
            sessionRepository;

    private final AssessmentDefinitionVersionRepository
            versionRepository;

    private final DimensionClarificationRepository
            clarificationRepository;

    private final DimensionTieBreakRepository
            tieBreakRepository;

    public GetDimensionTieBreakInteractionService(
            AssessmentSessionRepository sessionRepository,
            AssessmentDefinitionVersionRepository versionRepository,
            DimensionClarificationRepository clarificationRepository,
            DimensionTieBreakRepository tieBreakRepository
    ) {
        this.sessionRepository =
                Objects.requireNonNull(
                        sessionRepository,
                        "sessionRepository must not be null"
                );

        this.versionRepository =
                Objects.requireNonNull(
                        versionRepository,
                        "versionRepository must not be null"
                );

        this.clarificationRepository =
                Objects.requireNonNull(
                        clarificationRepository,
                        "clarificationRepository must not be null"
                );

        this.tieBreakRepository =
                Objects.requireNonNull(
                        tieBreakRepository,
                        "tieBreakRepository must not be null"
                );
    }

    @Transactional(readOnly = true)
    public DimensionTieBreakInteractionResult execute(
            UserId actorUserId,
            AssessmentSessionId sessionId,
            DimensionCode dimensionCode
    ) {
        Objects.requireNonNull(
                actorUserId,
                "actorUserId must not be null"
        );
        Objects.requireNonNull(
                sessionId,
                "sessionId must not be null"
        );
        Objects.requireNonNull(
                dimensionCode,
                "dimensionCode must not be null"
        );

        AssessmentSession session =
                sessionRepository
                        .findOwnedById(
                                sessionId,
                                actorUserId
                        )
                        .orElseThrow(
                                AssessmentSessionNotFoundException::new
                        );

        if (
                session.status()
                        == AssessmentSessionStatus.ABANDONED
        ) {
            throw new TieBreakInteractionUnavailableException(
                    "abandoned assessment does not expose tie-break interactions"
            );
        }

        AssessmentDefinitionVersion version =
                loadBoundVersion(
                        session
                );

        DimensionDefinition dimension =
                findDimension(
                        version,
                        dimensionCode
                );

        InitialDimensionResult initialDimension =
                findInitialDimension(
                        session,
                        dimensionCode
                );

        if (
                !initialDimension.ambiguous()
                        || !initialDimension.exactTie()
        ) {
            throw new TieBreakNotRequiredException(
                    "tie-break interaction is only available for an ambiguous exact tie"
            );
        }

        if (
                tieBreakRepository
                        .findBySessionIdAndDimension(
                                session.id(),
                                dimensionCode
                        )
                        .isPresent()
        ) {
            throw new TieBreakNotRequiredException(
                    "tie-break interaction is no longer pending"
            );
        }

        if (
                session.status()
                        == AssessmentSessionStatus.COMPLETED
        ) {
            throw new TieBreakNotRequiredException(
                    "completed assessment does not have a pending tie-break interaction"
            );
        }

        DimensionClarification clarification =
                clarificationRepository
                        .findBySessionIdAndDimension(
                                session.id(),
                                dimensionCode
                        )
                        .orElseThrow(
                                () ->
                                        new TieBreakNotRequiredException(
                                                "tie-break interaction is not ready for this dimension"
                                        )
                        );

        if (!requiresTieBreak(clarification)) {
            throw new TieBreakNotRequiredException(
                    "clarification has not left this exact tie unresolved"
            );
        }

        if (
                version
                        .specification()
                        .finalizationPolicy()
                        .usesLegacyDirectTieBreak()
        ) {
            return new DimensionTieBreakInteractionResult
                    .DirectPoleSelection(
                    dimension.code().value(),
                    List.of(
                            dimension.poleA().value(),
                            dimension.poleB().value()
                    )
            );
        }

        if (
                !version
                        .specification()
                        .finalizationPolicy()
                        .usesContextualTieBreakQuestions()
        ) {
            throw new IllegalStateException(
                    "unsupported finalization policy semantics"
            );
        }

        TieBreakQuestionDefinition question =
                version
                        .specification()
                        .tieBreakQuestions()
                        .stream()
                        .filter(candidate ->
                                candidate
                                        .dimension()
                                        .equals(
                                                dimension.code()
                                        )
                        )
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "contextual finalization policy is missing "
                                                        + "tie-break question for dimension "
                                                        + dimension.code().value()
                                        )
                        );

        return new DimensionTieBreakInteractionResult
                .ContextualQuestion(
                dimension.code().value(),
                question.questionId().value(),
                question.instruction(),
                question.prompt(),
                question
                        .options()
                        .stream()
                        .map(option ->
                                new DimensionTieBreakInteractionResult
                                        .Option(
                                        option
                                                .optionId()
                                                .value(),
                                        option.text()
                                )
                        )
                        .toList()
        );
    }

    private AssessmentDefinitionVersion loadBoundVersion(
            AssessmentSession session
    ) {
        AssessmentDefinitionVersion version =
                versionRepository
                        .findById(
                                session.definitionVersionId()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "bound assessment definition version does not exist"
                                        )
                        );

        if (
                !version.definitionId()
                        .equals(
                                session.definitionId()
                        )
        ) {
            throw new IllegalStateException(
                    "session definition/version binding is inconsistent"
            );
        }

        return version;
    }

    private DimensionDefinition findDimension(
            AssessmentDefinitionVersion version,
            DimensionCode dimensionCode
    ) {
        return version
                .specification()
                .dimensions()
                .stream()
                .filter(candidate ->
                        candidate
                                .code()
                                .equals(
                                        dimensionCode
                                )
                )
                .findFirst()
                .orElseThrow(
                        () ->
                                new TieBreakNotRequiredException(
                                        "dimension does not exist in the bound assessment version"
                                )
                );
    }

    private InitialDimensionResult findInitialDimension(
            AssessmentSession session,
            DimensionCode dimensionCode
    ) {
        if (session.initialResult() == null) {
            throw new TieBreakNotRequiredException(
                    "assessment has no submitted result requiring a tie-break"
            );
        }

        return session
                .initialResult()
                .dimensions()
                .stream()
                .filter(result ->
                        result
                                .dimension()
                                .equals(
                                        dimensionCode
                                )
                )
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "initial result is missing requested dimension"
                                )
                );
    }

    private boolean requiresTieBreak(
            DimensionClarification clarification
    ) {
        if (
                clarification.status()
                        == DimensionClarificationStatus.SKIPPED
        ) {
            return true;
        }

        return clarification.status()
                == DimensionClarificationStatus.CLARIFIED
                && clarification.result() != null
                && clarification
                .result()
                .resolution()
                == ClarificationResolution.UNCLEAR;
    }
}
