package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.mapper;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersionId;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import dev.springawsportfolio.portfolio.assessment.domain.session.questionnaire.QuestionnaireResponse;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.entity.AssessmentSessionJpaEntity;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.springframework.stereotype.Component;

@Component
public final class AssessmentSessionPersistenceMapper {

    private final QuestionnaireResponsePersistenceMapper
            questionnaireResponseMapper;

    private final AssessmentResultPersistenceMapper
            resultMapper;

    public AssessmentSessionPersistenceMapper(
            QuestionnaireResponsePersistenceMapper questionnaireResponseMapper,
            AssessmentResultPersistenceMapper resultMapper
    ) {
        this.questionnaireResponseMapper =
                questionnaireResponseMapper;

        this.resultMapper =
                resultMapper;
    }

    public AssessmentSession toDomain(
            AssessmentSessionJpaEntity entity
    ) {
        QuestionnaireResponse questionnaireResponse =
                questionnaireResponseMapper.toDomain(
                        entity.getQuestionnaireResponse()
                );

        InitialAssessmentResult initialResult =
                resultMapper.toInitialDomain(
                        entity.getInitialResult()
                );

        FinalAssessmentResult finalResult =
                resultMapper.toFinalDomain(
                        entity.getFinalResult()
                );

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
                AssessmentSessionStatus.valueOf(
                        entity.getStatus()
                ),
                questionnaireResponse,
                entity.getQuestionnaireSubmittedAt(),
                initialResult,
                finalResult,
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                entity.getAbandonedAt()
        );
    }
}
