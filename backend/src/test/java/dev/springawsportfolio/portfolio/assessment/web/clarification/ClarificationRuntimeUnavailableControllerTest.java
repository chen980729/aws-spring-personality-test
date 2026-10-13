package dev.springawsportfolio.portfolio.assessment.web.clarification;

import dev.springawsportfolio.portfolio.assessment.application.query.clarification.GetCurrentClarificationTurnService;
import dev.springawsportfolio.portfolio.assessment.application.query.session.GetAssessmentSessionService;
import dev.springawsportfolio.portfolio.assessment.web.error.AssessmentExceptionHandler;
import dev.springawsportfolio.portfolio.assessment.web.session.AssessmentSessionWebMapper;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Intentionally does not register ClarificationRuntimeOrchestrator.
 * The production default BEDROCK_CLARIFICATION_ENABLED=false must not
 * make the application fail startup or turn provider calls into 500s.
 */
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
class ClarificationRuntimeUnavailableControllerTest {

    @Autowired MockMvc mockMvc;

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
    void returns503ForStartWhenProviderCapabilityIsDisabled() throws Exception {
        UUID session = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/assessment-sessions/" + session
                        + "/clarifications/EI/start")
                        .with(authenticatedUser(UUID.randomUUID()))
                        .with(csrf().asHeader()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_CLARIFICATION_UNAVAILABLE"));
    }

    @Test
    void returns503ForContinueWhenProviderCapabilityIsDisabled() throws Exception {
        UUID session = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/assessment-sessions/" + session
                        + "/clarifications/EI/messages")
                        .with(authenticatedUser(UUID.randomUUID()))
                        .with(csrf().asHeader())
                        .contentType("application/json")
                        .content("{\"assistantSequence\":1,\"answer\":\"Example\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_CLARIFICATION_UNAVAILABLE"));
    }

    @Test
    void rejectsUnauthenticatedRequestsBeforeProviderCheck() throws Exception {
        UUID session = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/assessment-sessions/" + session
                        + "/clarifications/EI/retry")
                        .with(csrf().asHeader()))
                .andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor authenticatedUser(UUID userId) {
        Authentication authenticated = UsernamePasswordAuthenticationToken
                .authenticated(
                        new AuthenticatedUserPrincipal(userId, "user@example.com"),
                        null,
                        AuthorityUtils.NO_AUTHORITIES
                );
        return authentication(authenticated);
    }
}
