package dev.springawsportfolio.portfolio.identity.domain.repository;

import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.domain.account.UserAccount;

import java.util.Optional;

public interface UserAccountRepository {

    UserAccount save(UserAccount account);

    Optional<UserAccount> findById(UserId userId);

    Optional<UserAccount> findByEmail(String canonicalEmail);

    boolean existsByEmail(String canonicalEmail);
}