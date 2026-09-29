package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.repository;

import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.entity.AssessmentDimensionClarificationJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
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

    Optional<AssessmentDimensionClarificationJpaEntity>
    findById(
            UUID id
    );

    Optional<AssessmentDimensionClarificationJpaEntity>
    findBySessionIdAndDimensionCode(
            UUID sessionId,
            String dimensionCode
    );

    List<AssessmentDimensionClarificationJpaEntity>
    findBySessionIdOrderByDimensionCodeAsc(
            UUID sessionId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select clarification
            from AssessmentDimensionClarificationJpaEntity clarification
            where clarification.sessionId = :sessionId
              and clarification.dimensionCode = :dimensionCode
            """)
    Optional<AssessmentDimensionClarificationJpaEntity>
    findBySessionIdAndDimensionCodeForUpdate(
            @Param("sessionId")
            UUID sessionId,

            @Param("dimensionCode")
            String dimensionCode
    );
}
