package dev.springawsportfolio.portfolio.assessment.application.query.catalog;

import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinition;
import dev.springawsportfolio.portfolio.assessment.domain.definition.AssessmentDefinitionVersion;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionRepository;
import dev.springawsportfolio.portfolio.assessment.domain.repository.AssessmentDefinitionVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class ListAssessmentsQueryService {

    private final AssessmentDefinitionRepository definitionRepository;
    private final AssessmentDefinitionVersionRepository versionRepository;

    public ListAssessmentsQueryService(
            AssessmentDefinitionRepository definitionRepository,
            AssessmentDefinitionVersionRepository versionRepository
    ) {
        this.definitionRepository =
                definitionRepository;
        this.versionRepository =
                versionRepository;
    }

    @Transactional(readOnly = true)
    public List<AssessmentCatalogItemResult> execute() {
        return definitionRepository
                .findAll()
                .stream()
                .sorted(
                        Comparator.comparing(
                                AssessmentDefinition::code
                        )
                )
                .flatMap(definition ->
                        versionRepository
                                .findAvailableByDefinitionId(
                                        definition.id()
                                )
                                .map(version ->
                                        toResult(
                                                definition,
                                                version
                                        )
                                )
                                .stream()
                )
                .toList();
    }

    private AssessmentCatalogItemResult toResult(
            AssessmentDefinition definition,
            AssessmentDefinitionVersion version
    ) {
        int questionCount =
                version
                        .specification()
                        .questionnaire()
                        .questions()
                        .size();

        return new AssessmentCatalogItemResult(
                definition.code(),
                definition.name(),
                version.versionCode(),
                questionCount
        );
    }
}
