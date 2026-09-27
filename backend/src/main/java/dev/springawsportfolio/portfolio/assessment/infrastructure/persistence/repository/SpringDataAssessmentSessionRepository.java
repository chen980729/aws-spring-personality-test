package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.repository;

import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.entity.AssessmentSessionJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session
            from AssessmentSessionJpaEntity session
            where session.id = :id
              and session.userId = :userId
            """)
    Optional<AssessmentSessionJpaEntity>
    findOwnedByIdForUpdate(
            @Param("id")
            UUID id,

            @Param("userId")
            UUID userId
    );
}