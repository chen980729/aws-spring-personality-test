package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.mapper;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionStatus;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.entity.AssessmentDefinitionVersionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public final class AssessmentDefinitionVersionPersistenceMapper {

    private final AssessmentSpecificationPersistenceMapper specificationMapper;

    public AssessmentDefinitionVersionPersistenceMapper(
            AssessmentSpecificationPersistenceMapper specificationMapper
    ) {
        this.specificationMapper = specificationMapper;
    }

    public AssessmentDefinitionVersion toDomain(
            AssessmentDefinitionVersionJpaEntity entity
    ) {
        return AssessmentDefinitionVersion.restore(
                new AssessmentDefinitionVersionId(
                        entity.getId()
                ),
                new AssessmentDefinitionId(
                        entity.getDefinitionId()
                ),
                entity.getVersionCode(),
                mapStatus(entity.getStatus()),
                specificationMapper.toDomain(
                        entity.getSpecification()
                ),
                entity.getCreatedAt(),
                entity.getPublishedAt()
        );
    }

    private AssessmentDefinitionVersionStatus mapStatus(
            String status
    ) {
        try {
            return AssessmentDefinitionVersionStatus.valueOf(
                    status
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "unsupported assessment definition version status: "
                            + status,
                    exception
            );
        }
    }
}
