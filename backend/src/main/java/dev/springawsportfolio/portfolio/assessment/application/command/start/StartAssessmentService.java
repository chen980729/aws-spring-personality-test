package dev.springawsportfolio.portfolio.assessment.application.command.start;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Objects;

@Service
public class StartAssessmentService {

    private final AssessmentDefinitionRepository
            definitionRepository;

    private final AssessmentDefinitionVersionRepository
            versionRepository;

    private final AssessmentSessionRepository
            sessionRepository;

    private final Clock clock;

    public StartAssessmentService(
            AssessmentDefinitionRepository definitionRepository,
            AssessmentDefinitionVersionRepository versionRepository,
            AssessmentSessionRepository sessionRepository,
            Clock clock
    ) {
        this.definitionRepository =
                definitionRepository;

        this.versionRepository =
                versionRepository;

        this.sessionRepository =
                sessionRepository;

        this.clock =
                Objects.requireNonNull(
                        clock,
                        "clock must not be null"
                );
    }

    @Transactional
    public StartAssessmentResult execute(
            UserId actorUserId,
            String assessmentCode
    ) {
        Objects.requireNonNull(
                actorUserId,
                "actorUserId must not be null"
        );

        requireAssessmentCode(
                assessmentCode
        );

        AssessmentDefinition definition =
                definitionRepository
                        .findByCode(
                                assessmentCode
                        )
                        .orElseThrow(
                                () ->
                                        new AssessmentNotFoundException(
                                                assessmentCode
                                        )
                        );

        /*
         * Resume must be checked before resolving the
         * currently AVAILABLE DefinitionVersion.
         *
         * An existing Session always remains bound to the
         * exact DefinitionVersion with which it was started.
         */
        var existingActive =
                sessionRepository.findActive(
                        actorUserId,
                        definition.id()
                );

        if (existingActive.isPresent()) {
            return resumedResult(
                    definition,
                    existingActive.get()
            );
        }

        AssessmentDefinitionVersion availableVersion =
                versionRepository
                        .findAvailableByDefinitionId(
                                definition.id()
                        )
                        .orElseThrow(
                                () ->
                                        new AssessmentNotFoundException(
                                                assessmentCode
                                        )
                        );

        AssessmentSession candidate =
                AssessmentSession.start(
                        AssessmentSessionId.newId(),
                        actorUserId,
                        definition.id(),
                        availableVersion.id(),
                        clock.instant()
                );

        boolean created =
                sessionRepository.tryCreateActive(
                        candidate
                );

        if (created) {
            return result(
                    true,
                    definition,
                    availableVersion,
                    candidate
            );
        }

        /*
         * Another concurrent request won the race.
         *
         * tryCreateActive uses INSERT ... ON CONFLICT
         * DO NOTHING, so the current transaction remains
         * valid and we can load the authoritative winner.
         */
        AssessmentSession winner =
                sessionRepository
                        .findActive(
                                actorUserId,
                                definition.id()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "active assessment session "
                                                        + "was not found after "
                                                        + "concurrent creation"
                                        )
                        );

        return resumedResult(
                definition,
                winner
        );
    }

    private StartAssessmentResult resumedResult(
            AssessmentDefinition definition,
            AssessmentSession session
    ) {
        AssessmentDefinitionVersion boundVersion =
                versionRepository
                        .findById(
                                session.definitionVersionId()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "bound assessment definition "
                                                        + "version does not exist: "
                                                        + session
                                                        .definitionVersionId()
                                                        .value()
                                        )
                        );

        return result(
                false,
                definition,
                boundVersion,
                session
        );
    }

    private StartAssessmentResult result(
            boolean created,
            AssessmentDefinition definition,
            AssessmentDefinitionVersion version,
            AssessmentSession session
    ) {
        return new StartAssessmentResult(
                created,
                AssessmentSessionResult.from(
                        definition,
                        version,
                        session
                )
        );
    }

    private static void requireAssessmentCode(
            String assessmentCode
    ) {
        Objects.requireNonNull(
                assessmentCode,
                "assessmentCode must not be null"
        );

        if (assessmentCode.isBlank()) {
            throw new IllegalArgumentException(
                    "assessmentCode must not be blank"
            );
        }
    }
}
