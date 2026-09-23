package dev.springawsportfolio.portfolio.identity.web.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank
        String email,

        @NotNull
        @Size(min = 1, max = 128)
        String password
) {
}
