package dev.springawsportfolio.portfolio.assessment.web.session;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.query.session.GetSessionQuestionnaireResult;
import dev.springawsportfolio.portfolio.assessment.application.query.session.GetSessionQuestionnaireService;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
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
import org.springframework.http.MediaType;
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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        AssessmentSessionQuestionnaireController.class
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
class AssessmentSessionQuestionnaireControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GetSessionQuestionnaireService
            getSessionQuestionnaireService;

    @MockitoBean
    AuthenticationManager authenticationManager;

    @MockitoBean
    SecurityContextRepository securityContextRepository;

    @MockitoBean
    SessionAuthenticationStrategy
            sessionAuthenticationStrategy;

    @BeforeEach
    void stubSecurityContextRepository() {
        given(securityContextRepository.loadDeferredContext(any()))
                .willAnswer(invocation -> emptyDeferredContext());
    }

    private DeferredSecurityContext emptyDeferredContext() {
        SecurityContext empty =
                SecurityContextHolder.createEmptyContext();

        return new DeferredSecurityContext() {
            @Override
            public SecurityContext get() {
                return empty;
            }

            @Override
            public boolean isGenerated() {
                return true; // 表示"这是新建的空上下文，不是从存储里读出来的"
            }
        };
    }

    @Test
    void returnsSessionBoundQuestionnaireWithoutScoringMetadata()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        given(
                getSessionQuestionnaireService.execute(
                        eq(new UserId(userId)),
                        eq(
                                new AssessmentSessionId(
                                        sessionId
                                )
                        )
                )
        )
                .willReturn(
                        questionnaireResult()
                );

        mockMvc.perform(
                        get(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/questionnaire"
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
                        jsonPath(
                                "$.questionnaire.questions.length()"
                        )
                                .value(2)
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire.questions[0].questionId"
                        )
                                .value("Q1")
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire.questions[0].position"
                        )
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire.questions[0].prompt"
                        )
                                .value("Question 1")
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire.answerScale.length()"
                        )
                                .value(5)
                )
                .andExpect(
                        jsonPath(
                                "$.response.answers.length()"
                        )
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.response.answers[0].questionId"
                        )
                                .value("Q1")
                )
                .andExpect(
                        jsonPath(
                                "$.response.answers[0].value"
                        )
                                .value(5)
                )
                .andExpect(
                        jsonPath("$.response.submitted")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.response.submittedAt")
                                .value(
                                        nullValue()
                                )
                )

                // Anti-gaming boundary
                .andExpect(
                        jsonPath(
                                "$.questionnaire.questions[0].dimension"
                        )
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire.questions[0].keyedPole"
                        )
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.scoringPolicy")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.ambiguityPolicy")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.clarificationPolicy")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.finalizationPolicy")
                                .doesNotExist()
                );
    }

    @Test
    void returns404WhenSessionIsNotAvailableToCurrentUser()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        given(
                getSessionQuestionnaireService.execute(
                        eq(new UserId(userId)),
                        eq(
                                new AssessmentSessionId(
                                        sessionId
                                )
                        )
                )
        )
                .willThrow(
                        new AssessmentSessionNotFoundException()
                );

        mockMvc.perform(
                        get(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/questionnaire"
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
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Assessment session not found"
                                )
                );
    }

    @Test
    void returns401WhenUserIsNotAuthenticated()
            throws Exception {

        UUID sessionId =
                UUID.randomUUID();

        mockMvc.perform(
                        get(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/questionnaire"
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
                getSessionQuestionnaireService
        );
    }

    private GetSessionQuestionnaireResult
    questionnaireResult() {

        return new GetSessionQuestionnaireResult(
                "SIXTEEN_PERSONALITY",
                "1.0",

                List.of(
                        new GetSessionQuestionnaireResult
                                .QuestionResult(
                                "Q1",
                                1,
                                "Question 1"
                        ),
                        new GetSessionQuestionnaireResult
                                .QuestionResult(
                                "Q2",
                                2,
                                "Question 2"
                        )
                ),

                List.of(
                        new GetSessionQuestionnaireResult
                                .AnswerOptionResult(
                                1,
                                "Strongly Disagree"
                        ),
                        new GetSessionQuestionnaireResult
                                .AnswerOptionResult(
                                2,
                                "Disagree"
                        ),
                        new GetSessionQuestionnaireResult
                                .AnswerOptionResult(
                                3,
                                "Neutral"
                        ),
                        new GetSessionQuestionnaireResult
                                .AnswerOptionResult(
                                4,
                                "Agree"
                        ),
                        new GetSessionQuestionnaireResult
                                .AnswerOptionResult(
                                5,
                                "Strongly Agree"
                        )
                ),

                List.of(
                        new GetSessionQuestionnaireResult
                                .QuestionAnswerResult(
                                "Q1",
                                5
                        )
                ),

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
