package dev.springawsportfolio.portfolio.assessment.domain.repository;

import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.tiebreak.DimensionTieBreak;

import java.util.List;
import java.util.Optional;

public interface DimensionTieBreakRepository {

    Optional<DimensionTieBreak> findBySessionIdAndDimension(
            AssessmentSessionId sessionId,
            DimensionCode dimension
    );

    List<DimensionTieBreak> findBySessionId(
            AssessmentSessionId sessionId
    );

    void add(
            DimensionTieBreak tieBreak
    );
}
