package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.adapter;

import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionClarificationRepository;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.entity.AssessmentDimensionClarificationJpaEntity;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.repository.SpringDataAssessmentDimensionClarificationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class JpaDimensionClarificationRepositoryAdapter
        implements DimensionClarificationRepository {

    private final SpringDataAssessmentDimensionClarificationRepository
            repository;

    public JpaDimensionClarificationRepositoryAdapter(
            SpringDataAssessmentDimensionClarificationRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public void addAll(
            List<DimensionClarification> clarifications
    ) {
        Objects.requireNonNull(
                clarifications,
                "clarifications must not be null"
        );

        for (
                DimensionClarification clarification
                : clarifications
        ) {
            repository.save(
                    new AssessmentDimensionClarificationJpaEntity(
                            clarification
                    )
            );
        }
    }
}
