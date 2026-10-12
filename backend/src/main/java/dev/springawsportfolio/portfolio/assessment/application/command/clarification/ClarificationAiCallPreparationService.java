package dev.springawsportfolio.portfolio.assessment.application.command.clarification;

import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeContext;
import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeMessage;
import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimePolicy;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.ClarificationNotAllowedException;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationRuntimeContextStore;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionClarificationRepository;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
public class ClarificationAiCallPreparationService {

    private final AssessmentSessionRepository
            sessionRepository;

    private final AssessmentDefinitionVersionRepository
            versionRepository;

    private final DimensionClarificationRepository
            clarificationRepository;

    private final ClarificationRuntimeContextStore
            runtimeContextStore;

    private final ClarificationAiRequestAssembler
            requestAssembler;

    private final ClarificationRuntimePolicy
            runtimePolicy;

    private final Clock clock;

    public ClarificationAiCallPreparationService(
            AssessmentSessionRepository sessionRepository,
            AssessmentDefinitionVersionRepository versionRepository,
            DimensionClarificationRepository clarificationRepository,
            ClarificationRuntimeContextStore runtimeContextStore,
            ClarificationAiRequestAssembler requestAssembler,
            ClarificationRuntimePolicy runtimePolicy,
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

        this.runtimeContextStore =
                Objects.requireNonNull(
                        runtimeContextStore,
                        "runtimeContextStore must not be null"
                );

        this.requestAssembler =
                Objects.requireNonNull(
                        requestAssembler,
                        "requestAssembler must not be null"
                );

        this.runtimePolicy =
                Objects.requireNonNull(
                        runtimePolicy,
                        "runtimePolicy must not be null"
                );

        this.clock =
                Objects.requireNonNull(
                        clock,
                        "clock must not be null"
                );
    }

    @Transactional
    public ClarificationAiCallPreparation prepareInitial(
            ClarificationExecutionTicket ticket
    ) {
        Objects.requireNonNull(
                ticket,
                "ticket must not be null"
        );

        AssessmentSession session =
                sessionRepository
                        .findOwnedByIdForUpdate(
                                ticket.sessionId(),
                                ticket.actorUserId()
                        )
                        .orElse(null);

        if (
                session == null
                        || session.status()
                        != AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS
        ) {
            return ClarificationAiCallPreparation.stale(
                    ticket
            );
        }

        DimensionClarification clarification =
                clarificationRepository
                        .findBySessionIdAndDimensionForUpdate(
                                ticket.sessionId(),
                                ticket.dimension()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "active clarification session is missing target clarification"
                                        )
                        );

        if (
                clarification.status()
                        != DimensionClarificationStatus.IN_PROGRESS
                        || !clarification.matchesActiveExecution(
                        ticket.executionToken()
                )
        ) {
            return ClarificationAiCallPreparation.stale(
                    ticket
            );
        }

        ClarificationRuntimeContext context =
                runtimeContextStore.loadActive(
                        ticket.sessionId(),
                        ticket.dimension(),
                        ticket.executionToken()
                );

        if (!context.isEmpty()) {
            return ClarificationAiCallPreparation.stateLost(
                    ticket
            );
        }

        AssessmentDefinitionVersion version =
                loadBoundVersion(
                        session
                );

        return ClarificationAiCallPreparation.ready(
                ticket,
                requestAssembler.assemble(
                        session,
                        version,
                        ticket.dimension(),
                        context,
                        runtimePolicy.maximumModelTurns()
                ),
                0
        );
    }

    @Transactional
    public ClarificationAiCallPreparation prepareContinue(
            ContinueDimensionClarificationCommand command
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
                        != AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS
        ) {
            throw new ClarificationNotAllowedException(
                    "assessment session is not actively clarifying a dimension"
            );
        }

        DimensionClarification clarification =
                clarificationRepository
                        .findBySessionIdAndDimensionForUpdate(
                                command.sessionId(),
                                command.dimension()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "active clarification session is missing target clarification"
                                        )
                        );

        if (
                clarification.status()
                        != DimensionClarificationStatus.IN_PROGRESS
                        || clarification.activeExecutionToken() == null
        ) {
            throw new ClarificationNotAllowedException(
                    "clarification is not in an active execution"
            );
        }

        ClarificationExecutionTicket ticket =
                new ClarificationExecutionTicket(
                        command.actorUserId(),
                        command.sessionId(),
                        command.dimension(),
                        clarification.activeExecutionToken()
                );

        ClarificationRuntimeContext context =
                runtimeContextStore.loadActive(
                        command.sessionId(),
                        command.dimension(),
                        ticket.executionToken()
                );

        if (context.isEmpty()) {
            return ClarificationAiCallPreparation.stateLost(
                    ticket
            );
        }

        Instant now =
                clock.instant();

        if (
                !context
                        .messages()
                        .getFirst()
                        .expiresAt()
                        .isAfter(
                                now
                        )
        ) {
            return ClarificationAiCallPreparation.stateLost(
                    ticket
            );
        }

        ClarificationRuntimeMessage latest =
                context
                        .messages()
                        .getLast();

        if (
                latest.role()
                        != ClarificationRuntimeMessage.Role.ASSISTANT_QUESTION
        ) {
            throw new ClarificationNotAllowedException(
                    "clarification is not awaiting a user answer"
            );
        }

        int maximumFollowUpQuestionSequence =
                2 * runtimePolicy.maximumModelTurns() - 3;

        if (
                latest.sequenceNumber()
                        > maximumFollowUpQuestionSequence
        ) {
            return ClarificationAiCallPreparation.stateLost(
                    ticket
            );
        }

        Instant expiresAt =
                now.plus(
                        runtimePolicy.contextTtl()
                );

        ClarificationRuntimeMessage answer =
                new ClarificationRuntimeMessage(
                        latest.sequenceNumber() + 1,
                        ClarificationRuntimeMessage.Role.USER_ANSWER,
                        command.answer(),
                        now,
                        expiresAt
                );

        boolean appended =
                runtimeContextStore
                        .appendIfLatestSequenceMatches(
                                command.sessionId(),
                                command.dimension(),
                                ticket.executionToken(),
                                latest.sequenceNumber(),
                                answer
                        );

        if (!appended) {
            return ClarificationAiCallPreparation.stateLost(
                    ticket
            );
        }

        ClarificationRuntimeContext updatedContext =
                runtimeContextStore.loadActive(
                        command.sessionId(),
                        command.dimension(),
                        ticket.executionToken()
                );

        if (
                updatedContext.latestSequence()
                        != answer.sequenceNumber()
        ) {
            return ClarificationAiCallPreparation.stateLost(
                    ticket
            );
        }

        AssessmentDefinitionVersion version =
                loadBoundVersion(
                        session
                );

        return ClarificationAiCallPreparation.ready(
                ticket,
                requestAssembler.assemble(
                        session,
                        version,
                        command.dimension(),
                        updatedContext,
                        runtimePolicy.maximumModelTurns()
                ),
                updatedContext.latestSequence()
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
                                                "bound assessment definition version does not exist: "
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
}
