package dev.springawsportfolio.portfolio.assessment.application.query.clarification;

import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeContext;
import dev.springawsportfolio.portfolio.assessment.application.clarification.runtime.ClarificationRuntimeMessage;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.ClarificationNotAllowedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.ClarificationTurnConflictException;
import dev.springawsportfolio.portfolio.assessment.application.port.out.ClarificationRuntimeContextStore;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionClarificationRepository;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Objects;

/**
 * Read-only, owner-scoped recovery query. Never calls the AI provider,
 * mutates the execution, or exposes the temporary transcript/token.
 */
@Service
public class GetCurrentClarificationTurnService {

    private final AssessmentSessionRepository sessionRepository;
    private final AssessmentDefinitionVersionRepository versionRepository;
    private final DimensionClarificationRepository clarificationRepository;
    private final ClarificationRuntimeContextStore contextStore;
    private final Clock clock;

    public GetCurrentClarificationTurnService(
            AssessmentSessionRepository sessionRepository,
            AssessmentDefinitionVersionRepository versionRepository,
            DimensionClarificationRepository clarificationRepository,
            ClarificationRuntimeContextStore contextStore,
            Clock clock
    ) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository);
        this.versionRepository = Objects.requireNonNull(versionRepository);
        this.clarificationRepository = Objects.requireNonNull(clarificationRepository);
        this.contextStore = Objects.requireNonNull(contextStore);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional(readOnly = true)
    public CurrentClarificationTurnResult execute(
            UserId actorUserId,
            AssessmentSessionId sessionId,
            DimensionCode dimension
    ) {
        Objects.requireNonNull(actorUserId);
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(dimension);

        // Ownership is checked before any lookup of dimension runtime data.
        AssessmentSession session = sessionRepository
                .findOwnedById(sessionId, actorUserId)
                .orElseThrow(AssessmentSessionNotFoundException::new);

        AssessmentDefinitionVersion version = versionRepository
                .findById(session.definitionVersionId())
                .orElseThrow(() -> new IllegalStateException(
                        "bound assessment definition version does not exist"
                ));

        boolean knownDimension = version.specification()
                .dimensions()
                .stream()
                .anyMatch(candidate -> candidate.code().equals(dimension));

        if (!knownDimension
                || session.status() != AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS) {
            throw new ClarificationNotAllowedException(
                    "no active clarification for the bound session/dimension"
            );
        }

        DimensionClarification clarification = clarificationRepository
                .findBySessionIdAndDimension(sessionId, dimension)
                .orElseThrow(() -> new ClarificationNotAllowedException(
                        "dimension has no clarification"
                ));

        if (clarification.status() != DimensionClarificationStatus.IN_PROGRESS
                || clarification.activeExecutionToken() == null) {
            throw new ClarificationNotAllowedException(
                    "dimension is not actively clarifying"
            );
        }

        ClarificationRuntimeContext context = contextStore.loadActive(
                sessionId,
                dimension,
                clarification.activeExecutionToken()
        );

        if (context.isEmpty()
                || !context.messages().getFirst().expiresAt().isAfter(clock.instant())) {
            throw new ClarificationTurnConflictException(
                    "no unexpired assistant question is available"
            );
        }

        ClarificationRuntimeMessage latest = context.messages().getLast();
        if (latest.role() != ClarificationRuntimeMessage.Role.ASSISTANT_QUESTION) {
            throw new ClarificationTurnConflictException(
                    "the previous answer is still processing"
            );
        }

        return new CurrentClarificationTurnResult(
                sessionId.value(),
                dimension.value(),
                latest.sequenceNumber(),
                latest.text()
        );
    }
}
