package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.mapper;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.entity.AssessmentDefinitionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public final class AssessmentDefinitionPersistenceMapper {

    public AssessmentDefinition toDomain(
            AssessmentDefinitionJpaEntity entity
    ) {
        return AssessmentDefinition.restore(
                new AssessmentDefinitionId(
                        entity.getId()
                ),
                entity.getCode(),
                entity.getName(),
                entity.getCreatedAt()
        );
    }
}
