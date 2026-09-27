package dev.springawsportfolio.portfolio.assessment.application.port.out;

import dev.springawsportfolio.portfolio.assessment.application.query.history.AssessmentHistoryPage;
import dev.springawsportfolio.portfolio.identity.api.UserId;

public interface AssessmentHistoryQuery {

    AssessmentHistoryPage findCompletedByOwner(
            UserId ownerUserId,
            int page,
            int size
    );
}
