package dev.springawsportfolio.portfolio.assessment.web.clarification;

import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationRuntimeOrchestrator;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ClarificationRuntimeTurnResult;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.ContinueDimensionClarificationCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.RetryDimensionClarificationCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.StartDimensionClarificationCommand;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.ClarificationTurnConflictException;
import dev.springawsportfolio.portfolio.assessment.application.query.clarification.CurrentClarificationTurnResult;
import dev.springawsportfolio.portfolio.assessment.application.query.clarification.GetCurrentClarificationTurnService;
import dev.springawsportfolio.portfolio.assessment.application.query.session.GetAssessmentSessionService;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import dev.springawsportfolio.portfolio.assessment.web.error.AssessmentExceptionHandler;
import dev.springawsportfolio.portfolio.assessment.web.session.AssessmentSessionWebMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClarificationRuntimeController.class)
@Import({
        GlobalWebExceptionHandler.class,
        AssessmentExceptionHandler.class,
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        ApiLogoutSuccessHandler.class,
        AssessmentSessionWebMapper.class
})
class ClarificationRuntimeControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ClarificationRuntimeOrchestrator orchestrator;
    @MockitoBean GetCurrentClarificationTurnService currentTurnService;
    @MockitoBean GetAssessmentSessionService getSessionService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean SecurityContextRepository securityContextRepository;
    @MockitoBean SessionAuthenticationStrategy sessionAuthenticationStrategy;

    @BeforeEach
    void setUpSecurityContextRepository() {
        given(securityContextRepository.loadDeferredContext(any()))
                .willAnswer(invocation -> {
                    SecurityContext empty = SecurityContextHolder.createEmptyContext();
                    return new DeferredSecurityContext() {
                        @Override public SecurityContext get() { return empty; }
                        @Override public boolean isGenerated() { return true; }
                    };
                });
    }

    @Test
    void startsAndReturnsOnlyTheCurrentQuestion() throws Exception {
        UUID user = UUID.randomUUID();
        UUID session = UUID.randomUUID();

        given(orchestrator.start(eq(
                new StartDimensionClarificationCommand(
                        new UserId(user), new AssessmentSessionId(session), new DimensionCode("EI")
                )
        ))).willReturn(ClarificationRuntimeTurnResult.followUp(1, "What helps you recharge?"));
        given(getSessionService.execute(new UserId(user), new AssessmentSessionId(session)))
                .willReturn(sessionResult(session, DimensionClarificationStatus.IN_PROGRESS));

        mockMvc.perform(post(path(session, "start"))
                        .with(authenticatedUser(user))
                        .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(session.toString()))
                .andExpect(jsonPath("$.clarificationStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assistantSequence").value(1))
                .andExpect(jsonPath("$.question").value("What helps you recharge?"))
                .andExpect(jsonPath("$.sessionStatus").value("CLARIFICATION_IN_PROGRESS"))
                .andExpect(jsonPath("$.executionToken").doesNotExist())
                .andExpect(jsonPath("$.messages").doesNotExist());
    }

    @Test
    void continuesUsingOptimisticAssistantSequence() throws Exception {
        UUID user = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        ContinueDimensionClarificationCommand command =
                new ContinueDimensionClarificationCommand(
                        new UserId(user), new AssessmentSessionId(session),
                        new DimensionCode("EI"), 1, "I recharge alone."
                );

        given(orchestrator.continueClarification(eq(command)))
                .willReturn(ClarificationRuntimeTurnResult.followUp(3, "What about weekends?"));
        given(getSessionService.execute(new UserId(user), new AssessmentSessionId(session)))
                .willReturn(sessionResult(session, DimensionClarificationStatus.IN_PROGRESS));

        mockMvc.perform(post(path(session, "messages"))
                        .with(authenticatedUser(user))
                        .with(csrf().asHeader())
                        .contentType("application/json")
                        .content("{\"assistantSequence\":1,\"answer\":\"I recharge alone.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assistantSequence").value(3))
                .andExpect(jsonPath("$.question").value("What about weekends?"));

        verify(orchestrator).continueClarification(command);
    }

    @Test
    void retryReturnsFailedRetryableWorkflowSnapshot() throws Exception {
        UUID user = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        AssessmentSessionResult state =
                sessionResult(session, DimensionClarificationStatus.FAILED_RETRYABLE);

        given(orchestrator.retry(eq(new RetryDimensionClarificationCommand(
                new UserId(user), new AssessmentSessionId(session), new DimensionCode("EI")
        )))).willReturn(ClarificationRuntimeTurnResult.failedRetryable(state));

        mockMvc.perform(post(path(session, "retry"))
                        .with(authenticatedUser(user))
                        .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clarificationStatus").value("FAILED_RETRYABLE"))
                .andExpect(jsonPath("$.assistantSequence").isEmpty())
                .andExpect(jsonPath("$.workflow.retryableClarificationDimensions[0]").value("EI"));
    }

    @Test
    void currentTurnIsReadOnlyAndDoesNotRequireCsrf() throws Exception {
        UUID user = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        given(currentTurnService.execute(
                new UserId(user), new AssessmentSessionId(session), new DimensionCode("EI")
        )).willReturn(new CurrentClarificationTurnResult(session, "EI", 3, "Question 2?"));

        mockMvc.perform(get(path(session, "current-turn"))
                        .with(authenticatedUser(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assistantSequence").value(3))
                .andExpect(jsonPath("$.question").value("Question 2?"))
                .andExpect(jsonPath("$.executionToken").doesNotExist())
                .andExpect(jsonPath("$.messages").doesNotExist());

        verifyNoInteractions(orchestrator);
    }

    @Test
    void exposes409ForStaleAssistantSequence() throws Exception {
        UUID user = UUID.randomUUID();
        UUID session = UUID.randomUUID();

        given(orchestrator.continueClarification(any()))
                .willThrow(new ClarificationTurnConflictException("stale turn"));

        mockMvc.perform(post(path(session, "messages"))
                        .with(authenticatedUser(user))
                        .with(csrf().asHeader())
                        .contentType("application/json")
                        .content("{\"assistantSequence\":1,\"answer\":\"Something\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLARIFICATION_TURN_CONFLICT"));
    }

    @Test
    void hidesUnknownOrForeignSessionBehind404() throws Exception {
        UUID user = UUID.randomUUID();
        UUID session = UUID.randomUUID();

        given(currentTurnService.execute(any(), any(), any()))
                .willThrow(new AssessmentSessionNotFoundException());

        mockMvc.perform(get(path(session, "current-turn"))
                        .with(authenticatedUser(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ASSESSMENT_SESSION_NOT_FOUND"));
    }

    @Test
    void rejectsInvalidAssistantSequenceAs400() throws Exception {
        UUID user = UUID.randomUUID();
        UUID session = UUID.randomUUID();

        mockMvc.perform(post(path(session, "messages"))
                        .with(authenticatedUser(user))
                        .with(csrf().asHeader())
                        .contentType("application/json")
                        .content("{\"assistantSequence\":2,\"answer\":\"Something\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(orchestrator);
    }

    @Test
    void requiresCsrfForProviderBackedPost() throws Exception {
        UUID user = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        mockMvc.perform(post(path(session, "start"))
                        .with(authenticatedUser(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_VALIDATION_FAILED"));

        verifyNoInteractions(orchestrator);
    }

    @Test
    void requiresAuthenticationForRecoveryRead() throws Exception {
        UUID session = UUID.randomUUID();
        mockMvc.perform(get(path(session, "current-turn")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        verifyNoInteractions(currentTurnService);
    }

    private String path(UUID session, String suffix) {
        return "/api/v1/assessment-sessions/" + session + "/clarifications/EI/" + suffix;
    }

    private AssessmentSessionResult sessionResult(
            UUID session,
            DimensionClarificationStatus clarificationStatus
    ) {
        Instant startedAt = Instant.parse("2026-10-01T00:00:00Z");
        return new AssessmentSessionResult(
                session,
                "SIXTEEN_PERSONALITY",
                "1.1",
                clarificationStatus == DimensionClarificationStatus.FAILED_RETRYABLE
                        ? AssessmentSessionStatus.AWAITING_CLARIFICATION
                        : AssessmentSessionStatus.CLARIFICATION_IN_PROGRESS,
                List.of(),
                startedAt,
                null,
                List.of(),
                List.of(new AssessmentSessionResult.ClarificationStateResult(
                        "EI", clarificationStatus, null, startedAt, null
                )),
                List.of(),
                null,
                new AssessmentSessionResult.WorkflowResult(
                        List.of(),
                        clarificationStatus == DimensionClarificationStatus.FAILED_RETRYABLE
                                ? List.of("EI") : List.of(),
                        List.of(),
                        false
                ),
                startedAt,
                null,
                null
        );
    }

    private RequestPostProcessor authenticatedUser(UUID userId) {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                userId, "user@example.com"
        );
        Authentication authenticated = UsernamePasswordAuthenticationToken
                .authenticated(principal, null, AuthorityUtils.NO_AUTHORITIES);
        return authentication(authenticated);
    }
}
