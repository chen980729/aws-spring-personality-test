package dev.springawsportfolio.portfolio.assessment.domain.repository;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;

import java.util.Optional;

public interface AssessmentDefinitionVersionRepository {

    Optional<AssessmentDefinitionVersion> findById(
            AssessmentDefinitionVersionId id
    );

    Optional<AssessmentDefinitionVersion> findAvailableByDefinitionId(
            AssessmentDefinitionId definitionId
    );
}
