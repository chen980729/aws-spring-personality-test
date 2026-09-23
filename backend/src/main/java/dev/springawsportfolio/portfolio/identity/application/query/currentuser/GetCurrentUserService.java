package dev.springawsportfolio.portfolio.identity.application.query.currentuser;

import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.domain.account.UserAccount;
import dev.springawsportfolio.portfolio.identity.domain.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCurrentUserService {

    private final UserAccountRepository userAccountRepository;

    public GetCurrentUserService(
            UserAccountRepository userAccountRepository
    ) {
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public CurrentUserResult get(UserId userId) {
        UserAccount account = userAccountRepository
                .findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user does not exist"
                        )
                );

        return new CurrentUserResult(
                account.id(),
                account.email(),
                account.displayName()
        );
    }
}
