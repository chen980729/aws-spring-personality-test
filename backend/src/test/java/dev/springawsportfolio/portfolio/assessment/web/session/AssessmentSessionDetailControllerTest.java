package dev.springawsportfolio.portfolio.assessment.web.session;

import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.query.session.GetAssessmentSessionService;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalAssessmentResult;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDecisionSource;
import dev.springawsportfolio.portfolio.assessment.domain.result.FinalDimensionConclusion;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        AssessmentSessionDetailController.class
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
class AssessmentSessionDetailControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GetAssessmentSessionService
            getAssessmentSessionService;

    @MockitoBean
    AuthenticationManager authenticationManager;

    @MockitoBean
    SecurityContextRepository securityContextRepository;

    @MockitoBean
    SessionAuthenticationStrategy
            sessionAuthenticationStrategy;

    @Test
    void returnsCompletedAssessmentSession()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        given(
                getAssessmentSessionService.execute(
                        any(),
                        any()
                )
        )
                .willReturn(
                        completedResult(
                                sessionId
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
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
                        jsonPath("$.status")
                                .value("COMPLETED")
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire.submitted"
                        )
                                .value(true)
                )
                .andExpect(
                        jsonPath(
                                "$.initialResult.dimensions[0].rawScore"
                        )
                                .value(3)
                )
                .andExpect(
                        jsonPath("$.finalResult.finalType")
                                .value("X")
                )
                .andExpect(
                        jsonPath(
                                "$.finalResult.dimensions[0].questionnairePreference"
                        )
                                .value("X")
                )
                .andExpect(
                        jsonPath(
                                "$.finalResult.dimensions[0].finalPreference"
                        )
                                .value("X")
                )
                .andExpect(
                        jsonPath(
                                "$.finalResult.dimensions[0].source"
                        )
                                .value("QUESTIONNAIRE")
                )
                .andExpect(
                        jsonPath(
                                "$.finalResult.dimensions[0].overrodeBaseline"
                        )
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.workflow.completed")
                                .value(true)
                );
    }

    @Test
    void returns404WhenSessionIsNotOwnedByCurrentUser()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        given(
                getAssessmentSessionService.execute(
                        any(),
                        any()
                )
        )
                .willThrow(
                        new AssessmentSessionNotFoundException()
                );

        mockMvc.perform(
                        get(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
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
                        jsonPath("$.code")
                                .value(
                                        "ASSESSMENT_SESSION_NOT_FOUND"
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
                        )
                )
                .andExpect(
                        status().isUnauthorized()
                );

        verifyNoInteractions(
                getAssessmentSessionService
        );
    }

    private AssessmentSessionResult completedResult(
            UUID sessionId
    ) {
        DimensionCode dimension =
                new DimensionCode("XY");

        PoleCode poleX =
                new PoleCode("X");

        InitialAssessmentResult initialResult =
                new InitialAssessmentResult(
                        List.of(
                                new InitialDimensionResult(
                                        dimension,
                                        3,
                                        poleX,
                                        false
                                )
                        )
                );

        FinalAssessmentResult finalResult =
                new FinalAssessmentResult(
                        "X",
                        List.of(
                                new FinalDimensionConclusion(
                                        dimension,
                                        poleX,
                                        FinalDecisionSource.QUESTIONNAIRE
                                )
                        )
                );

        Instant submittedAt =
                Instant.parse(
                        "2026-09-27T00:01:00Z"
                );

        return new AssessmentSessionResult(
                sessionId,
                "TEST_ASSESSMENT",
                "1.0",
                AssessmentSessionStatus.COMPLETED,

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
                                75.0,
                                "Y",
                                25.0
                        )
                ),

                List.of(),

                List.of(),

                finalResult,

                new AssessmentSessionResult
                        .WorkflowResult(
                        List.of(),
                        List.of(),
                        List.of(),
                        true
                ),

                Instant.parse(
                        "2026-09-27T00:00:00Z"
                ),

                submittedAt,

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
