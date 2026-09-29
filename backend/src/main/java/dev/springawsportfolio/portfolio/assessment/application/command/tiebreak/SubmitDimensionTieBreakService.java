package dev.springawsportfolio.portfolio.assessment.application.command.tiebreak;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.InvalidDimensionTieBreakException;
import dev.springawsportfolio.portfolio.assessment.application.exception.TieBreakNotRequiredException;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionWorkflowSnapshot;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.ClarificationResolution;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarification;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentSessionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionClarificationRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.DimensionTieBreakRepository;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import dev.springawsportfolio.portfolio.assessment.domain.service.AssessmentFinalizationService;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSession;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import dev.springawsportfolio.portfolio.assessment.domain.tiebreak.DimensionTieBreak;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class SubmitDimensionTieBreakService {

    private final AssessmentSessionRepository
            sessionRepository;

    private final AssessmentDefinitionRepository
            definitionRepository;

    private final AssessmentDefinitionVersionRepository
            versionRepository;

    private final DimensionClarificationRepository
            clarificationRepository;

    private final DimensionTieBreakRepository
            tieBreakRepository;

    private final Clock clock;

    private final AssessmentFinalizationService
            finalizationService;

    public SubmitDimensionTieBreakService(
            AssessmentSessionRepository sessionRepository,
            AssessmentDefinitionRepository definitionRepository,
            AssessmentDefinitionVersionRepository versionRepository,
            DimensionClarificationRepository clarificationRepository,
            DimensionTieBreakRepository tieBreakRepository,
            Clock clock
    ) {
        this.sessionRepository =
                Objects.requireNonNull(
                        sessionRepository,
                        "sessionRepository must not be null"
                );

        this.definitionRepository =
                Objects.requireNonNull(
                        definitionRepository,
                        "definitionRepository must not be null"
                );

        this.versionRepository =
                Objects.requireNonNull(
                        versionRepository,
                        "versionRepository must not be null"
                );

        this.clarificationRepository =
                Objects.requireNonNull(
                        clarificationRepository,
                        "clarificationRepository must not be null"
                );

        this.tieBreakRepository =
                Objects.requireNonNull(
                        tieBreakRepository,
                        "tieBreakRepository must not be null"
                );

        this.clock =
                Objects.requireNonNull(
                        clock,
                        "clock must not be null"
                );

        this.finalizationService =
                new AssessmentFinalizationService();
    }

    @Transactional
    public AssessmentSessionResult execute(
            SubmitDimensionTieBreakCommand command
    ) {
        Objects.requireNonNull(
                command,
                "command must not be null"
        );

        AssessmentSession session =
                sessionRepository
                        .findOwnedByIdForUpdate(
                                command.sessionId(),
                                command.actorUserId()
                        )
                        .orElseThrow(
                                AssessmentSessionNotFoundException::new
                        );

        AssessmentDefinitionVersion version =
                loadBoundVersion(
                        session
                );

        DimensionDefinition dimension =
                findDimension(
                        version,
                        command
                                .dimension()
                                .value()
                );

        if (
                !dimension.containsPole(
                        command.selectedPole()
                )
        ) {
            throw new InvalidDimensionTieBreakException(
                    "selected pole is not valid for dimension "
                            + dimension.code().value()
                            + ": "
                            + command.selectedPole().value()
            );
        }

        InitialDimensionResult initialDimension =
                findInitialDimension(
                        session,
                        command
                                .dimension()
                                .value()
                );

        if (
                !initialDimension.ambiguous()
                        || !initialDimension.exactTie()
        ) {
            throw new TieBreakNotRequiredException(
                    "tie-break is only allowed for an ambiguous exact tie"
            );
        }

        Optional<DimensionTieBreak> existingTieBreak =
                tieBreakRepository
                        .findBySessionIdAndDimension(
                                session.id(),
                                command.dimension()
                        );

        if (
                session.status()
                        == AssessmentSessionStatus.COMPLETED
        ) {
            return recoverCompletedRetry(
                    command,
                    session,
                    version,
                    existingTieBreak
            );
        }

        if (
                session.status()
                        != AssessmentSessionStatus.AWAITING_CLARIFICATION
                        && session.status()
                        != AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS
        ) {
            throw new TieBreakNotRequiredException(
                    "assessment session is not in a state that accepts tie-breaks"
            );
        }

        DimensionClarification clarification =
                clarificationRepository
                        .findBySessionIdAndDimensionForUpdate(
                                session.id(),
                                command.dimension()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "exact-tie ambiguous dimension is missing "
                                                        + "its clarification lifecycle: "
                                                        + command
                                                        .dimension()
                                                        .value()
                                        )
                        );

        if (!requiresTieBreak(clarification)) {
            throw new TieBreakNotRequiredException(
                    "clarification has not left this exact tie unresolved"
            );
        }

        DimensionTieBreak acceptedTieBreak;

        if (existingTieBreak.isPresent()) {
            acceptedTieBreak =
                    existingTieBreak.orElseThrow();

            if (
                    !acceptedTieBreak
                            .selectedPole()
                            .equals(
                                    command.selectedPole()
                            )
            ) {
                throw new TieBreakNotRequiredException(
                        "tie-break has already been decided for this dimension"
                );
            }
        } else {
            acceptedTieBreak =
                    new DimensionTieBreak(
                            session.id(),
                            command.dimension(),
                            command.selectedPole(),
                            clock.instant()
                    );

            tieBreakRepository.add(
                    acceptedTieBreak
            );
        }

        List<DimensionClarification> clarifications =
                clarificationRepository.findBySessionId(
                        session.id()
                );

        List<DimensionTieBreak> tieBreaks =
                tieBreakRepository.findBySessionId(
                        session.id()
                );

        Instant now =
                clock.instant();

        Optional<FinalAssessmentResult> finalResult =
                finalizationService.finalizeIfReady(
                        version.specification().dimensions(),
                        Objects.requireNonNull(
                                session.initialResult(),
                                "post-submission session must have initialResult"
                        ),
                        clarifications,
                        tieBreaks
                );

        if (finalResult.isPresent()) {
            session.completeAfterClarification(
                    finalResult.orElseThrow(),
                    now
            );

            sessionRepository.update(
                    session
            );
        }

        AssessmentDefinition definition =
                loadDefinition(
                        session
                );

        return AssessmentSessionResult.from(
                definition,
                version,
                session,
                AssessmentSessionWorkflowSnapshot.fromDomain(
                        clarifications,
                        tieBreaks
                )
        );
    }

    private AssessmentSessionResult recoverCompletedRetry(
            SubmitDimensionTieBreakCommand command,
            AssessmentSession session,
            AssessmentDefinitionVersion version,
            Optional<DimensionTieBreak> existingTieBreak
    ) {
        DimensionTieBreak persisted =
                existingTieBreak
                        .orElseThrow(
                                () ->
                                        new TieBreakNotRequiredException(
                                                "completed assessment does not contain "
                                                        + "this tie-break fact"
                                        )
                        );

        if (
                !persisted
                        .selectedPole()
                        .equals(
                                command.selectedPole()
                        )
        ) {
            throw new TieBreakNotRequiredException(
                    "completed assessment tie-break cannot be changed"
            );
        }

        List<DimensionClarification> clarifications =
                clarificationRepository.findBySessionId(
                        session.id()
                );

        List<DimensionTieBreak> tieBreaks =
                tieBreakRepository.findBySessionId(
                        session.id()
                );

        return AssessmentSessionResult.from(
                loadDefinition(
                        session
                ),
                version,
                session,
                AssessmentSessionWorkflowSnapshot.fromDomain(
                        clarifications,
                        tieBreaks
                )
        );
    }

    private boolean requiresTieBreak(
            DimensionClarification clarification
    ) {
        if (
                clarification.status()
                        == DimensionClarificationStatus.SKIPPED
        ) {
            return true;
        }

        return clarification.status()
                == DimensionClarificationStatus.CLARIFIED
                && clarification.result() != null
                && clarification
                .result()
                .resolution()
                == ClarificationResolution.UNCLEAR;
    }

    private AssessmentDefinitionVersion loadBoundVersion(
            AssessmentSession session
    ) {
        AssessmentDefinitionVersion version =
                versionRepository
                        .findById(
                                session.definitionVersionId()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "bound assessment definition version "
                                                        + "does not exist: "
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
                    "session definition/version binding is inconsistent"
            );
        }

        return version;
    }

    private AssessmentDefinition loadDefinition(
            AssessmentSession session
    ) {
        return definitionRepository
                .findById(
                        session.definitionId()
                )
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "bound assessment definition does not exist: "
                                                + session
                                                .definitionId()
                                                .value()
                                )
                );
    }

    private DimensionDefinition findDimension(
            AssessmentDefinitionVersion version,
            String dimensionCode
    ) {
        return version
                .specification()
                .dimensions()
                .stream()
                .filter(dimension ->
                        dimension
                                .code()
                                .value()
                                .equals(
                                        dimensionCode
                                )
                )
                .findFirst()
                .orElseThrow(
                        () ->
                                new TieBreakNotRequiredException(
                                        "dimension does not exist in the bound "
                                                + "assessment version: "
                                                + dimensionCode
                                )
                );
    }

    private InitialDimensionResult findInitialDimension(
            AssessmentSession session,
            String dimensionCode
    ) {
        return Objects.requireNonNull(
                        session.initialResult(),
                        "tie-break requires an initial assessment result"
                )
                .dimensions()
                .stream()
                .filter(result ->
                        result
                                .dimension()
                                .value()
                                .equals(
                                        dimensionCode
                                )
                )
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "initial result is missing dimension: "
                                                + dimensionCode
                                )
                );
    }
}
