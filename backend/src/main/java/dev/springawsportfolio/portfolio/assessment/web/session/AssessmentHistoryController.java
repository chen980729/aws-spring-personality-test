package dev.springawsportfolio.portfolio.assessment.web.session;

import dev.springawsportfolio.portfolio.assessment.web.error.InvalidAssessmentHistoryStatusException;
import dev.springawsportfolio.portfolio.assessment.application.query.history.AssessmentHistoryPage;
import dev.springawsportfolio.portfolio.assessment.application.query.history.GetAssessmentHistoryService;
import dev.springawsportfolio.portfolio.assessment.web.session.dto.AssessmentHistoryResponse;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication.AuthenticatedUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assessment-sessions")
public class AssessmentHistoryController {

    private static final String COMPLETED_STATUS =
            "COMPLETED";

    private final GetAssessmentHistoryService
            getAssessmentHistoryService;

    public AssessmentHistoryController(
            GetAssessmentHistoryService getAssessmentHistoryService
    ) {
        this.getAssessmentHistoryService =
                getAssessmentHistoryService;
    }

    @GetMapping
    public AssessmentHistoryResponse getAssessmentHistory(
            @RequestParam(
                    defaultValue = COMPLETED_STATUS
            )
            String status,

            @RequestParam(
                    defaultValue = "1"
            )
            int page,

            @RequestParam(
                    defaultValue = "20"
            )
            int size,

            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        if (!COMPLETED_STATUS.equals(status)) {
            throw new InvalidAssessmentHistoryStatusException(
                    status
            );
        }

        AssessmentHistoryPage result =
                getAssessmentHistoryService.execute(
                        new UserId(
                                principal.userId()
                        ),
                        page,
                        size
                );

        return new AssessmentHistoryResponse(
                result
                        .items()
                        .stream()
                        .map(item ->
                                new AssessmentHistoryResponse
                                        .AssessmentHistoryItemResponse(
                                        item.sessionId(),
                                        item.assessmentCode(),
                                        item.assessmentVersion(),
                                        item.finalType(),
                                        item.completedAt()
                                )
                        )
                        .toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
