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
     * Loads an owned Session while acquiring a database write lock.
     *
     * Used by cross-Session commands such as Restart where the old
     * Session must be revalidated under concurrency before it is
     * replaced.
     */
    Optional<AssessmentSession> findOwnedByIdForUpdate(
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
     */
    void update(
            AssessmentSession session
    );

    /**
     * Persists an already-abandoned old Session and creates its
     * replacement Session as one persistence operation inside the
     * caller-owned business transaction.
     *
     * The implementation must flush the old Session transition before
     * attempting replacement creation so PostgreSQL's partial UNIQUE
     * active-session index observes the old Session as non-active.
     */
    void replaceActive(
            AssessmentSession abandonedSession,
            AssessmentSession replacementSession
    );
}