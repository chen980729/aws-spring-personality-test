package dev.springawsportfolio.portfolio.assessment.web.session;

import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireResult;
import dev.springawsportfolio.portfolio.assessment.application.command.submission.SubmitQuestionnaireService;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentAlreadySubmittedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.QuestionnaireIncompleteException;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.clarification.DimensionClarificationStatus;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.InitialDimensionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionStatus;
import dev.springawsportfolio.portfolio.assessment.web.error.AssessmentExceptionHandler;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        AssessmentSessionSubmissionController.class
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
class AssessmentSessionSubmissionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SubmitQuestionnaireService
            submitQuestionnaireService;

    @MockitoBean
    AuthenticationManager authenticationManager;

    @MockitoBean
    SecurityContextRepository securityContextRepository;

    @MockitoBean
    SessionAuthenticationStrategy
            sessionAuthenticationStrategy;

    @Test
    void submitsQuestionnaireAndReturnsAwaitingClarificationState()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        given(
                submitQuestionnaireService.execute(
                        any()
                )
        )
                .willReturn(
                        new SubmitQuestionnaireResult(
                                awaitingClarificationResult(
                                        sessionId
                                )
                        )
                );

        mockMvc.perform(
                        post(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/questionnaire/submission"
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                                .with(
                                        csrf().asHeader()
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "answers": [
                                            {
                                              "questionId": "Q1",
                                              "value": 5
                                            }
                                          ]
                                        }
                                        """
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
                        jsonPath("$.status")
                                .value(
                                        "AWAITING_CLARIFICATION"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire.submitted"
                        )
                                .value(true)
                )
                .andExpect(
                        jsonPath(
                                "$.initialResult.dimensions[0].dimensionCode"
                        )
                                .value("XY")
                )
                .andExpect(
                        jsonPath(
                                "$.initialResult.dimensions[0].rawScore"
                        )
                                .value(0)
                )
                .andExpect(
                        jsonPath(
                                "$.initialResult.dimensions[0].questionnairePreference"
                        )
                                .value(
                                        org.hamcrest.Matchers.nullValue()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.initialResult.dimensions[0].ambiguous"
                        )
                                .value(true)
                )
                .andExpect(
                        jsonPath(
                                "$.initialResult.dimensions[0].evidence.poleA"
                        )
                                .value("X")
                )
                .andExpect(
                        jsonPath(
                                "$.initialResult.dimensions[0].evidence.poleAPercentage"
                        )
                                .value(50.0)
                )
                .andExpect(
                        jsonPath(
                                "$.clarifications[0].dimensionCode"
                        )
                                .value("XY")
                )
                .andExpect(
                        jsonPath(
                                "$.clarifications[0].status"
                        )
                                .value("PENDING")
                )
                .andExpect(
                        jsonPath("$.finalResult")
                                .value(
                                        org.hamcrest.Matchers.nullValue()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.workflow.pendingClarificationDimensions[0]"
                        )
                                .value("XY")
                )
                .andExpect(
                        jsonPath("$.workflow.completed")
                                .value(false)
                );
    }

    @Test
    void returns422WhenQuestionnaireIsIncomplete()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        given(
                submitQuestionnaireService.execute(
                        any()
                )
        )
                .willThrow(
                        new QuestionnaireIncompleteException()
                );

        mockMvc.perform(
                        post(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/questionnaire/submission"
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                                .with(
                                        csrf().asHeader()
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "answers": []
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isUnprocessableContent()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "QUESTIONNAIRE_INCOMPLETE"
                                )
                );
    }

    @Test
    void returns409WhenQuestionnaireWasAlreadySubmitted()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        given(
                submitQuestionnaireService.execute(
                        any()
                )
        )
                .willThrow(
                        new AssessmentAlreadySubmittedException()
                );

        mockMvc.perform(
                        post(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/questionnaire/submission"
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                                .with(
                                        csrf().asHeader()
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "answers": [
                                            {
                                              "questionId": "Q1",
                                              "value": 5
                                            }
                                          ]
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "ASSESSMENT_ALREADY_SUBMITTED"
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
                                        + "/questionnaire/submission"
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "answers": []
                                        }
                                        """
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
                submitQuestionnaireService
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
                                        + "/questionnaire/submission"
                        )
                                .with(
                                        csrf().asHeader()
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "answers": []
                                        }
                                        """
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
                submitQuestionnaireService
        );
    }

    private AssessmentSessionResult
    awaitingClarificationResult(
            UUID sessionId
    ) {
        Instant submittedAt =
                Instant.parse(
                        "2026-09-27T00:01:00Z"
                );

        InitialAssessmentResult initialResult =
                new InitialAssessmentResult(
                        List.of(
                                new InitialDimensionResult(
                                        new DimensionCode("XY"),
                                        0,
                                        null,
                                        true
                                )
                        )
                );

        return new AssessmentSessionResult(
                sessionId,
                "TEST_ASSESSMENT",
                "1.0",
                AssessmentSessionStatus
                        .AWAITING_CLARIFICATION,

                List.of(
                        new AssessmentSessionResult
                                .QuestionAnswerResult(
                                "Q1",
                                5
                        )
                ),

                submittedAt,

                initialResult,

                List.of(
                        new AssessmentSessionResult
                                .DimensionEvidenceResult(
                                "XY",
                                "X",
                                50.0,
                                "Y",
                                50.0
                        )
                ),

                List.of(
                        new AssessmentSessionResult
                                .ClarificationStateResult(
                                "XY",
                                DimensionClarificationStatus.PENDING,
                                null,
                                null
                        )
                ),

                List.of(),

                null,

                new AssessmentSessionResult
                        .WorkflowResult(
                        List.of("XY"),
                        List.of(),
                        List.of(),
                        false
                ),

                Instant.parse(
                        "2026-09-27T00:00:00Z"
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
