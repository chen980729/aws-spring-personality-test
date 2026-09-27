package dev.springawsportfolio.portfolio.assessment.domain.repository;

import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;

import java.util.List;

public interface DimensionClarificationRepository {

    void addAll(
            List<DimensionClarification> clarifications
    );
}
