package dev.springawsportfolio.portfolio.assessment.web.catalog;

import dev.springawsportfolio.portfolio.assessment.application.query.catalog.AssessmentCatalogItemResult;
import dev.springawsportfolio.portfolio.assessment.application.query.catalog.AssessmentDetailsResult;
import dev.springawsportfolio.portfolio.assessment.application.query.catalog.GetAssessmentDetailsQueryService;
import dev.springawsportfolio.portfolio.assessment.application.query.catalog.ListAssessmentsQueryService;
import dev.springawsportfolio.portfolio.assessment.web.dto.AssessmentCatalogResponse;
import dev.springawsportfolio.portfolio.assessment.web.dto.AssessmentDetailsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assessments")
public class AssessmentCatalogController {

    private final ListAssessmentsQueryService
            listAssessmentsQueryService;

    private final GetAssessmentDetailsQueryService
            getAssessmentDetailsQueryService;

    public AssessmentCatalogController(
            ListAssessmentsQueryService listAssessmentsQueryService,
            GetAssessmentDetailsQueryService getAssessmentDetailsQueryService
    ) {
        this.listAssessmentsQueryService =
                listAssessmentsQueryService;

        this.getAssessmentDetailsQueryService =
                getAssessmentDetailsQueryService;
    }

    @GetMapping
    public AssessmentCatalogResponse listAvailableAssessments() {
        List<AssessmentCatalogItemResult> results =
                listAssessmentsQueryService.execute();

        List<AssessmentCatalogResponse.AssessmentSummaryResponse>
                items =
                results.stream()
                        .map(result ->
                                new AssessmentCatalogResponse
                                        .AssessmentSummaryResponse(
                                        result.code(),
                                        result.name(),
                                        result.availableVersion(),
                                        result.questionCount()
                                )
                        )
                        .toList();

        return new AssessmentCatalogResponse(
                items
        );
    }

    @GetMapping("/{assessmentCode}")
    public AssessmentDetailsResponse getAssessmentDetails(
            @PathVariable("assessmentCode")
            String assessmentCode
    ) {
        AssessmentDetailsResult result =
                getAssessmentDetailsQueryService.execute(
                        assessmentCode
                );

        List<AssessmentDetailsResponse.PublicQuestionResponse>
                questions =
                result.questions()
                        .stream()
                        .map(question ->
                                new AssessmentDetailsResponse
                                        .PublicQuestionResponse(
                                        question.questionId(),
                                        question.position(),
                                        question.prompt()
                                )
                        )
                        .toList();

        List<AssessmentDetailsResponse.AnswerScaleOptionResponse>
                answerScale =
                result.answerScale()
                        .stream()
                        .map(option ->
                                new AssessmentDetailsResponse
                                        .AnswerScaleOptionResponse(
                                        option.value(),
                                        option.label()
                                )
                        )
                        .toList();

        AssessmentDetailsResponse.PublicQuestionnaireResponse
                questionnaire =
                new AssessmentDetailsResponse
                        .PublicQuestionnaireResponse(
                        questions,
                        answerScale
                );

        return new AssessmentDetailsResponse(
                result.code(),
                result.name(),
                result.versionCode(),
                result.questionCount(),
                questionnaire
        );
    }
}