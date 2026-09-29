package dev.springawsportfolio.portfolio.assessment.web.clarification.dto;

import jakarta.validation.constraints.NotBlank;

public record TieBreakRequest(
        @NotBlank
        String selectedPole
) {
}
