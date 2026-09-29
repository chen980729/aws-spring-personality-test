package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionWorkflowSnapshot;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.AIProvenance;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResolution;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResult;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class CompleteClarificationExecutionService {

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

    public CompleteClarificationExecutionService(
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

    /**
     * Transaction B success path.
     *
     * The caller must have completed external work before entering this
     * method. The Session and target Clarification are reloaded under lock;
     * stale work is discarded rather than mutating newer workflow state.
     */
    @Transactional
    public ClarificationExecutionCompletionResult accept(
            ClarificationExecutionTicket ticket,
            ClarificationResult result,
            AIProvenance aiProvenance
    ) {
        Objects.requireNonNull(
                ticket,
                "ticket must not be null"
        );

        Objects.requireNonNull(
                result,
                "result must not be null"
        );

        Objects.requireNonNull(
                aiProvenance,
                "aiProvenance must not be null"
        );

        Optional<AssessmentSession> optionalSession =
                sessionRepository.findOwnedByIdForUpdate(
                        ticket.sessionId(),
                        ticket.actorUserId()
                );

        if (optionalSession.isEmpty()) {
            return ClarificationExecutionCompletionResult
                    .staleDiscarded();
        }

        AssessmentSession session =
                optionalSession.orElseThrow();

        if (
                session.status()
                        != AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS
        ) {
            return ClarificationExecutionCompletionResult
                    .staleDiscarded();
        }

        AssessmentDefinitionVersion version =
                loadBoundVersion(
                        session
                );

        DimensionDefinition dimension =
                requireEligibleAmbiguousDimension(
                        session,
                        version,
                        ticket.dimension()
                );

        DimensionClarification target =
                clarificationRepository
                        .findBySessionIdAndDimensionForUpdate(
                                session.id(),
                                ticket.dimension()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "CLARIFICATION_IN_PROGRESS session is missing "
                                                        + "target clarification: "
                                                        + ticket
                                                        .dimension()
                                                        .value()
                                        )
                        );

        if (
                target.status()
                        != DimensionClarificationStatus.IN_PROGRESS
                        || !target.matchesActiveExecution(
                        ticket.executionToken()
                )
        ) {
            return ClarificationExecutionCompletionResult
                    .staleDiscarded();
        }

        validateAcceptedResult(
                dimension,
                result
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

        target.acceptResult(
                ticket.executionToken(),
                result,
                aiProvenance,
                now
        );

        session.returnToAwaitingClarification();

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

        if (finalResult.isPresent()) {
            session.completeAfterClarification(
                    finalResult.orElseThrow(),
                    now
            );
        }

        sessionRepository.update(
                session
        );

        AssessmentDefinition definition =
                loadDefinition(
                        session
                );

        return ClarificationExecutionCompletionResult.accepted(
                AssessmentSessionResult.from(
                        definition,
                        version,
                        session,
                        AssessmentSessionWorkflowSnapshot.fromDomain(
                                clarifications,
                                tieBreaks
                        )
                )
        );
    }

    /**
     * Transaction B technical-failure path.
     *
     * Provider timeout, transport errors, and invalid structured output are
     * recoverable technical failures. They return the Session to
     * AWAITING_CLARIFICATION and leave the same logical clarification in
     * FAILED_RETRYABLE.
     */
    @Transactional
    public ClarificationExecutionCompletionResult failRetryable(
            ClarificationExecutionTicket ticket
    ) {
        Objects.requireNonNull(
                ticket,
                "ticket must not be null"
        );

        Optional<AssessmentSession> optionalSession =
                sessionRepository.findOwnedByIdForUpdate(
                        ticket.sessionId(),
                        ticket.actorUserId()
                );

        if (optionalSession.isEmpty()) {
            return ClarificationExecutionCompletionResult
                    .staleDiscarded();
        }

        AssessmentSession session =
                optionalSession.orElseThrow();

        if (
                session.status()
                        != AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS
        ) {
            return ClarificationExecutionCompletionResult
                    .staleDiscarded();
        }

        AssessmentDefinitionVersion version =
                loadBoundVersion(
                        session
                );

        requireEligibleAmbiguousDimension(
                session,
                version,
                ticket.dimension()
        );

        DimensionClarification target =
                clarificationRepository
                        .findBySessionIdAndDimensionForUpdate(
                                session.id(),
                                ticket.dimension()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "CLARIFICATION_IN_PROGRESS session is missing "
                                                        + "target clarification: "
                                                        + ticket
                                                        .dimension()
                                                        .value()
                                        )
                        );

        if (
                target.status()
                        != DimensionClarificationStatus.IN_PROGRESS
                        || !target.matchesActiveExecution(
                        ticket.executionToken()
                )
        ) {
            return ClarificationExecutionCompletionResult
                    .staleDiscarded();
        }

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

        target.failRetryable(
                ticket.executionToken(),
                now
        );

        session.returnToAwaitingClarification();

        clarificationRepository.update(
                target
        );

        sessionRepository.update(
                session
        );

        return ClarificationExecutionCompletionResult.accepted(
                AssessmentSessionResult.from(
                        loadDefinition(
                                session
                        ),
                        version,
                        session,
                        AssessmentSessionWorkflowSnapshot.fromDomain(
                                clarifications,
                                tieBreaks
                        )
                )
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

    private DimensionDefinition requireEligibleAmbiguousDimension(
            AssessmentSession session,
            AssessmentDefinitionVersion version,
            DimensionCode dimensionCode
    ) {
        DimensionDefinition dimension =
                version
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
                                        new IllegalStateException(
                                                "clarification dimension does not exist in "
                                                        + "the bound assessment version: "
                                                        + dimensionCode.value()
                                        )
                        );

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
                                                dimensionCode
                                        )
                        )
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "initial result is missing dimension: "
                                                        + dimensionCode.value()
                                        )
                        );

        if (!initialDimension.ambiguous()) {
            throw new IllegalStateException(
                    "active clarification points to a non-ambiguous dimension"
            );
        }

        return dimension;
    }

    private void validateAcceptedResult(
            DimensionDefinition dimension,
            ClarificationResult result
    ) {
        if (
                result.resolution()
                        == ClarificationResolution.RESOLVED
                        && !dimension.containsPole(
                        Objects.requireNonNull(
                                result.suggestedPole(),
                                "RESOLVED clarification result must have suggestedPole"
                        )
                )
        ) {
            throw new IllegalArgumentException(
                    "clarification suggested pole is not valid for dimension "
                            + dimension.code().value()
            );
        }
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
        List<DimensionClarification> result =
                new ArrayList<>(
                        clarifications.size()
                );

        boolean replaced =
                false;

        for (
                DimensionClarification clarification
                : clarifications
        ) {
            if (
                    clarification.id()
                            .equals(
                                    target.id()
                            )
            ) {
                result.add(
                        target
                );

                replaced =
                        true;
            } else {
                result.add(
                        clarification
                );
            }
        }

        if (!replaced) {
            throw new IllegalStateException(
                    "target clarification is missing from Session workflow"
            );
        }

        return List.copyOf(
                result
        );
    }
}
