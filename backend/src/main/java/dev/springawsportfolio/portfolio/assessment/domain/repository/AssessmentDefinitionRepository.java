package dev.springawsportfolio.portfolio.assessment.domain.repository;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;

import java.util.List;
import java.util.Optional;

public interface AssessmentDefinitionRepository {

    List<AssessmentDefinition> findAll();

    Optional<AssessmentDefinition> findById(
            AssessmentDefinitionId id
    );

    Optional<AssessmentDefinition> findByCode(
            String code
    );
}
