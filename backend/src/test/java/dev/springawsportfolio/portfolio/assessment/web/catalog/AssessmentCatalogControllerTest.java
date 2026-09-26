package dev.springawsportfolio.portfolio.assessment.web.catalog;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.query.catalog.AssessmentCatalogItemResult;
import dev.springawsportfolio.portfolio.assessment.application.query.catalog.AssessmentDetailsResult;
import dev.springawsportfolio.portfolio.assessment.application.query.catalog.GetAssessmentDetailsQueryService;
import dev.springawsportfolio.portfolio.assessment.application.query.catalog.ListAssessmentsQueryService;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication.AuthenticatedUserPrincipal;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.config.SecurityConfig;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiAccessDeniedHandler;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiAuthenticationEntryPoint;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiLogoutSuccessHandler;
import dev.springawsportfolio.portfolio.platform.web.error.GlobalWebExceptionHandler;
import dev.springawsportfolio.portfolio.assessment.web.error.AssessmentExceptionHandler;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(AssessmentCatalogController.class)
@Import({
        GlobalWebExceptionHandler.class,
        AssessmentExceptionHandler.class,
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        ApiLogoutSuccessHandler.class
})
class AssessmentCatalogControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ListAssessmentsQueryService listAssessmentsQueryService;

    @MockitoBean
    GetAssessmentDetailsQueryService
            getAssessmentDetailsQueryService;

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
                .willReturn(new DeferredSecurityContext() {
                    @Override
                    public SecurityContext get() {
                        return SecurityContextHolder.createEmptyContext();
                    }

                    @Override
                    public boolean isGenerated() {
                        return true;
                    }
                });
    }
    @Test
    void returnsAvailableAssessmentCatalog()
            throws Exception {

        given(
                listAssessmentsQueryService.execute()
        ).willReturn(
                List.of(
                        new AssessmentCatalogItemResult(
                                "SIXTEEN_PERSONALITY",
                                "Personality Type Explorer",
                                "1.0",
                                48
                        )
                )
        );

        mockMvc.perform(
                        get("/api/v1/assessments")
                                .with(authenticatedUser())
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.items.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.items[0].code")
                                .value(
                                        "SIXTEEN_PERSONALITY"
                                )
                )
                .andExpect(
                        jsonPath("$.items[0].name")
                                .value(
                                        "Personality Type Explorer"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.items[0].availableVersion"
                        )
                                .value("1.0")
                )
                .andExpect(
                        jsonPath(
                                "$.items[0].questionCount"
                        )
                                .value(48)
                );
    }

    @Test
    void returnsPublicAssessmentDetailsWithoutScoringMetadata()
            throws Exception {

        given(
                getAssessmentDetailsQueryService.execute(
                        "SIXTEEN_PERSONALITY"
                )
        ).willReturn(
                new AssessmentDetailsResult(
                        "SIXTEEN_PERSONALITY",
                        "Personality Type Explorer",
                        "1.0",
                        List.of(
                                new AssessmentDetailsResult
                                        .AnswerOptionResult(
                                        1,
                                        "Strongly Disagree"
                                ),
                                new AssessmentDetailsResult
                                        .AnswerOptionResult(
                                        5,
                                        "Strongly Agree"
                                )
                        ),
                        List.of(
                                new AssessmentDetailsResult
                                        .QuestionResult(
                                        "Q1",
                                        1,
                                        "Question 1"
                                ),
                                new AssessmentDetailsResult
                                        .QuestionResult(
                                        "Q2",
                                        2,
                                        "Question 2"
                                )
                        )
                )
        );

        mockMvc.perform(
                        get(
                                "/api/v1/assessments/"
                                        + "SIXTEEN_PERSONALITY"
                        )
                                .with(authenticatedUser())
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "SIXTEEN_PERSONALITY"
                                )
                )
                .andExpect(
                        jsonPath("$.name")
                                .value(
                                        "Personality Type Explorer"
                                )
                )
                .andExpect(
                        jsonPath("$.version")
                                .value("1.0")
                )
                .andExpect(
                        jsonPath("$.versionCode")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.questionCount")
                                .value(2)
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire"
                                        + ".questions.length()"
                        )
                                .value(2)
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire"
                                        + ".questions[0]"
                                        + ".questionId"
                        )
                                .value("Q1")
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire"
                                        + ".questions[0]"
                                        + ".position"
                        )
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire"
                                        + ".questions[0]"
                                        + ".prompt"
                        )
                                .value("Question 1")
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire"
                                        + ".answerScale.length()"
                        )
                                .value(2)
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire"
                                        + ".answerScale[0].value"
                        )
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire"
                                        + ".answerScale[0].label"
                        )
                                .value(
                                        "Strongly Disagree"
                                )
                )

                // Anti-gaming boundary
                .andExpect(
                        jsonPath(
                                "$.questionnaire"
                                        + ".questions[0]"
                                        + ".dimension"
                        )
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire"
                                        + ".questions[0]"
                                        + ".keyedPole"
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
    void returns404WhenAssessmentIsNotAvailable()
            throws Exception {

        given(
                getAssessmentDetailsQueryService.execute(
                        "UNKNOWN"
                )
        ).willThrow(
                new AssessmentNotFoundException(
                        "UNKNOWN"
                )
        );

        mockMvc.perform(
                        get(
                                "/api/v1/assessments/UNKNOWN"
                        )
                                .with(authenticatedUser())
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
                                        "ASSESSMENT_NOT_FOUND"
                                )
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Assessment not found"
                                )
                );
    }

    @Test
    void returns401WhenUserIsNotAuthenticated()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/assessments")
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "AUTHENTICATION_REQUIRED"
                                )
                );

        verifyNoInteractions(
                listAssessmentsQueryService,
                getAssessmentDetailsQueryService
        );
    }

    private RequestPostProcessor authenticatedUser() {
        AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(
                        UUID.randomUUID(),
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
