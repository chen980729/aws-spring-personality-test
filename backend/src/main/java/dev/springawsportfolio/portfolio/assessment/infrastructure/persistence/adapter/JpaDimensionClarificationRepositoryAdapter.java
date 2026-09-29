package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.adapter;

import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionClarificationRepository;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.entity.AssessmentDimensionClarificationJpaEntity;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.mapper.DimensionClarificationPersistenceMapper;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.repository.SpringDataAssessmentDimensionClarificationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class JpaDimensionClarificationRepositoryAdapter
        implements DimensionClarificationRepository {

    private final SpringDataAssessmentDimensionClarificationRepository
            repository;

    private final DimensionClarificationPersistenceMapper
            mapper;

    public JpaDimensionClarificationRepositoryAdapter(
            SpringDataAssessmentDimensionClarificationRepository repository,
            DimensionClarificationPersistenceMapper mapper
    ) {
        this.repository =
                Objects.requireNonNull(
                        repository,
                        "repository must not be null"
                );

        this.mapper =
                Objects.requireNonNull(
                        mapper,
                        "mapper must not be null"
                );
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
                            Objects.requireNonNull(
                                    clarification,
                                    "clarification must not be null"
                            )
                    )
            );
        }
    }

    @Override
    public Optional<DimensionClarification>
    findBySessionIdAndDimension(
            AssessmentSessionId sessionId,
            DimensionCode dimension
    ) {
        Objects.requireNonNull(
                sessionId,
                "sessionId must not be null"
        );

        Objects.requireNonNull(
                dimension,
                "dimension must not be null"
        );

        return repository
                .findBySessionIdAndDimensionCode(
                        sessionId.value(),
                        dimension.value()
                )
                .map(
                        mapper::toDomain
                );
    }

    @Override
    public Optional<DimensionClarification>
    findBySessionIdAndDimensionForUpdate(
            AssessmentSessionId sessionId,
            DimensionCode dimension
    ) {
        Objects.requireNonNull(
                sessionId,
                "sessionId must not be null"
        );

        Objects.requireNonNull(
                dimension,
                "dimension must not be null"
        );

        return repository
                .findBySessionIdAndDimensionCodeForUpdate(
                        sessionId.value(),
                        dimension.value()
                )
                .map(
                        mapper::toDomain
                );
    }

    @Override
    public List<DimensionClarification> findBySessionId(
            AssessmentSessionId sessionId
    ) {
        Objects.requireNonNull(
                sessionId,
                "sessionId must not be null"
        );

        return repository
                .findBySessionIdOrderByDimensionCodeAsc(
                        sessionId.value()
                )
                .stream()
                .map(
                        mapper::toDomain
                )
                .toList();
    }

    @Override
    public void update(
            DimensionClarification clarification
    ) {
        Objects.requireNonNull(
                clarification,
                "clarification must not be null"
        );

        AssessmentDimensionClarificationJpaEntity entity =
                repository
                        .findById(
                                clarification.id().value()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "DimensionClarification disappeared "
                                                        + "during update: "
                                                        + clarification
                                                        .id()
                                                        .value()
                                        )
                        );

        entity.updateFrom(
                clarification
        );

        repository.save(
                entity
        );
    }
}
