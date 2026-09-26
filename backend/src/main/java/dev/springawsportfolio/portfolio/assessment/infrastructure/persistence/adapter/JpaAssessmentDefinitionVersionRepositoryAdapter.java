package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.adapter;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionStatus;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.mapper.AssessmentDefinitionVersionPersistenceMapper;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.repository.SpringDataAssessmentDefinitionVersionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class JpaAssessmentDefinitionVersionRepositoryAdapter
        implements AssessmentDefinitionVersionRepository {

    private final SpringDataAssessmentDefinitionVersionRepository repository;
    private final AssessmentDefinitionVersionPersistenceMapper mapper;

    public JpaAssessmentDefinitionVersionRepositoryAdapter(
            SpringDataAssessmentDefinitionVersionRepository repository,
            AssessmentDefinitionVersionPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<AssessmentDefinitionVersion> findById(
            AssessmentDefinitionVersionId id
    ) {
        return repository
                .findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<AssessmentDefinitionVersion>
    findAvailableByDefinitionId(
            AssessmentDefinitionId definitionId
    ) {
        return repository
                .findByDefinitionIdAndStatus(
                        definitionId.value(),
                        AssessmentDefinitionVersionStatus
                                .AVAILABLE
                                .name()
                )
                .map(mapper::toDomain);
    }
}
