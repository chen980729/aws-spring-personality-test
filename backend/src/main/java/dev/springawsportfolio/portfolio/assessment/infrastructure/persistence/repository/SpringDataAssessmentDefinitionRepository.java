package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.repository;

import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.entity.AssessmentDefinitionJpaEntity;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataAssessmentDefinitionRepository
        extends Repository<AssessmentDefinitionJpaEntity, UUID> {

    List<AssessmentDefinitionJpaEntity> findAll();

    Optional<AssessmentDefinitionJpaEntity> findById(
            UUID id
    );

    Optional<AssessmentDefinitionJpaEntity> findByCode(
            String code
    );
}
