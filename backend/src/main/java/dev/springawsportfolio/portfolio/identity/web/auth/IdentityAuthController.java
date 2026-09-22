package dev.springawsportfolio.portfolio.identity.web.auth;

import dev.springawsportfolio.portfolio.identity.application.command.register.RegisterUserCommand;
import dev.springawsportfolio.portfolio.identity.application.command.register.RegisterUserResult;
import dev.springawsportfolio.portfolio.identity.application.command.register.RegisterUserService;
import dev.springawsportfolio.portfolio.identity.web.auth.dto.RegisterUserRequest;
import dev.springawsportfolio.portfolio.identity.web.auth.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class IdentityAuthController {

    private final RegisterUserService registerUserService;

    public IdentityAuthController(
            RegisterUserService registerUserService
    ) {
        this.registerUserService = registerUserService;
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
}