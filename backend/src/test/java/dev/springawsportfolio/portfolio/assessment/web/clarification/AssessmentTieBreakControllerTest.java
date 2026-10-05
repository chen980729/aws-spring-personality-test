package dev.springawsportfolio.portfolio.assessment.web.clarification;

import dev.springawsportfolio.portfolio.assessment.application.command.tiebreak.SubmitDimensionTieBreakCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.tiebreak.SubmitDimensionTieBreakService;
import dev.springawsportfolio.portfolio.assessment.application.command.tiebreak.TieBreakSelection;
import dev.springawsportfolio.portfolio.assessment.application.query.tiebreak.DimensionTieBreakInteractionResult;
import dev.springawsportfolio.portfolio.assessment.application.query.tiebreak.GetDimensionTieBreakInteractionService;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.InvalidDimensionTieBreakException;
import dev.springawsportfolio.portfolio.assessment.application.exception.TieBreakNotRequiredException;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.DimensionCode;
import dev.springawsportfolio.portfolio.assessment.domain.definition.specification.PoleCode;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        AssessmentTieBreakController.class
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
class AssessmentTieBreakControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GetDimensionTieBreakInteractionService
            getDimensionTieBreakInteractionService;

    @MockitoBean
    SubmitDimensionTieBreakService
            submitDimensionTieBreakService;

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
    void returnsLegacyDirectPoleInteractionWithoutCsrf()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        given(
                getDimensionTieBreakInteractionService.execute(
                        any(),
                        any(),
                        any()
                )
        )
                .willReturn(
                        new DimensionTieBreakInteractionResult
                                .DirectPoleSelection(
                                "EI",
                                List.of(
                                        "E",
                                        "I"
                                )
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/tie-breaks/EI"
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.interactionType")
                                .value(
                                        "DIRECT_POLE_SELECTION"
                                )
                )
                .andExpect(
                        jsonPath("$.dimensionCode")
                                .value("EI")
                )
                .andExpect(
                        jsonPath("$.allowedPoles[0]")
                                .value("E")
                )
                .andExpect(
                        jsonPath("$.allowedPoles[1]")
                                .value("I")
                );
    }

    @Test
    void returnsContextualInteractionWithoutPoleMappings()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        given(
                getDimensionTieBreakInteractionService.execute(
                        any(),
                        any(),
                        any()
                )
        )
                .willReturn(
                        new DimensionTieBreakInteractionResult
                                .ContextualQuestion(
                                "EI",
                                "TB-EI-1",
                                "Choose one.",
                                "Which approach feels natural?",
                                List.of(
                                        new DimensionTieBreakInteractionResult
                                                .Option(
                                                "TB-EI-01",
                                                "Talk it through."
                                        ),
                                        new DimensionTieBreakInteractionResult
                                                .Option(
                                                "TB-EI-02",
                                                "Think privately."
                                        )
                                )
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/tie-breaks/EI"
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.interactionType")
                                .value(
                                        "CONTEXTUAL_QUESTION"
                                )
                )
                .andExpect(
                        jsonPath("$.questionId")
                                .value("TB-EI-1")
                )
                .andExpect(
                        jsonPath("$.options[0].optionId")
                                .value("TB-EI-01")
                )
                .andExpect(
                        jsonPath("$.options[0].text")
                                .value("Talk it through.")
                )
                .andExpect(
                        jsonPath("$.options[0].resolvedPole")
                                .doesNotExist()
                );
    }

    @Test
    void acceptsTieBreakAndReturnsCurrentSession()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        SubmitDimensionTieBreakCommand command =
                new SubmitDimensionTieBreakCommand(
                        new UserId(userId),
                        new AssessmentSessionId(sessionId),
                        new DimensionCode("EI"),
                        new PoleCode("I")
                );

        given(
                submitDimensionTieBreakService.execute(
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
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/tie-breaks/EI"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "selectedPole": "I"
                                        }
                                        """
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
                                .value("COMPLETED")
                )
                .andExpect(
                        jsonPath("$.workflow.completed")
                                .value(true)
                );
    }

    @Test
    void acceptsContextualTieBreakRequest()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        given(
                submitDimensionTieBreakService.execute(
                        any()
                )
        )
                .willReturn(
                        sessionResult(
                                sessionId,
                                AssessmentSessionStatus
                                        .AWAITING_CLARIFICATION
                        )
                );

        mockMvc.perform(
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/tie-breaks/EI"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "questionId": "TB-EI-1",
                                          "selectedOptionId": "TB-EI-02"
                                        }
                                        """
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
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor
                <SubmitDimensionTieBreakCommand>
                captor =
                org.mockito.ArgumentCaptor.forClass(
                        SubmitDimensionTieBreakCommand.class
                );

        verify(
                submitDimensionTieBreakService
        )
                .execute(
                        captor.capture()
                );

        TieBreakSelection.ContextualOptionSelection
                selection =
                (TieBreakSelection.ContextualOptionSelection)
                        captor
                                .getValue()
                                .selection();

        org.junit.jupiter.api.Assertions.assertEquals(
                "TB-EI-1",
                selection
                        .questionId()
                        .value()
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                "TB-EI-02",
                selection
                        .selectedOptionId()
                        .value()
        );
    }

    @Test
    void returns400ForMixedTieBreakRequestShapes()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/tie-breaks/EI"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "selectedPole": "I",
                                          "questionId": "TB-EI-1",
                                          "selectedOptionId": "TB-EI-02"
                                        }
                                        """
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
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("VALIDATION_FAILED")
                );

        verifyNoInteractions(
                submitDimensionTieBreakService
        );
    }

    @Test
    void returns400WhenRequestContainsUnknownField()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/tie-breaks/EI"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "selectedPole": "I",
                                          "unexpected": "value"
                                        }
                                        """
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
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("VALIDATION_FAILED")
                );

        verifyNoInteractions(
                submitDimensionTieBreakService
        );
    }

    @Test
    void returns400WhenSelectedPoleIsBlank()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/tie-breaks/EI"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "selectedPole": ""
                                        }
                                        """
                                )
                                .with(authenticatedUser(userId))
                                .with(csrf().asHeader())
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("VALIDATION_FAILED")
                );

        verifyNoInteractions(
                submitDimensionTieBreakService
        );
    }

    @Test
    void returns422WhenTieBreakIsNotRequired()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        given(
                submitDimensionTieBreakService.execute(any())
        )
                .willThrow(
                        new TieBreakNotRequiredException(
                                "dimension is not an unresolved exact tie"
                        )
                );

        mockMvc.perform(
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/tie-breaks/EI"
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"selectedPole":"I"}
                                        """
                                )
                                .with(authenticatedUser(userId))
                                .with(csrf().asHeader())
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(
                        jsonPath("$.code")
                                .value("TIE_BREAK_NOT_REQUIRED")
                );
    }

    @Test
    void returns422WhenSelectedPoleIsInvalidForDimension()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        given(
                submitDimensionTieBreakService.execute(any())
        )
                .willThrow(
                        new InvalidDimensionTieBreakException(
                                "selected pole is invalid"
                        )
                );

        mockMvc.perform(
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/tie-breaks/EI"
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"selectedPole":"N"}
                                        """
                                )
                                .with(authenticatedUser(userId))
                                .with(csrf().asHeader())
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_DIMENSION_TIE_BREAK")
                );
    }

    @Test
    void returns404WhenSessionIsNotAvailableToCurrentUser()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        given(
                submitDimensionTieBreakService.execute(any())
        )
                .willThrow(
                        new AssessmentSessionNotFoundException()
                );

        mockMvc.perform(
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/tie-breaks/EI"
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"selectedPole":"I"}
                                        """
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
    void returns403WhenCsrfTokenIsMissing()
            throws Exception {

        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/tie-breaks/EI"
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"selectedPole":"I"}
                                        """
                                )
                                .with(authenticatedUser(userId))
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.code")
                                .value("CSRF_VALIDATION_FAILED")
                );

        verifyNoInteractions(
                submitDimensionTieBreakService
        );
    }

    @Test
    void returns401WhenUserIsNotAuthenticated()
            throws Exception {

        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/tie-breaks/EI"
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"selectedPole":"I"}
                                        """
                                )
                                .with(csrf().asHeader())
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.code")
                                .value("AUTHENTICATION_REQUIRED")
                );

        verifyNoInteractions(
                submitDimensionTieBreakService
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
