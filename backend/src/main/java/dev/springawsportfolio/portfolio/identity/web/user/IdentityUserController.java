package dev.springawsportfolio.portfolio.identity.web.user;

import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.application.query.currentuser.CurrentUserResult;
import dev.springawsportfolio.portfolio.identity.application.query.currentuser.GetCurrentUserService;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication.AuthenticatedUserPrincipal;
import dev.springawsportfolio.portfolio.identity.web.dto.CurrentUserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class IdentityUserController {

    private final GetCurrentUserService getCurrentUserService;

    public IdentityUserController(
            GetCurrentUserService getCurrentUserService
    ) {
        this.getCurrentUserService = getCurrentUserService;
    }

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser(
            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        CurrentUserResult result =
                getCurrentUserService.get(
                        new UserId(principal.userId())
                );

        return new CurrentUserResponse(
                result.id().value(),
                result.email(),
                result.displayName()
        );
    }
}