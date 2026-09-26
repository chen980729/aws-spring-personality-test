package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.repository;

import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.entity.AssessmentDefinitionVersionJpaEntity;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataAssessmentDefinitionVersionRepository
        extends Repository<AssessmentDefinitionVersionJpaEntity, UUID> {

    Optional<AssessmentDefinitionVersionJpaEntity> findById(
            UUID id
    );

    Optional<AssessmentDefinitionVersionJpaEntity>
    findByDefinitionIdAndStatus(
            UUID definitionId,
            String status
    );
}