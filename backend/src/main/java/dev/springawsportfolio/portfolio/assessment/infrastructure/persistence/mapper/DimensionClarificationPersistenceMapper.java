package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.mapper;

import dev.springawsportfolio.portfolio.assessment.domain.clarification.AIProvenance;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationConfidence;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationExecutionToken;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResolution;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResult;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationId;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.entity.AssessmentDimensionClarificationJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class DimensionClarificationPersistenceMapper {

    public DimensionClarification toDomain(
            AssessmentDimensionClarificationJpaEntity entity
    ) {
        Objects.requireNonNull(
                entity,
                "entity must not be null"
        );

        ClarificationResult result =
                toResult(
                        entity
                );

        AIProvenance aiProvenance =
                toAIProvenance(
                        entity
                );

        return DimensionClarification.restore(
                new DimensionClarificationId(
                        entity.getId()
                ),
                new AssessmentSessionId(
                        entity.getSessionId()
                ),
                new DimensionCode(
                        entity.getDimensionCode()
                ),
                DimensionClarificationStatus.valueOf(
                        entity.getStatus()
                ),
                entity.getActiveExecutionToken() == null
                        ? null
                        : new ClarificationExecutionToken(
                        entity.getActiveExecutionToken()
                ),
                result,
                aiProvenance,
                entity.getStartedAt(),
                entity.getAcceptedAt(),
                entity.getUpdatedAt()
        );
    }

    private ClarificationResult toResult(
            AssessmentDimensionClarificationJpaEntity entity
    ) {
        boolean noResultData =
                entity.getResultOutcome() == null
                        && entity.getSuggestedPole() == null
                        && entity.getResultConfidence() == null
                        && entity.getResultSummary() == null;

        if (noResultData) {
            return null;
        }

        String resultOutcome =
                Objects.requireNonNull(
                        entity.getResultOutcome(),
                        "persisted clarification resultOutcome must not be null"
                );

        String resultConfidence =
                Objects.requireNonNull(
                        entity.getResultConfidence(),
                        "persisted clarification resultConfidence must not be null"
                );

        PoleCode suggestedPole =
                entity.getSuggestedPole() == null
                        ? null
                        : new PoleCode(
                        entity.getSuggestedPole()
                );

        return new ClarificationResult(
                ClarificationResolution.valueOf(
                        resultOutcome
                ),
                suggestedPole,
                ClarificationConfidence.valueOf(
                        resultConfidence
                ),
                entity.getResultSummary()
        );
    }

    private AIProvenance toAIProvenance(
            AssessmentDimensionClarificationJpaEntity entity
    ) {
        boolean noProvenanceData =
                entity.getAiProvider() == null
                        && entity.getAiModelIdentifier() == null
                        && entity.getClarificationPolicyRevision() == null;

        if (noProvenanceData) {
            return null;
        }

        return new AIProvenance(
                Objects.requireNonNull(
                        entity.getAiProvider(),
                        "persisted aiProvider must not be null"
                ),
                Objects.requireNonNull(
                        entity.getAiModelIdentifier(),
                        "persisted aiModelIdentifier must not be null"
                ),
                Objects.requireNonNull(
                        entity.getClarificationPolicyRevision(),
                        "persisted clarificationPolicyRevision must not be null"
                )
        );
    }
}
