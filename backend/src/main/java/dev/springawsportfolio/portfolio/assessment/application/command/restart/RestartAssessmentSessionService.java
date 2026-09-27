package dev.springawsportfolio.portfolio.assessment.application.command.restart;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionAlreadyAbandonedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionAlreadyCompletedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
public class RestartAssessmentSessionService {

    private final AssessmentSessionRepository
            sessionRepository;

    private final AssessmentDefinitionRepository
            definitionRepository;

    private final AssessmentDefinitionVersionRepository
            versionRepository;

    private final Clock clock;

    public RestartAssessmentSessionService(
            AssessmentSessionRepository sessionRepository,
            AssessmentDefinitionRepository definitionRepository,
            AssessmentDefinitionVersionRepository versionRepository,
            Clock clock
    ) {
        this.sessionRepository =
                sessionRepository;

        this.definitionRepository =
                definitionRepository;

        this.versionRepository =
                versionRepository;

        this.clock =
                Objects.requireNonNull(
                        clock,
                        "clock must not be null"
                );
    }

    @Transactional
    public RestartAssessmentSessionResult execute(
            RestartAssessmentSessionCommand command
    ) {
        Objects.requireNonNull(
                command,
                "command must not be null"
        );

        AssessmentSession oldSession =
                sessionRepository
                        .findOwnedByIdForUpdate(
                                command.sessionId(),
                                command.actorUserId()
                        )
                        .orElseThrow(
                                AssessmentSessionNotFoundException::new
                        );

        validateRestartable(
                oldSession
        );

        AssessmentDefinition definition =
                definitionRepository
                        .findById(
                                oldSession.definitionId()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "bound AssessmentDefinition "
                                                        + "does not exist: "
                                                        + oldSession
                                                        .definitionId()
                                                        .value()
                                        )
                        );

        /*
         * Restart intentionally does NOT reuse the old bound version.
         *
         * A new attempt binds the current unique AVAILABLE version.
         */
        AssessmentDefinitionVersion replacementVersion =
                versionRepository
                        .findAvailableByDefinitionId(
                                oldSession.definitionId()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "no AVAILABLE "
                                                        + "AssessmentDefinitionVersion "
                                                        + "exists for definition: "
                                                        + oldSession
                                                        .definitionId()
                                                        .value()
                                        )
                        );

        if (
                !replacementVersion
                        .definitionId()
                        .equals(
                                oldSession.definitionId()
                        )
        ) {
            throw new IllegalStateException(
                    "replacement DefinitionVersion does not belong "
                            + "to the old Session's AssessmentDefinition"
            );
        }

        Instant now =
                clock.instant();

        oldSession.abandon(
                now
        );

        AssessmentSession replacement =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        oldSession.ownerUserId(),
                        oldSession.definitionId(),
                        replacementVersion.id(),
                        now
                );

        sessionRepository.replaceActive(
                oldSession,
                replacement
        );

        AssessmentSessionResult replacementResult =
                AssessmentSessionResult.from(
                        definition,
                        replacementVersion,
                        replacement
                );

        return new RestartAssessmentSessionResult(
                oldSession.id().value(),
                replacementResult
        );
    }

    private void validateRestartable(
            AssessmentSession session
    ) {
        if (
                session.status()
                        == AssessmentSessionStatus.ABANDONED
        ) {
            throw new AssessmentSessionAlreadyAbandonedException();
        }

        if (
                session.status()
                        == AssessmentSessionStatus.COMPLETED
        ) {
            throw new AssessmentSessionAlreadyCompletedException();
        }

        if (!session.isActive()) {
            throw new IllegalStateException(
                    "only an active AssessmentSession "
                            + "can be restarted"
            );
        }
    }
}