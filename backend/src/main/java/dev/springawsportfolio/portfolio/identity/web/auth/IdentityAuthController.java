package dev.springawsportfolio.portfolio.identity.web.auth;

import dev.springawsportfolio.portfolio.identity.application.command.register.RegisterUserCommand;
import dev.springawsportfolio.portfolio.identity.application.command.register.RegisterUserResult;
import dev.springawsportfolio.portfolio.identity.application.command.register.RegisterUserService;
import dev.springawsportfolio.portfolio.identity.application.query.currentuser.GetCurrentUserService;
import dev.springawsportfolio.portfolio.identity.web.auth.dto.RegisterUserRequest;
import dev.springawsportfolio.portfolio.identity.web.auth.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.springawsportfolio.portfolio.identity.web.auth.dto.CsrfTokenResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;

import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.application.query.currentuser.CurrentUserResult;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication.AuthenticatedUserPrincipal;
import dev.springawsportfolio.portfolio.identity.web.auth.dto.LoginRequest;
import dev.springawsportfolio.portfolio.identity.web.dto.CurrentUserResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;


@RestController
@RequestMapping("/api/v1/auth")
public class IdentityAuthController {

    private final RegisterUserService registerUserService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final GetCurrentUserService getCurrentUserService;

    public IdentityAuthController(
            RegisterUserService registerUserService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            GetCurrentUserService getCurrentUserService
    ) {
        this.registerUserService = registerUserService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.getCurrentUserService = getCurrentUserService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterUserRequest request
    ) {
        RegisterUserResult result =
                registerUserService.register(
                        new RegisterUserCommand(
                                request.email(),
                                request.password(),
                                request.displayName()
                        )
                );

        UserResponse response = new UserResponse(
                result.id().value(),
                result.email(),
                result.displayName(),
                result.createdAt()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(
            CsrfToken csrfToken
    ) {
        return new CsrfTokenResponse(
                csrfToken.getToken(),
                csrfToken.getHeaderName(),
                csrfToken.getParameterName()
        );
    }

    @PostMapping("/login")
    public CurrentUserResponse login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken
                        .unauthenticated(
                                loginRequest.email(),
                                loginRequest.password()
                        );

        Authentication authentication =
                authenticationManager.authenticate(
                        authenticationRequest
                );

        AuthenticatedUserPrincipal principal =
                (AuthenticatedUserPrincipal)
                        authentication.getPrincipal();

        CurrentUserResult currentUser =
                getCurrentUserService.get(
                        new UserId(principal.userId())
                );

        sessionAuthenticationStrategy.onAuthentication(
                authentication,
                request,
                response
        );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(
                context,
                request,
                response
        );

        return new CurrentUserResponse(
                currentUser.id().value(),
                currentUser.email(),
                currentUser.displayName()
        );
    }
}