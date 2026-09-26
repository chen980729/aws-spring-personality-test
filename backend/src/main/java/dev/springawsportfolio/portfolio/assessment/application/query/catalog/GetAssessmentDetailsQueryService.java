package dev.springawsportfolio.portfolio.assessment.application.query.catalog;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentNotFoundException;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.AnswerOption;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.QuestionDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class GetAssessmentDetailsQueryService {

    private final AssessmentDefinitionRepository definitionRepository;
    private final AssessmentDefinitionVersionRepository versionRepository;

    public GetAssessmentDetailsQueryService(
            AssessmentDefinitionRepository definitionRepository,
            AssessmentDefinitionVersionRepository versionRepository
    ) {
        this.definitionRepository =
                definitionRepository;
        this.versionRepository =
                versionRepository;
    }

    @Transactional(readOnly = true)
    public AssessmentDetailsResult execute(
            String assessmentCode
    ) {
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
                        .findByCode(assessmentCode)
                        .orElseThrow(
                                () ->
                                        new AssessmentNotFoundException(
                                                assessmentCode
                                        )
                        );

        AssessmentDefinitionVersion version =
                versionRepository
                        .findAvailableByDefinitionId(
                                definition.id()
                        )
                        .orElseThrow(
                                () ->
                                        new AssessmentNotFoundException(
                                                assessmentCode
                                        )
                        );

        List<AssessmentDetailsResult.AnswerOptionResult>
                answerScale =
                version
                        .specification()
                        .questionnaire()
                        .answerScale()
                        .stream()
                        .sorted(
                                Comparator.comparingInt(
                                        AnswerOption::value
                                )
                        )
                        .map(option ->
                                new AssessmentDetailsResult
                                        .AnswerOptionResult(
                                        option.value(),
                                        option.label()
                                )
                        )
                        .toList();

        List<AssessmentDetailsResult.QuestionResult>
                questions =
                version
                        .specification()
                        .questionnaire()
                        .questions()
                        .stream()
                        .sorted(
                                Comparator.comparingInt(
                                        QuestionDefinition::position
                                )
                        )
                        .map(question ->
                                new AssessmentDetailsResult
                                        .QuestionResult(
                                        question
                                                .questionId()
                                                .value(),
                                        question.position(),
                                        question.prompt()
                                )
                        )
                        .toList();

        return new AssessmentDetailsResult(
                definition.code(),
                definition.name(),
                version.versionCode(),
                answerScale,
                questions
        );
    }
}
