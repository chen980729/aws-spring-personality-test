package dev.springawsportfolio.portfolio.assessment.domain.repository;

import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;

import java.util.List;
import java.util.Optional;

public interface DimensionClarificationRepository {

    void addAll(
            List<DimensionClarification> clarifications
    );

    Optional<DimensionClarification> findBySessionIdAndDimension(
            AssessmentSessionId sessionId,
            DimensionCode dimension
    );

    /**
     * Loads one logical Session + Dimension clarification while acquiring
     * a database write lock.
     *
     * Clarification commands use this together with the Session lock so
     * the current clarification state is revalidated under concurrency.
     */
    Optional<DimensionClarification> findBySessionIdAndDimensionForUpdate(
            AssessmentSessionId sessionId,
            DimensionCode dimension
    );

    List<DimensionClarification> findBySessionId(
            AssessmentSessionId sessionId
    );

    void update(
            DimensionClarification clarification
    );
}
