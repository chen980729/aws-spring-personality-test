package dev.springawsportfolio.portfolio.assessment.web.clarification;

import dev.springawsportfolio.portfolio.assessment.application.command.clarification.SkipDimensionClarificationCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.SkipDimensionClarificationService;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.SkipRemainingClarificationsCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.clarification.SkipRemainingClarificationsService;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.ClarificationNotAllowedException;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        AssessmentClarificationController.class
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
class AssessmentClarificationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SkipDimensionClarificationService
            skipDimensionClarificationService;

    @MockitoBean
    SkipRemainingClarificationsService
            skipRemainingClarificationsService;

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
                            SecurityContextHolder.createEmptyContext();

                    return new DeferredSecurityContext() {
                        @Override
                        public SecurityContext get() {
                            return empty;
                        }

                        @Override
                        public boolean isGenerated() {
                            return true;
                        }
                    };
                });
    }

    @Test
    void skipsOneClarificationAndReturnsCurrentSession()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        SkipDimensionClarificationCommand command =
                new SkipDimensionClarificationCommand(
                        new UserId(userId),
                        new AssessmentSessionId(sessionId),
                        new DimensionCode("EI")
                );

        given(
                skipDimensionClarificationService.execute(
                        eq(command)
                )
        )
                .willReturn(
                        sessionResult(
                                sessionId,
                                AssessmentSessionStatus.AWAITING_CLARIFICATION
                        )
                );

        mockMvc.perform(
                        post(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/clarifications/EI/skip"
                        )
                                .with(authenticatedUser(userId))
                                .with(csrf().asHeader())
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(sessionId.toString())
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("AWAITING_CLARIFICATION")
                );
    }

    @Test
    void skipsRemainingClarificationsAndReturnsCurrentSession()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        SkipRemainingClarificationsCommand command =
                new SkipRemainingClarificationsCommand(
                        new UserId(userId),
                        new AssessmentSessionId(sessionId)
                );

        given(
                skipRemainingClarificationsService.execute(
                        eq(command)
                )
        )
                .willReturn(
                        sessionResult(
                                sessionId,
                                AssessmentSessionStatus.COMPLETED
                        )
                );

        mockMvc.perform(
                        post(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/clarifications/skip-remaining"
                        )
                                .with(authenticatedUser(userId))
                                .with(csrf().asHeader())
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("COMPLETED")
                )
                .andExpect(
                        jsonPath("$.workflow.completed")
                                .value(true)
                );
    }

    @Test
    void returns404WhenSessionIsNotAvailableToCurrentUser()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        given(
                skipDimensionClarificationService.execute(
                        eq(
                                new SkipDimensionClarificationCommand(
                                        new UserId(userId),
                                        new AssessmentSessionId(sessionId),
                                        new DimensionCode("EI")
                                )
                        )
                )
        )
                .willThrow(
                        new AssessmentSessionNotFoundException()
                );

        mockMvc.perform(
                        post(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/clarifications/EI/skip"
                        )
                                .with(authenticatedUser(userId))
                                .with(csrf().asHeader())
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.code")
                                .value("ASSESSMENT_SESSION_NOT_FOUND")
                );
    }

    @Test
    void returns422WhenClarificationCannotBeSkipped()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        given(
                skipDimensionClarificationService.execute(any())
        )
                .willThrow(
                        new ClarificationNotAllowedException(
                                "clarification is not skippable"
                        )
                );

        mockMvc.perform(
                        post(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/clarifications/EI/skip"
                        )
                                .with(authenticatedUser(userId))
                                .with(csrf().asHeader())
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(
                        jsonPath("$.code")
                                .value("CLARIFICATION_NOT_ALLOWED")
                );
    }

    @Test
    void returns403WhenCsrfTokenIsMissing()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(
                        post(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/clarifications/EI/skip"
                        )
                                .with(authenticatedUser(userId))
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.code")
                                .value("CSRF_VALIDATION_FAILED")
                );

        verifyNoInteractions(
                skipDimensionClarificationService,
                skipRemainingClarificationsService
        );
    }

    @Test
    void returns401WhenUserIsNotAuthenticated()
            throws Exception {

        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(
                        post(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/clarifications/skip-remaining"
                        )
                                .with(csrf().asHeader())
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.code")
                                .value("AUTHENTICATION_REQUIRED")
                );

        verifyNoInteractions(
                skipDimensionClarificationService,
                skipRemainingClarificationsService
        );
    }

    private AssessmentSessionResult sessionResult(
            UUID sessionId,
            AssessmentSessionStatus status
    ) {
        Instant completedAt =
                status == AssessmentSessionStatus.COMPLETED
                        ? Instant.parse("2026-09-29T12:30:00Z")
                        : null;

        return new AssessmentSessionResult(
                sessionId,
                "SIXTEEN_PERSONALITY",
                "1.0",
                status,
                List.of(),
                Instant.parse("2026-09-29T12:00:00Z"),
                Instant.parse("2026-09-29T11:00:00Z"),
                completedAt,
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
