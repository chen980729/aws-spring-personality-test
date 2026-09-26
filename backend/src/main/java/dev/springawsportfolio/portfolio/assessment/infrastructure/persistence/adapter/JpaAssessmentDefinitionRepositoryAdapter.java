package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.adapter;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.mapper.AssessmentDefinitionPersistenceMapper;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.repository.SpringDataAssessmentDefinitionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class JpaAssessmentDefinitionRepositoryAdapter
        implements AssessmentDefinitionRepository {

    private final SpringDataAssessmentDefinitionRepository repository;

    private final AssessmentDefinitionPersistenceMapper mapper;

    public JpaAssessmentDefinitionRepositoryAdapter(
            SpringDataAssessmentDefinitionRepository repository,
            AssessmentDefinitionPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<AssessmentDefinition> findAll() {
        return repository
                .findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<AssessmentDefinition> findById(
            AssessmentDefinitionId id
    ) {
        return repository
                .findById(
                        id.value()
                )
                .map(
                        mapper::toDomain
                );
    }

    @Override
    public Optional<AssessmentDefinition> findByCode(
            String code
    ) {
        return repository
                .findByCode(
                        code
                )
                .map(
                        mapper::toDomain
                );
    }
}
