package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.ClarificationNotAllowedException;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationExecutionToken;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionClarificationRepository;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class RetryDimensionClarificationService {

    private final AssessmentSessionRepository
            sessionRepository;

    private final AssessmentDefinitionVersionRepository
            versionRepository;

    private final DimensionClarificationRepository
            clarificationRepository;

    private final Clock clock;

    public RetryDimensionClarificationService(
            AssessmentSessionRepository sessionRepository,
            AssessmentDefinitionVersionRepository versionRepository,
            DimensionClarificationRepository clarificationRepository,
            Clock clock
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

        this.clock =
                Objects.requireNonNull(
                        clock,
                        "clock must not be null"
                );
    }

    /**
     * Re-enters the same logical clarification Aggregate into active
     * execution. Retry never creates a second Session+Dimension lifecycle.
     */
    @Transactional
    public ClarificationExecutionTicket execute(
            RetryDimensionClarificationCommand command
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

        if (
                session.status()
                        != AssessmentSessionStatus.AWAITING_CLARIFICATION
        ) {
            throw new ClarificationNotAllowedException(
                    "assessment session is not awaiting clarification"
            );
        }

        AssessmentDefinitionVersion version =
                loadBoundVersion(
                        session
                );

        requireEligibleAmbiguousDimension(
                session,
                version,
                command.dimension()
        );

        List<DimensionClarification> clarifications =
                clarificationRepository.findBySessionId(
                        session.id()
                );

        requireNoInProgressClarification(
                clarifications
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

        if (
                target.status()
                        != DimensionClarificationStatus.FAILED_RETRYABLE
        ) {
            throw new ClarificationNotAllowedException(
                    "only a FAILED_RETRYABLE clarification can be retried"
            );
        }

        Instant now =
                clock.instant();

        ClarificationExecutionToken executionToken =
                target.retry(
                        now
                );

        session.beginClarification();

        clarificationRepository.update(
                target
        );

        sessionRepository.update(
                session
        );

        return new ClarificationExecutionTicket(
                command.actorUserId(),
                session.id(),
                command.dimension(),
                executionToken
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

    private void requireEligibleAmbiguousDimension(
            AssessmentSession session,
            AssessmentDefinitionVersion version,
            DimensionCode dimension
    ) {
        boolean dimensionExists =
                version
                        .specification()
                        .dimensions()
                        .stream()
                        .anyMatch(definition ->
                                definition
                                        .code()
                                        .equals(
                                                dimension
                                        )
                        );

        if (!dimensionExists) {
            throw new ClarificationNotAllowedException(
                    "dimension does not exist in the bound assessment version: "
                            + dimension.value()
            );
        }

        InitialDimensionResult initialDimension =
                Objects.requireNonNull(
                                session.initialResult(),
                                "clarification session must have initialResult"
                        )
                        .dimensions()
                        .stream()
                        .filter(result ->
                                result
                                        .dimension()
                                        .equals(
                                                dimension
                                        )
                        )
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "initial result is missing dimension: "
                                                        + dimension.value()
                                        )
                        );

        if (!initialDimension.ambiguous()) {
            throw new ClarificationNotAllowedException(
                    "only an ambiguous dimension may be clarified"
            );
        }
    }

    private void requireNoInProgressClarification(
            List<DimensionClarification> clarifications
    ) {
        boolean hasInProgress =
                clarifications
                        .stream()
                        .anyMatch(clarification ->
                                clarification.status()
                                        == DimensionClarificationStatus.IN_PROGRESS
                        );

        if (hasInProgress) {
            throw new IllegalStateException(
                    "AWAITING_CLARIFICATION session must not already contain "
                            + "an IN_PROGRESS clarification"
            );
        }
    }
}
