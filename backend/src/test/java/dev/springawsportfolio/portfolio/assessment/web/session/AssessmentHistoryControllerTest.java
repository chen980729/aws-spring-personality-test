package dev.springawsportfolio.portfolio.assessment.web.session;

import dev.springawsportfolio.portfolio.assessment.application.exception.InvalidAssessmentHistoryPageException;
import dev.springawsportfolio.portfolio.assessment.application.query.history.AssessmentHistoryItem;
import dev.springawsportfolio.portfolio.assessment.application.query.history.AssessmentHistoryPage;
import dev.springawsportfolio.portfolio.assessment.application.query.history.GetAssessmentHistoryService;
import dev.springawsportfolio.portfolio.assessment.web.error.AssessmentExceptionHandler;
import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication.AuthenticatedUserPrincipal;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.config.SecurityConfig;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiAccessDeniedHandler;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiAuthenticationEntryPoint;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiLogoutSuccessHandler;
import dev.springawsportfolio.portfolio.platform.web.error.GlobalWebExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        AssessmentHistoryController.class
)
@Import({
        GlobalWebExceptionHandler.class,
        AssessmentExceptionHandler.class,
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        ApiLogoutSuccessHandler.class
})
class AssessmentHistoryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GetAssessmentHistoryService
            getAssessmentHistoryService;

    @MockitoBean
    AuthenticationManager authenticationManager;

    @MockitoBean
    SecurityContextRepository securityContextRepository;

    @MockitoBean
    SessionAuthenticationStrategy
            sessionAuthenticationStrategy;

    @Test
    void returnsCompletedHistoryUsingDefaultPagination()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        UserId actorUserId =
                new UserId(
                        userId
                );

        given(
                getAssessmentHistoryService.execute(
                        eq(actorUserId),
                        eq(1),
                        eq(20)
                )
        )
                .willReturn(
                        new AssessmentHistoryPage(
                                List.of(
                                        new AssessmentHistoryItem(
                                                sessionId,
                                                "SIXTEEN_PERSONALITY",
                                                "1.0",
                                                "INFJ",
                                                Instant.parse(
                                                        "2026-09-27T12:00:00Z"
                                                )
                                        )
                                ),
                                1,
                                20,
                                1,
                                1
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/v1/assessment-sessions"
                        )
                                .param(
                                        "status",
                                        "COMPLETED"
                                )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.items[0].sessionId")
                                .value(
                                        sessionId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.items[0].assessmentCode")
                                .value(
                                        "SIXTEEN_PERSONALITY"
                                )
                )
                .andExpect(
                        jsonPath("$.items[0].assessmentVersion")
                                .value("1.0")
                )
                .andExpect(
                        jsonPath("$.items[0].finalType")
                                .value("INFJ")
                )
                .andExpect(
                        jsonPath("$.page")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.size")
                                .value(20)
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.totalPages")
                                .value(1)
                );
    }

    @Test
    void forwardsCustomPagination()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UserId actorUserId =
                new UserId(
                        userId
                );

        given(
                getAssessmentHistoryService.execute(
                        eq(actorUserId),
                        eq(2),
                        eq(5)
                )
        )
                .willReturn(
                        new AssessmentHistoryPage(
                                List.of(),
                                2,
                                5,
                                6,
                                2
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/v1/assessment-sessions"
                        )
                                .param(
                                        "status",
                                        "COMPLETED"
                                )
                                .param(
                                        "page",
                                        "2"
                                )
                                .param(
                                        "size",
                                        "5"
                                )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.items.length()")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.page")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.size")
                                .value(5)
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(6)
                )
                .andExpect(
                        jsonPath("$.totalPages")
                                .value(2)
                );
    }

    @Test
    void returns400ForUnsupportedHistoryStatus()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        mockMvc.perform(
                        get(
                                "/api/v1/assessment-sessions"
                        )
                                .param(
                                        "status",
                                        "ABANDONED"
                                )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "VALIDATION_FAILED"
                                )
                );

        verifyNoInteractions(
                getAssessmentHistoryService
        );
    }

    @Test
    void returns400ForInvalidPagination()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UserId actorUserId =
                new UserId(
                        userId
                );

        given(
                getAssessmentHistoryService.execute(
                        eq(actorUserId),
                        eq(0),
                        eq(20)
                )
        )
                .willThrow(
                        new InvalidAssessmentHistoryPageException(
                                0,
                                20
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/v1/assessment-sessions"
                        )
                                .param(
                                        "status",
                                        "COMPLETED"
                                )
                                .param(
                                        "page",
                                        "0"
                                )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "VALIDATION_FAILED"
                                )
                );
    }

    @Test
    void returns401WhenUserIsNotAuthenticated()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/assessment-sessions"
                        )
                                .param(
                                        "status",
                                        "COMPLETED"
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
                getAssessmentHistoryService
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
