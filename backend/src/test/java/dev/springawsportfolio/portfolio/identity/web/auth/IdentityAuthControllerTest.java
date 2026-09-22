package dev.springawsportfolio.portfolio.identity.web.auth;

import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.application.command.register.RegisterUserResult;
import dev.springawsportfolio.portfolio.identity.application.command.register.RegisterUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IdentityAuthController.class)
@Import({
        dev.springawsportfolio.portfolio.platform.web.error.GlobalWebExceptionHandler.class,
        dev.springawsportfolio.portfolio.identity.web.error.IdentityExceptionHandler.class
})
class IdentityAuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RegisterUserService registerUserService;

    @Test
    void returns201WhenRegistrationSucceeds()
            throws Exception {

        UserId userId = UserId.newId();

        given(registerUserService.register(any()))
                .willReturn(
                        new RegisterUserResult(
                                userId,
                                "user@example.com",
                                "Chen",
                                Instant.parse(
                                        "2026-09-22T10:00:00Z"
                                )
                        )
                );

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "email": "user@example.com",
                                          "password": "a-secure-password",
                                          "displayName": "Chen"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        userId.value().toString()
                                )
                )
                .andExpect(
                        jsonPath("$.email")
                                .value("user@example.com")
                )
                .andExpect(
                        jsonPath("$.displayName")
                                .value("Chen")
                );
    }

    @Test
    void returns400WhenPasswordIsTooShort()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                      "email": "user@example.com",
                                      "password": "short",
                                      "displayName": "Chen"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("VALIDATION_FAILED")
                )
                .andExpect(
                        jsonPath("$.fieldErrors[0].field")
                                .value("password")
                );
    }
}
