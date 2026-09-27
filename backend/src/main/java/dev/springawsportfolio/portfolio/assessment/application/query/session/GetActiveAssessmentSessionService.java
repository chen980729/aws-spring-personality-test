package dev.springawsportfolio.portfolio.assessment.application.query.session;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentNotFoundException;
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
import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class GetActiveAssessmentSessionService {

    private final AssessmentDefinitionRepository
            definitionRepository;

    private final AssessmentDefinitionVersionRepository
            versionRepository;

    private final AssessmentSessionRepository
            sessionRepository;

    private final AssessmentSessionWorkflowQuery
            workflowQuery;

    public GetActiveAssessmentSessionService(
            AssessmentDefinitionRepository definitionRepository,
            AssessmentDefinitionVersionRepository versionRepository,
            AssessmentSessionRepository sessionRepository,
            AssessmentSessionWorkflowQuery workflowQuery
    ) {
        this.definitionRepository =
                definitionRepository;

        this.versionRepository =
                versionRepository;

        this.sessionRepository =
                sessionRepository;

        this.workflowQuery =
                workflowQuery;
    }

    @Transactional(readOnly = true)
    public AssessmentSessionResult execute(
            UserId actorUserId,
            String assessmentCode
    ) {
        Objects.requireNonNull(
                actorUserId,
                "actorUserId must not be null"
        );

        Objects.requireNonNull(
                assessmentCode,
                "assessmentCode must not be null"
        );

        if (assessmentCode.isBlank()) {
            throw new IllegalArgumentException(
                    "assessmentCode must not be blank"
            );
        }

        AssessmentDefinition definition =
                definitionRepository
                        .findByCode(
                                assessmentCode
                        )
                        .orElseThrow(
                                () ->
                                        new AssessmentNotFoundException(
                                                assessmentCode
                                        )
                        );

        AssessmentSession session =
                sessionRepository
                        .findActive(
                                actorUserId,
                                definition.id()
                        )
                        .orElseThrow(
                                AssessmentSessionNotFoundException::new
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
