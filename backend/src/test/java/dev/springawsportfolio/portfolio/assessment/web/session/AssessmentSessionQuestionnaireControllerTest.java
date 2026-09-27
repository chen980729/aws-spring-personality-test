package dev.springawsportfolio.portfolio.assessment.web.session;

import dev.springawsportfolio.portfolio.assessment.application.command.questionnaire.SaveQuestionnaireProgressCommand;
import dev.springawsportfolio.portfolio.assessment.application.command.questionnaire.SaveQuestionnaireProgressService;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentAlreadySubmittedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionAlreadyAbandonedException;
import dev.springawsportfolio.portfolio.assessment.application.exception.AssessmentSessionNotFoundException;
import dev.springawsportfolio.portfolio.assessment.application.exception.InvalidQuestionnaireResponseException;
import dev.springawsportfolio.portfolio.assessment.application.query.session.GetSessionQuestionnaireResult;
import dev.springawsportfolio.portfolio.assessment.application.query.session.GetSessionQuestionnaireService;
import dev.springawsportfolio.portfolio.assessment.application.session.AssessmentSessionResult;
import dev.springawsportfolio.portfolio.assessment.domain.session.AssessmentSessionId;
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
import org.springframework.dao.OptimisticLockingFailureException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    SaveQuestionnaireProgressService
            saveQuestionnaireProgressService;

    @MockitoBean
    AuthenticationManager authenticationManager;

    @MockitoBean
    SecurityContextRepository securityContextRepository;

    @MockitoBean
    SessionAuthenticationStrategy
            sessionAuthenticationStrategy;

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
    void savesQuestionnaireSnapshotAndReturnsUpdatedSession()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        SaveQuestionnaireProgressCommand command =
                new SaveQuestionnaireProgressCommand(
                        new UserId(userId),
                        new AssessmentSessionId(sessionId),
                        List.of(
                                new SaveQuestionnaireProgressCommand
                                        .AnswerInput(
                                        "Q1",
                                        5
                                ),
                                new SaveQuestionnaireProgressCommand
                                        .AnswerInput(
                                        "Q2",
                                        3
                                )
                        )
                );

        given(
                saveQuestionnaireProgressService.execute(
                        eq(command)
                )
        )
                .willReturn(
                        sessionResult(
                                sessionId
                        )
                );

        mockMvc.perform(
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/questionnaire"
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
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "answers": [
                                            {
                                              "questionId": "Q1",
                                              "value": 5
                                            },
                                            {
                                              "questionId": "Q2",
                                              "value": 3
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
                                .value(2)
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
                                "$.questionnaire.answers[1].questionId"
                        )
                                .value("Q2")
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire.answers[1].value"
                        )
                                .value(3)
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire.submitted"
                        )
                                .value(false)
                )
                .andExpect(
                        jsonPath(
                                "$.questionnaire.submittedAt"
                        )
                                .value(
                                        nullValue()
                                )
                );
    }

    @Test
    void returns400WhenAnswerValueIsMissing()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        mockMvc.perform(
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/questionnaire"
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
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "answers": [
                                            {
                                              "questionId": "Q1"
                                            }
                                          ]
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "VALIDATION_FAILED"
                                )
                );

        verifyNoInteractions(
                saveQuestionnaireProgressService
        );
    }

    @Test
    void returns422WhenQuestionnaireSnapshotIsSemanticallyInvalid()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        SaveQuestionnaireProgressCommand command =
                new SaveQuestionnaireProgressCommand(
                        new UserId(userId),
                        new AssessmentSessionId(sessionId),
                        List.of(
                                new SaveQuestionnaireProgressCommand
                                        .AnswerInput(
                                        "Q1",
                                        99
                                )
                        )
                );

        given(
                saveQuestionnaireProgressService.execute(
                        eq(command)
                )
        )
                .willThrow(
                        new InvalidQuestionnaireResponseException(
                                "invalid answer value"
                        )
                );

        mockMvc.perform(
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/questionnaire"
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
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "answers": [
                                            {
                                              "questionId": "Q1",
                                              "value": 99
                                            }
                                          ]
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
                                        "INVALID_QUESTIONNAIRE_RESPONSE"
                                )
                );
    }

    @Test
    void returns404WhenSavingSessionNotOwnedByCurrentUser()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        SaveQuestionnaireProgressCommand command =
                emptyCommand(
                        userId,
                        sessionId
                );

        given(
                saveQuestionnaireProgressService.execute(
                        eq(command)
                )
        )
                .willThrow(
                        new AssessmentSessionNotFoundException()
                );

        mockMvc.perform(
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/questionnaire"
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
                                        MediaType.APPLICATION_JSON
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
    void returns409WhenAssessmentIsAlreadySubmitted()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        SaveQuestionnaireProgressCommand command =
                emptyCommand(
                        userId,
                        sessionId
                );

        given(
                saveQuestionnaireProgressService.execute(
                        eq(command)
                )
        )
                .willThrow(
                        new AssessmentAlreadySubmittedException()
                );

        mockMvc.perform(
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/questionnaire"
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
                                        MediaType.APPLICATION_JSON
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
    void returns409WhenAssessmentSessionIsAlreadyAbandoned()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        SaveQuestionnaireProgressCommand command =
                emptyCommand(
                        userId,
                        sessionId
                );

        given(
                saveQuestionnaireProgressService.execute(
                        eq(command)
                )
        )
                .willThrow(
                        new AssessmentSessionAlreadyAbandonedException()
                );

        mockMvc.perform(
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/questionnaire"
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
                                        MediaType.APPLICATION_JSON
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
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "ASSESSMENT_SESSION_ALREADY_ABANDONED"
                                )
                );
    }

    @Test
    void returns409WhenQuestionnaireWasConcurrentlyModified()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        SaveQuestionnaireProgressCommand command =
                emptyCommand(
                        userId,
                        sessionId
                );

        given(
                saveQuestionnaireProgressService.execute(
                        eq(command)
                )
        )
                .willThrow(
                        new OptimisticLockingFailureException(
                                "concurrent modification"
                        )
                );

        mockMvc.perform(
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/questionnaire"
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
                                        MediaType.APPLICATION_JSON
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
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "ASSESSMENT_SESSION_CONCURRENT_MODIFICATION"
                                )
                );
    }

    @Test
    void returns403WhenCsrfTokenIsMissingForAutosave()
            throws Exception {

        UUID userId =
                UUID.randomUUID();

        UUID sessionId =
                UUID.randomUUID();

        mockMvc.perform(
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/questionnaire"
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
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
                saveQuestionnaireProgressService
        );
    }

    @Test
    void returns401WhenAutosaveUserIsNotAuthenticated()
            throws Exception {

        UUID sessionId =
                UUID.randomUUID();

        mockMvc.perform(
                        put(
                                "/api/v1/assessment-sessions/"
                                        + sessionId
                                        + "/questionnaire"
                        )
                                .with(
                                        csrf().asHeader()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
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
                saveQuestionnaireProgressService
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
                );
    }

    @Test
    void returns401WhenQuestionnaireReadUserIsNotAuthenticated()
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

    private SaveQuestionnaireProgressCommand emptyCommand(
            UUID userId,
            UUID sessionId
    ) {
        return new SaveQuestionnaireProgressCommand(
                new UserId(
                        userId
                ),
                new AssessmentSessionId(
                        sessionId
                ),
                List.of()
        );
    }

    private AssessmentSessionResult sessionResult(
            UUID sessionId
    ) {
        return new AssessmentSessionResult(
                sessionId,
                "SIXTEEN_PERSONALITY",
                "1.0",
                AssessmentSessionStatus.IN_PROGRESS,
                List.of(
                        new AssessmentSessionResult
                                .QuestionAnswerResult(
                                "Q1",
                                5
                        ),
                        new AssessmentSessionResult
                                .QuestionAnswerResult(
                                "Q2",
                                3
                        )
                ),
                null,
                Instant.parse(
                        "2026-09-26T00:00:00Z"
                ),
                null,
                null
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
