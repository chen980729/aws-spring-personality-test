package dev.springawsportfolio.portfolio.assessment.web.session;

import dev.springawsportfolio.portfolio.assessment.application.command.start.StartAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.application.command.start.StartAssessmentService;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.query.session.GetActiveAssessmentSessionService;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
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
import org.springframework.http.MediaType;
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

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssessmentSessionController.class)
@Import({
        GlobalWebExceptionHandler.class,
        AssessmentExceptionHandler.class,
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        ApiLogoutSuccessHandler.class,
        AssessmentSessionWebMapper.class
})
class AssessmentSessionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    StartAssessmentService startAssessmentService;

    @MockitoBean
    GetActiveAssessmentSessionService
            getActiveAssessmentSessionService;

    @MockitoBean
    AuthenticationManager authenticationManager;

    @MockitoBean
    SecurityContextRepository securityContextRepository;

    @MockitoBean
    SessionAuthenticationStrategy
            sessionAuthenticationStrategy;

    @Test
    void returns201AndLocationWhenNewSessionIsCreated()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        given(
                startAssessmentService.execute(
                        eq(new UserId(userId)),
                        eq("SIXTEEN_PERSONALITY")
                )
        )
                .willReturn(
                        startResult(
                                true,
                                sessionId
                        )
                );

        mockMvc.perform(
                        post(
                                "/api/v1/assessments/"
                                        + "SIXTEEN_PERSONALITY"
                                        + "/sessions"
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
                                        + sessionId
                        )
                )
                .andExpect(
                        jsonPath("$.created")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.session.id")
                                .value(
                                        sessionId.toString()
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
                                .value("1.0")
                )
                .andExpect(
                        jsonPath("$.session.status")
                                .value("IN_PROGRESS")
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
                                "$.session.questionnaire.submittedAt"
                        )
                                .value(
                                        nullValue()
                                )
                )
                .andExpect(
                        jsonPath("$.session.initialResult")
                                .value(
                                        nullValue()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.session.clarifications.length()"
                        )
                                .value(0)
                )
                .andExpect(
                        jsonPath(
                                "$.session.tieBreaks.length()"
                        )
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.session.finalResult")
                                .value(
                                        nullValue()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.session.workflow.completed"
                        )
                                .value(false)
                );
    }

    @Test
    void returns200WhenActiveSessionIsResumed()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        given(
                startAssessmentService.execute(
                        eq(new UserId(userId)),
                        eq("SIXTEEN_PERSONALITY")
                )
        )
                .willReturn(
                        startResult(
                                false,
                                sessionId
                        )
                );

        mockMvc.perform(
                        post(
                                "/api/v1/assessments/"
                                        + "SIXTEEN_PERSONALITY"
                                        + "/sessions"
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
                        status().isOk()
                )
                .andExpect(
                        header().doesNotExist(
                                "Location"
                        )
                )
                .andExpect(
                        jsonPath("$.created")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.session.id")
                                .value(
                                        sessionId.toString()
                                )
                );
    }

    @Test
    void returnsActiveAssessmentSession()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        given(
                getActiveAssessmentSessionService.execute(
                        eq(new UserId(userId)),
                        eq("SIXTEEN_PERSONALITY")
                )
        )
                .willReturn(
                        sessionResult(
                                sessionId,
                                List.of(
                                        new AssessmentSessionResult
                                                .QuestionAnswerResult(
                                                "Q1",
                                                5
                                        )
                                )
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/v1/assessments/"
                                        + "SIXTEEN_PERSONALITY"
                                        + "/sessions/active"
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
                        jsonPath("$.id")
                                .value(
                                        sessionId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.assessment.code")
                                .value(
                                        "SIXTEEN_PERSONALITY"
                                )
                )
                .andExpect(
                        jsonPath("$.assessment.version")
                                .value("1.0")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("IN_PROGRESS")
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire.answers.length()"
                        )
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire.answers[0].questionId"
                        )
                                .value("Q1")
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire.answers[0].value"
                        )
                                .value(5)
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire.submitted"
                        )
                                .value(false)
                );
    }

    @Test
    void returns404WhenNoActiveAssessmentSessionExists()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        given(
                getActiveAssessmentSessionService.execute(
                        eq(new UserId(userId)),
                        eq("SIXTEEN_PERSONALITY")
                )
        )
                .willThrow(
                        new AssessmentSessionNotFoundException()
                );

        mockMvc.perform(
                        get(
                                "/api/v1/assessments/"
                                        + "SIXTEEN_PERSONALITY"
                                        + "/sessions/active"
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "ASSESSMENT_SESSION_NOT_FOUND"
                                )
                );
    }

    @Test
    void returns403WhenCsrfTokenIsMissing()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        mockMvc.perform(
                        post(
                                "/api/v1/assessments/"
                                        + "SIXTEEN_PERSONALITY"
                                        + "/sessions"
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
                startAssessmentService
        );
    }

    @Test
    void returns401WhenStartUserIsNotAuthenticated()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/assessments/"
                                        + "SIXTEEN_PERSONALITY"
                                        + "/sessions"
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
                startAssessmentService
        );
    }

    @Test
    void returns401WhenActiveSessionUserIsNotAuthenticated()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/assessments/"
                                        + "SIXTEEN_PERSONALITY"
                                        + "/sessions/active"
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
                getActiveAssessmentSessionService
        );
    }

    private StartAssessmentResult startResult(
            boolean created,
            UUID sessionId
    ) {
        return new StartAssessmentResult(
                created,
                sessionResult(
                        sessionId,
                        List.of()
                )
        );
    }

    private AssessmentSessionResult sessionResult(
            UUID sessionId,
            List<AssessmentSessionResult.QuestionAnswerResult> answers
    ) {
        return new AssessmentSessionResult(
                sessionId,
                "SIXTEEN_PERSONALITY",
                "1.0",
                AssessmentSessionStatus.IN_PROGRESS,
                answers,
                null,
                Instant.parse(
                        "2026-09-26T00:00:00Z"
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
