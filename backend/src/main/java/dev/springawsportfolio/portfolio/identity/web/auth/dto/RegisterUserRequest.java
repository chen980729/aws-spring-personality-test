package dev.springawsportfolio.portfolio.identity.web.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(

        @NotBlank
        String email,

        @NotNull
        @Size(min = 15, max = 128)
        String password,

        @NotBlank
        @Size(max = 80)
        String displayName
) {
}