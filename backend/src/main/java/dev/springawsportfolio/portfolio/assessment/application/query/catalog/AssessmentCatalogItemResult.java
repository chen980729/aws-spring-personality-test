package dev.springawsportfolio.portfolio.assessment.application.query.catalog;

public record AssessmentCatalogItemResult(
        String code,
        String name,
        String availableVersion,
        int questionCount
) {
}