package dev.springawsportfolio.portfolio.assessment.application.query.session;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.port.out.AssessmentSessionWorkflowQuery;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionWorkflowSnapshot;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class GetAssessmentSessionService {

    private final AssessmentSessionRepository
            sessionRepository;

    private final AssessmentDefinitionRepository
            definitionRepository;

    private final AssessmentDefinitionVersionRepository
            versionRepository;

    private final AssessmentSessionWorkflowQuery
            workflowQuery;

    public GetAssessmentSessionService(
            AssessmentSessionRepository sessionRepository,
            AssessmentDefinitionRepository definitionRepository,
            AssessmentDefinitionVersionRepository versionRepository,
            AssessmentSessionWorkflowQuery workflowQuery
    ) {
        this.sessionRepository =
                sessionRepository;

        this.definitionRepository =
                definitionRepository;

        this.versionRepository =
                versionRepository;

        this.workflowQuery =
                workflowQuery;
    }

    @Transactional(readOnly = true)
    public AssessmentSessionResult execute(
            UserId actorUserId,
            AssessmentSessionId sessionId
    ) {
        Objects.requireNonNull(
                actorUserId,
                "actorUserId must not be null"
        );

        Objects.requireNonNull(
                sessionId,
                "sessionId must not be null"
        );

        AssessmentSession session =
                sessionRepository
                        .findOwnedById(
                                sessionId,
                                actorUserId
                        )
                        .orElseThrow(
                                AssessmentSessionNotFoundException::new
                        );

        AssessmentDefinition definition =
                definitionRepository
                        .findById(
                                session.definitionId()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "bound assessment definition "
                                                        + "does not exist: "
                                                        + session
                                                        .definitionId()
                                                        .value()
                                        )
                        );

        AssessmentDefinitionVersion version =
                versionRepository
                        .findById(
                                session.definitionVersionId()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "bound assessment definition "
                                                        + "version does not exist: "
                                                        + session
                                                        .definitionVersionId()
                                                        .value()
                                        )
                        );

        if (
                !version.definitionId()
                        .equals(
                                session.definitionId()
                        )
        ) {
            throw new IllegalStateException(
                    "session definition/version binding "
                            + "is inconsistent"
            );
        }

        AssessmentSessionWorkflowSnapshot workflowSnapshot =
                workflowQuery.findBySessionId(
                        session.id()
                );

        return AssessmentSessionResult.from(
                definition,
                version,
                session,
                workflowSnapshot
        );
    }
}
