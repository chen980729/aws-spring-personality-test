package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.repository;

import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.entity.AssessmentDimensionClarificationJpaEntity;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface SpringDataAssessmentDimensionClarificationRepository
        extends Repository<
        AssessmentDimensionClarificationJpaEntity,
        UUID
        > {

    <S extends AssessmentDimensionClarificationJpaEntity>
    S save(
            S entity
    );
}
