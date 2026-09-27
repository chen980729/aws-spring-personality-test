package dev.springawsportfolio.portfolio.assessment.domain.repository;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.identity.api.UserId;

import java.util.Optional;

public interface AssessmentSessionRepository {

    Optional<AssessmentSession> findActive(
            UserId ownerUserId,
            AssessmentDefinitionId definitionId
    );

    Optional<AssessmentSession> findOwnedById(
            AssessmentSessionId sessionId,
            UserId ownerUserId
    );

    /**
     * Atomically attempts to create this Session as the active Session
     * for its owner + AssessmentDefinition.
     *
     * @return true if created; false if another active Session already exists.
     */
    boolean tryCreateActive(
            AssessmentSession session
    );

    /**
     * Persists the mutable state of an existing AssessmentSession.
     *
     * This operation does not create a new Session.
     */
    void update(
            AssessmentSession session
    );
}