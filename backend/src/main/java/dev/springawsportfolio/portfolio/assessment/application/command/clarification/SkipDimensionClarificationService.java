package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.ClarificationNotAllowedException;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionWorkflowSnapshot;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionClarificationRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionTieBreakRepository;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import dev.springawsportfolio.portfolio.assessment.domain.service.AssessmentFinalizationService;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import dev.springawsportfolio.portfolio.assessment.domain.tiebreak.DimensionTieBreak;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class SkipDimensionClarificationService {

    private final AssessmentSessionRepository
            sessionRepository;

    private final AssessmentDefinitionRepository
            definitionRepository;

    private final AssessmentDefinitionVersionRepository
            versionRepository;

    private final DimensionClarificationRepository
            clarificationRepository;

    private final DimensionTieBreakRepository
            tieBreakRepository;

    private final Clock clock;

    private final AssessmentFinalizationService
            finalizationService;

    public SkipDimensionClarificationService(
            AssessmentSessionRepository sessionRepository,
            AssessmentDefinitionRepository definitionRepository,
            AssessmentDefinitionVersionRepository versionRepository,
            DimensionClarificationRepository clarificationRepository,
            DimensionTieBreakRepository tieBreakRepository,
            Clock clock
    ) {
        this.sessionRepository =
                Objects.requireNonNull(
                        sessionRepository,
                        "sessionRepository must not be null"
                );

        this.definitionRepository =
                Objects.requireNonNull(
                        definitionRepository,
                        "definitionRepository must not be null"
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

        this.clock =
                Objects.requireNonNull(
                        clock,
                        "clock must not be null"
                );

        this.finalizationService =
                new AssessmentFinalizationService();
    }

    @Transactional
    public AssessmentSessionResult execute(
            SkipDimensionClarificationCommand command
    ) {
        Objects.requireNonNull(
                command,
                "command must not be null"
        );

        AssessmentSession session =
                sessionRepository
                        .findOwnedByIdForUpdate(
                                command.sessionId(),
                                command.actorUserId()
                        )
                        .orElseThrow(
                                AssessmentSessionNotFoundException::new
                        );

        validateSessionState(
                session
        );

        AssessmentDefinitionVersion version =
                loadBoundVersion(
                        session
                );

        validateClarificationEligibility(
                session,
                command,
                version
        );

        DimensionClarification target =
                clarificationRepository
                        .findBySessionIdAndDimensionForUpdate(
                                session.id(),
                                command.dimension()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "eligible ambiguous dimension is missing "
                                                        + "its clarification lifecycle: "
                                                        + command
                                                        .dimension()
                                                        .value()
                                        )
                        );

        validateTargetState(
                session,
                target
        );

        List<DimensionClarification> clarifications =
                replaceTarget(
                        clarificationRepository.findBySessionId(
                                session.id()
                        ),
                        target
                );

        List<DimensionTieBreak> tieBreaks =
                tieBreakRepository.findBySessionId(
                        session.id()
                );

        Instant now =
                clock.instant();

        AssessmentSessionStatus originalStatus =
                session.status();

        target.skip(
                now
        );

        if (
                originalStatus
                        == AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS
        ) {
            session.returnToAwaitingClarification();
        }

        clarificationRepository.update(
                target
        );

        Optional<FinalAssessmentResult> finalResult =
                finalizationService.finalizeIfReady(
                        version.specification().dimensions(),
                        Objects.requireNonNull(
                                session.initialResult(),
                                "post-submission session must have initialResult"
                        ),
                        clarifications,
                        tieBreaks
                );

        boolean sessionChanged =
                originalStatus
                        == AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS;

        if (finalResult.isPresent()) {
            session.completeAfterClarification(
                    finalResult.orElseThrow(),
                    now
            );

            sessionChanged =
                    true;
        }

        if (sessionChanged) {
            sessionRepository.update(
                    session
            );
        }

        AssessmentDefinition definition =
                loadDefinition(
                        session
                );

        return AssessmentSessionResult.from(
                definition,
                version,
                session,
                AssessmentSessionWorkflowSnapshot.fromDomain(
                        clarifications,
                        tieBreaks
                )
        );
    }

    private void validateSessionState(
            AssessmentSession session
    ) {
        if (
                session.status()
                        != AssessmentSessionStatus.AWAITING_CLARIFICATION
                        && session.status()
                        != AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS
        ) {
            throw new ClarificationNotAllowedException(
                    "assessment session is not in a clarification state"
            );
        }
    }

    private void validateClarificationEligibility(
            AssessmentSession session,
            SkipDimensionClarificationCommand command,
            AssessmentDefinitionVersion version
    ) {
        boolean dimensionExists =
                version
                        .specification()
                        .dimensions()
                        .stream()
                        .anyMatch(dimension ->
                                dimension
                                        .code()
                                        .equals(
                                                command.dimension()
                                        )
                        );

        if (!dimensionExists) {
            throw new ClarificationNotAllowedException(
                    "dimension does not exist in the bound assessment version: "
                            + command.dimension().value()
            );
        }

        InitialDimensionResult initialDimension =
                Objects.requireNonNull(
                                session.initialResult(),
                                "post-submission session must have initialResult"
                        )
                        .dimensions()
                        .stream()
                        .filter(result ->
                                result
                                        .dimension()
                                        .equals(
                                                command.dimension()
                                        )
                        )
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "initial result is missing dimension: "
                                                        + command
                                                        .dimension()
                                                        .value()
                                        )
                        );

        if (!initialDimension.ambiguous()) {
            throw new ClarificationNotAllowedException(
                    "only an ambiguous dimension can be skipped"
            );
        }
    }

    private void validateTargetState(
            AssessmentSession session,
            DimensionClarification target
    ) {
        if (
                session.status()
                        == AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS
        ) {
            if (
                    target.status()
                            != DimensionClarificationStatus.IN_PROGRESS
            ) {
                throw new ClarificationNotAllowedException(
                        "while a clarification is in progress, only the "
                                + "current IN_PROGRESS dimension can be skipped"
                );
            }

            return;
        }

        if (
                target.status()
                        == DimensionClarificationStatus.IN_PROGRESS
        ) {
            throw new IllegalStateException(
                    "AWAITING_CLARIFICATION session must not have "
                            + "an IN_PROGRESS clarification"
            );
        }

        if (
                target.status()
                        != DimensionClarificationStatus.PENDING
                        && target.status()
                        != DimensionClarificationStatus.FAILED_RETRYABLE
        ) {
            throw new ClarificationNotAllowedException(
                    "clarification is already terminal and cannot be skipped"
            );
        }
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
                                                "bound assessment definition version "
                                                        + "does not exist: "
                                                        + session
                                                        .definitionVersionId()
                                                        .value()
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

    private AssessmentDefinition loadDefinition(
            AssessmentSession session
    ) {
        return definitionRepository
                .findById(
                        session.definitionId()
                )
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "bound assessment definition does not exist: "
                                                + session
                                                .definitionId()
                                                .value()
                                )
                );
    }

    private List<DimensionClarification> replaceTarget(
            List<DimensionClarification> clarifications,
            DimensionClarification target
    ) {
        boolean found =
                clarifications
                        .stream()
                        .anyMatch(clarification ->
                                clarification
                                        .dimension()
                                        .equals(
                                                target.dimension()
                                        )
                        );

        if (!found) {
            throw new IllegalStateException(
                    "target clarification is missing from Session workflow"
            );
        }

        return clarifications
                .stream()
                .map(clarification ->
                        clarification
                                .dimension()
                                .equals(
                                        target.dimension()
                                )
                                ? target
                                : clarification
                )
                .toList();
    }
}
