package dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.adapter;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionId;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.entity.AssessmentSessionJpaEntity;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.mapper.AssessmentResultPersistenceMapper;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.mapper.AssessmentSessionPersistenceMapper;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.mapper.QuestionnaireResponsePersistenceMapper;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.repository.PostgresAssessmentSessionAtomicCreator;
import dev.springawsportfolio.portfolio.assessment.infrastructure.persistence.repository.SpringDataAssessmentSessionRepository;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaAssessmentSessionRepositoryAdapter
        implements AssessmentSessionRepository {

    private static final List<String> ACTIVE_STATUSES =
            List.of(
                    AssessmentSessionStatus
                            .IN_PROGRESS
                            .name(),
                    AssessmentSessionStatus
                            .AWAITING_CLARIFICATION
                            .name(),
                    AssessmentSessionStatus
                            .CLARIFICATION_IN_PROGRESS
                            .name()
            );

    private final SpringDataAssessmentSessionRepository repository;

    private final PostgresAssessmentSessionAtomicCreator
            atomicCreator;

    private final AssessmentSessionPersistenceMapper mapper;

    private final QuestionnaireResponsePersistenceMapper
            questionnaireResponseMapper;

    private final AssessmentResultPersistenceMapper
            resultMapper;

    public JpaAssessmentSessionRepositoryAdapter(
            SpringDataAssessmentSessionRepository repository,
            PostgresAssessmentSessionAtomicCreator atomicCreator,
            AssessmentSessionPersistenceMapper mapper,
            QuestionnaireResponsePersistenceMapper questionnaireResponseMapper,
            AssessmentResultPersistenceMapper resultMapper
    ) {
        this.repository = repository;
        this.atomicCreator = atomicCreator;
        this.mapper = mapper;
        this.questionnaireResponseMapper =
                questionnaireResponseMapper;
        this.resultMapper =
                resultMapper;
    }

    @Override
    public Optional<AssessmentSession> findActive(
            UserId ownerUserId,
            AssessmentDefinitionId definitionId
    ) {
        return repository
                .findByUserIdAndDefinitionIdAndStatusIn(
                        ownerUserId.value(),
                        definitionId.value(),
                        ACTIVE_STATUSES
                )
                .map(mapper::toDomain);
    }

    @Override
    public Optional<AssessmentSession> findOwnedById(
            AssessmentSessionId sessionId,
            UserId ownerUserId
    ) {
        return repository
                .findByIdAndUserId(
                        sessionId.value(),
                        ownerUserId.value()
                )
                .map(mapper::toDomain);
    }

    @Override
    public boolean tryCreateActive(
            AssessmentSession session
    ) {
        if (!session.isActive()) {
            throw new IllegalArgumentException(
                    "only an active AssessmentSession "
                            + "can be created through tryCreateActive"
            );
        }

        return atomicCreator.tryCreate(
                session
        );
    }

    @Override
    public void update(
            AssessmentSession session
    ) {
        AssessmentSessionJpaEntity entity =
                repository
                        .findByIdAndUserId(
                                session.id().value(),
                                session.ownerUserId().value()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "AssessmentSession disappeared "
                                                        + "during update: "
                                                        + session
                                                        .id()
                                                        .value()
                                        )
                        );

        entity.updateFrom(
                session,

                questionnaireResponseMapper.toJsonNode(
                        session.questionnaireResponse()
                ),

                resultMapper.toInitialJsonNode(
                        session.initialResult()
                ),

                resultMapper.toFinalJsonNode(
                        session.finalResult()
                )
        );
    }
}
