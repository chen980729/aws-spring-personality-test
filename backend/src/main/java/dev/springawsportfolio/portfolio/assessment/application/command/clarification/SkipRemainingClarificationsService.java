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
public class SkipRemainingClarificationsService {

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

    public SkipRemainingClarificationsService(
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
            SkipRemainingClarificationsCommand command
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

        List<DimensionClarification> clarifications =
                clarificationRepository.findBySessionId(
                        session.id()
                );

        validateWorkflowConsistency(
                session,
                clarifications
        );

        List<DimensionTieBreak> tieBreaks =
                tieBreakRepository.findBySessionId(
                        session.id()
                );

        Instant now =
                clock.instant();

        boolean hadInProgress =
                session.status()
                        == AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS;

        for (
                DimensionClarification clarification
                : clarifications
        ) {
            if (
                    clarification.status()
                            == DimensionClarificationStatus.PENDING
                            || clarification.status()
                            == DimensionClarificationStatus.IN_PROGRESS
                            || clarification.status()
                            == DimensionClarificationStatus.FAILED_RETRYABLE
            ) {
                clarification.skip(
                        now
                );

                clarificationRepository.update(
                        clarification
                );
            }
        }

        if (hadInProgress) {
            session.returnToAwaitingClarification();
        }

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
                hadInProgress;

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

    private void validateWorkflowConsistency(
            AssessmentSession session,
            List<DimensionClarification> clarifications
    ) {
        long inProgressCount =
                clarifications
                        .stream()
                        .filter(clarification ->
                                clarification.status()
                                        == DimensionClarificationStatus.IN_PROGRESS
                        )
                        .count();

        if (
                session.status()
                        == AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS
                        && inProgressCount != 1
        ) {
            throw new IllegalStateException(
                    "CLARIFICATION_IN_PROGRESS session must have exactly "
                            + "one IN_PROGRESS clarification"
            );
        }

        if (
                session.status()
                        == AssessmentSessionStatus.AWAITING_CLARIFICATION
                        && inProgressCount != 0
        ) {
            throw new IllegalStateException(
                    "AWAITING_CLARIFICATION session must not have "
                            + "an IN_PROGRESS clarification"
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
}
