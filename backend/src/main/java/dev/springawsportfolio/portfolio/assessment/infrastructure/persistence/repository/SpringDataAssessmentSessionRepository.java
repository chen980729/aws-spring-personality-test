package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.repository;

import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.entity.AssessmentSessionJpaEntity;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataAssessmentSessionRepository
        extends Repository<AssessmentSessionJpaEntity, UUID> {

    Optional<AssessmentSessionJpaEntity>
    findByUserIdAndDefinitionIdAndStatusIn(
            UUID userId,
            UUID definitionId,
            Collection<String> statuses
    );

    Optional<AssessmentSessionJpaEntity>
    findByIdAndUserId(
            UUID id,
            UUID userId
    );
}