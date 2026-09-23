package dev.springawsportfolio.portfolio.identity.web.auth;

import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.application.command.register.RegisterUserResult;
import dev.springawsportfolio.portfolio.identity.application.command.register.RegisterUserService;
import dev.springawsportfolio.portfolio.identity.application.query.currentuser.CurrentUserResult;
import dev.springawsportfolio.portfolio.identity.application.query.currentuser.GetCurrentUserService;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication.AuthenticatedUserPrincipal;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiLogoutSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;
import org.springframework.security.core.context.DeferredSecurityContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(IdentityAuthController.class)
@Import({
        dev.springawsportfolio.portfolio.platform.web.error.GlobalWebExceptionHandler.class,
        dev.springawsportfolio.portfolio.identity.web.error.IdentityExceptionHandler.class,
        dev.springawsportfolio.portfolio.identity.infrastructure.security.config.SecurityConfig.class,
        dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiAuthenticationEntryPoint.class,
        dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiAccessDeniedHandler.class,
        dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiLogoutSuccessHandler.class
})
class IdentityAuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RegisterUserService registerUserService;

    @MockitoBean
    AuthenticationManager authenticationManager;

    @MockitoBean
    SecurityContextRepository securityContextRepository;

    @MockitoBean
    SessionAuthenticationStrategy sessionAuthenticationStrategy;

    @MockitoBean
    GetCurrentUserService getCurrentUserService;

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
    void returns201WhenRegistrationSucceeds()
            throws Exception {

        UserId userId = UserId.newId();

        given(registerUserService.register(any()))
                .willReturn(
                        new RegisterUserResult(
                                userId,
                                "user@example.com",
                                "Chen",
                                Instant.parse(
                                        "2026-09-22T10:00:00Z"
                                )
                        )
                );

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .with(csrf().asHeader())
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "email": "user@example.com",
                                          "password": "a-secure-password",
                                          "displayName": "Chen"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        userId.value().toString()
                                )
                )
                .andExpect(
                        jsonPath("$.email")
                                .value("user@example.com")
                )
                .andExpect(
                        jsonPath("$.displayName")
                                .value("Chen")
                );
    }

    @Test
    void returns400WhenPasswordIsTooShort()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .with(csrf().asHeader())
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                      "email": "user@example.com",
                                      "password": "short",
                                      "displayName": "Chen"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("VALIDATION_FAILED")
                )
                .andExpect(
                        jsonPath("$.fieldErrors[0].field")
                                .value("password")
                );
    }

    @Test
    void returns403WhenCsrfTokenIsMissing()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                      "email": "user@example.com",
                                      "password": "a-secure-password",
                                      "displayName": "Chen"
                                    }
                                    """)
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("CSRF_VALIDATION_FAILED")
                );
    }

    @Test
    void returns200AndCreatesAuthenticatedSessionWhenLoginSucceeds()
            throws Exception {

        UUID userId = UUID.randomUUID();

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

        given(authenticationManager.authenticate(any()))
                .willReturn(authenticated);

        given(getCurrentUserService.get(
                new UserId(userId)
        )).willReturn(
                new CurrentUserResult(
                        new UserId(userId),
                        "user@example.com",
                        "Chen"
                )
        );

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .with(csrf().asHeader())
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                      "email": "user@example.com",
                                      "password": "correct-password"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(userId.toString())
                )
                .andExpect(
                        jsonPath("$.email")
                                .value("user@example.com")
                )
                .andExpect(
                        jsonPath("$.displayName")
                                .value("Chen")
                );

        verify(sessionAuthenticationStrategy)
                .onAuthentication(
                        eq(authenticated),
                        any(HttpServletRequest.class),
                        any(HttpServletResponse.class)
                );

        var contextCaptor =
                org.mockito.ArgumentCaptor.forClass(
                        SecurityContext.class
                );

        verify(securityContextRepository)
                .saveContext(
                        contextCaptor.capture(),
                        any(HttpServletRequest.class),
                        any(HttpServletResponse.class)
                );

        assertSame(
                authenticated,
                contextCaptor
                        .getValue()
                        .getAuthentication()
        );
        var authenticationCaptor =
                org.mockito.ArgumentCaptor.forClass(
                        Authentication.class
                );

        verify(authenticationManager)
                .authenticate(
                        authenticationCaptor.capture()
                );

        Authentication authenticationRequest =
                authenticationCaptor.getValue();

        assertFalse(
                authenticationRequest.isAuthenticated()
        );

        assertEquals(
                "user@example.com",
                authenticationRequest.getName()
        );

        assertEquals(
                "correct-password",
                authenticationRequest.getCredentials()
        );
    }

    @Test
    void returns401WhenCredentialsAreInvalid()
            throws Exception {

        given(authenticationManager.authenticate(any()))
                .willThrow(
                        new BadCredentialsException(
                                "Invalid credentials"
                        )
                );

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .with(csrf().asHeader())
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                      "email": "user@example.com",
                                      "password": "wrong-password"
                                    }
                                    """)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_CREDENTIALS")
                );

        verifyNoInteractions(
                getCurrentUserService
        );

        verifyNoInteractions(
                sessionAuthenticationStrategy
        );
    }

    @Test
    void returns403WhenLoginHasNoCsrfToken()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                      "email": "user@example.com",
                                      "password": "correct-password"
                                    }
                                    """)
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.code")
                                .value("CSRF_VALIDATION_FAILED")
                );

        verifyNoInteractions(
                authenticationManager
        );
    }

    @Test
    void returns204WhenAuthenticatedUserLogsOut()
            throws Exception {

        AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(
                        UUID.randomUUID(),
                        "user@example.com"
                );

        Authentication authentication =
                UsernamePasswordAuthenticationToken
                        .authenticated(
                                principal,
                                null,
                                AuthorityUtils.NO_AUTHORITIES
                        );

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .with(authentication(
                                        authentication
                                ))
                                .with(csrf().asHeader())
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void returns403WhenLogoutHasNoCsrfToken()
            throws Exception {

        AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(
                        UUID.randomUUID(),
                        "user@example.com"
                );

        Authentication authentication =
                UsernamePasswordAuthenticationToken
                        .authenticated(
                                principal,
                                null,
                                AuthorityUtils.NO_AUTHORITIES
                        );

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .with(authentication(
                                        authentication
                                ))
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "CSRF_VALIDATION_FAILED"
                                )
                );
    }

    @Test
    void returns401WhenAnonymousUserLogsOut()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .with(csrf().asHeader())
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "AUTHENTICATION_REQUIRED"
                                )
                );
    }


}
