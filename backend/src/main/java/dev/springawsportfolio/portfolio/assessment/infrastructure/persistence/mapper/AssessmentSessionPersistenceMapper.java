package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.mapper;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.entity.AssessmentSessionJpaEntity;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.springframework.stereotype.Component;

@Component
public final class AssessmentSessionPersistenceMapper {

    private final QuestionnaireResponsePersistenceMapper
            questionnaireResponseMapper;

    public AssessmentSessionPersistenceMapper(
            QuestionnaireResponsePersistenceMapper
                    questionnaireResponseMapper
    ) {
        this.questionnaireResponseMapper =
                questionnaireResponseMapper;
    }

    public AssessmentSession toDomain(
            AssessmentSessionJpaEntity entity
    ) {
        return AssessmentSession.restore(
                new AssessmentSessionId(
                        entity.getId()
                ),
                new UserId(
                        entity.getUserId()
                ),
                new AssessmentDefinitionId(
                        entity.getDefinitionId()
                ),
                new AssessmentDefinitionVersionId(
                        entity.getDefinitionVersionId()
                ),
                mapStatus(
                        entity.getStatus()
                ),
                questionnaireResponseMapper.toDomain(
                        entity.getQuestionnaireResponse()
                ),
                entity.getQuestionnaireSubmittedAt(),
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                entity.getAbandonedAt()
        );
    }

    private AssessmentSessionStatus mapStatus(
            String status
    ) {
        try {
            return AssessmentSessionStatus.valueOf(
                    status
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "unsupported assessment session status: "
                            + status,
                    exception
            );
        }
    }
}
