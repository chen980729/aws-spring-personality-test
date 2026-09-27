package dev.springawsportfolio.portfolio.assessment.web.session;

import dev.springawsportfolio.portfolio.assessment.application.command.restart.RestartAssessmentSessionCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.restart.RestartAssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.application.command.restart.RestartAssessmentSessionService;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionAlreadyAbandonedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionAlreadyCompletedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import dev.springawsportfolio.portfolio.assessment.web.error.AssessmentExceptionHandler;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication.AuthenticatedUserPrincipal;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.config.SecurityConfig;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiAccessDeniedHandler;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiAuthenticationEntryPoint;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiLogoutSuccessHandler;
import dev.springawsportfolio.portfolio.platform.web.error.GlobalWebExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        AssessmentSessionRestartController.class
)
@Import({
        GlobalWebExceptionHandler.class,
        AssessmentExceptionHandler.class,
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        ApiLogoutSuccessHandler.class,
        AssessmentSessionWebMapper.class
})
class AssessmentSessionRestartControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RestartAssessmentSessionService
            restartAssessmentSessionService;

    @MockitoBean
    AuthenticationManager authenticationManager;

    @MockitoBean
    SecurityContextRepository securityContextRepository;

    @MockitoBean
    SessionAuthenticationStrategy
            sessionAuthenticationStrategy;

    @BeforeEach
    void setUpSecurityContextRepository() {
        given(securityContextRepository.loadDeferredContext(any()))
                .willAnswer(invocation -> {
                    SecurityContext empty =
                            SecurityContextHolder.createEmptyContext(); // 每个请求只建一次

                    return new DeferredSecurityContext() {
                        @Override
                        public SecurityContext get() {
                            return empty; // 同一个请求内始终返回同一个对象
                        }

                        @Override
                        public boolean isGenerated() {
                            return true;
                        }
                    };
                });
    }

    @Test
    void restartsAssessmentAndReturnsCreatedReplacement()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID oldSessionId =
                UUID.randomUUID();

        UUID newSessionId =
                UUID.randomUUID();

        RestartAssessmentSessionCommand command =
                new RestartAssessmentSessionCommand(
                        new UserId(
                                userId
                        ),
                        new AssessmentSessionId(
                                oldSessionId
                        )
                );

        given(
                restartAssessmentSessionService.execute(
                        eq(command)
                )
        )
                .willReturn(
                        new RestartAssessmentSessionResult(
                                oldSessionId,
                                replacementResult(
                                        newSessionId
                                )
                        )
                );

        mockMvc.perform(
                        post(
                                "/api/v1/assessment-sessions/"
                                        + oldSessionId
                                        + "/restart"
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                                .with(
                                        csrf().asHeader()
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        header().string(
                                "Location",
                                "/api/v1/assessment-sessions/"
                                        + newSessionId
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.abandonedSessionId"
                        )
                                .value(
                                        oldSessionId.toString()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.session.id"
                        )
                                .value(
                                        newSessionId.toString()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.session.status"
                        )
                                .value(
                                        "IN_PROGRESS"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.session.assessment.code"
                        )
                                .value(
                                        "SIXTEEN_PERSONALITY"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.session.assessment.version"
                        )
                                .value(
                                        "1.0"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.session.questionnaire.answers.length()"
                        )
                                .value(0)
                )
                .andExpect(
                        jsonPath(
                                "$.session.questionnaire.submitted"
                        )
                                .value(false)
                )
                .andExpect(
                        jsonPath(
                                "$.session.workflow.completed"
                        )
                                .value(false)
                );
    }

    @Test
    void returns404WhenOldSessionIsNotAvailableToCurrentUser()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        RestartAssessmentSessionCommand command =
                new RestartAssessmentSessionCommand(
                        new UserId(
                                userId
                        ),
                        new AssessmentSessionId(
                                sessionId
                        )
                );

        given(
                restartAssessmentSessionService.execute(
                        eq(command)
                )
        )
                .willThrow(
                        new AssessmentSessionNotFoundException()
                );

        mockMvc.perform(
                        post(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/restart"
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                                .with(
                                        csrf().asHeader()
                                )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "ASSESSMENT_SESSION_NOT_FOUND"
                                )
                );
    }

    @Test
    void returns409WhenOldSessionWasAlreadyAbandoned()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        RestartAssessmentSessionCommand command =
                new RestartAssessmentSessionCommand(
                        new UserId(
                                userId
                        ),
                        new AssessmentSessionId(
                                sessionId
                        )
                );

        given(
                restartAssessmentSessionService.execute(
                        eq(command)
                )
        )
                .willThrow(
                        new AssessmentSessionAlreadyAbandonedException()
                );

        mockMvc.perform(
                        post(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/restart"
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                                .with(
                                        csrf().asHeader()
                                )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "ASSESSMENT_SESSION_ALREADY_ABANDONED"
                                )
                );
    }

    @Test
    void returns409WhenSessionIsAlreadyCompleted()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        RestartAssessmentSessionCommand command =
                new RestartAssessmentSessionCommand(
                        new UserId(
                                userId
                        ),
                        new AssessmentSessionId(
                                sessionId
                        )
                );

        given(
                restartAssessmentSessionService.execute(
                        eq(command)
                )
        )
                .willThrow(
                        new AssessmentSessionAlreadyCompletedException()
                );

        mockMvc.perform(
                        post(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/restart"
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                                .with(
                                        csrf().asHeader()
                                )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "ASSESSMENT_SESSION_ALREADY_COMPLETED"
                                )
                );
    }

    @Test
    void returns403WhenCsrfTokenIsMissing()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        mockMvc.perform(
                        post(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/restart"
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "CSRF_VALIDATION_FAILED"
                                )
                );

        verifyNoInteractions(
                restartAssessmentSessionService
        );
    }

    @Test
    void returns401WhenUserIsNotAuthenticated()
            throws Exception {

        UUID sessionId =
                UUID.randomUUID();

        mockMvc.perform(
                        post(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/restart"
                        )
                                .with(
                                        csrf().asHeader()
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "AUTHENTICATION_REQUIRED"
                                )
                );

        verifyNoInteractions(
                restartAssessmentSessionService
        );
    }

    private AssessmentSessionResult replacementResult(
            UUID sessionId
    ) {
        return new AssessmentSessionResult(
                sessionId,
                "SIXTEEN_PERSONALITY",
                "1.0",
                AssessmentSessionStatus.IN_PROGRESS,
                List.of(),
                null,
                Instant.parse(
                        "2026-09-27T10:00:00Z"
                ),
                null,
                null
        );
    }

    private RequestPostProcessor authenticatedUser(
            UUID userId
    ) {
        AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(
                        userId,
                        "user@example.com"
                );

        Authentication authenticated =
                UsernamePasswordAuthenticationToken
                        .authenticated(
                                principal,
                                null,
                                AuthorityUtils.NO_AUTHORITIES
                        );

        return authentication(
                authenticated
        );
    }
}
