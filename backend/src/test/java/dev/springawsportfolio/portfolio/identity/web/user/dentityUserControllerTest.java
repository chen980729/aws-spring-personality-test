package dev.springawsportfolio.portfolio.identity.web.user;

import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.application.command.register.RegisterUserService;
import dev.springawsportfolio.portfolio.identity.application.query.currentuser.CurrentUserResult;
import dev.springawsportfolio.portfolio.identity.application.query.currentuser.GetCurrentUserService;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication.AuthenticatedUserPrincipal;

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

import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IdentityUserController.class)
@Import({
        dev.springawsportfolio.portfolio.platform.web.error.GlobalWebExceptionHandler.class,
        dev.springawsportfolio.portfolio.identity.web.error.IdentityExceptionHandler.class,
        dev.springawsportfolio.portfolio.identity.infrastructure.security.config.SecurityConfig.class,
        dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiAuthenticationEntryPoint.class,
        dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiAccessDeniedHandler.class,
        dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiLogoutSuccessHandler.class
})
class IdentityUserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GetCurrentUserService getCurrentUserService;

    @MockitoBean
    RegisterUserService registerUserService;

    @MockitoBean
    AuthenticationManager authenticationManager;

    @MockitoBean
    SecurityContextRepository securityContextRepository;

    @MockitoBean
    SessionAuthenticationStrategy sessionAuthenticationStrategy;


    @Test
    void returnsCurrentAuthenticatedUser()
            throws Exception {

        UUID userId = UUID.randomUUID();

        AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(
                        userId,
                        "user@example.com"
                );

        Authentication authentication =
                UsernamePasswordAuthenticationToken
                        .authenticated(
                                principal,
                                null,
                                AuthorityUtils.NO_AUTHORITIES
                        );

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
                        get("/api/v1/users/me")
                                .with(authentication(
                                        authentication
                                ))
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
    }

    @Test
    void returns401WhenUserIsNotAuthenticated()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/users/me")
                )
                .andExpect(status().isUnauthorized())
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
                getCurrentUserService
        );
    }
}