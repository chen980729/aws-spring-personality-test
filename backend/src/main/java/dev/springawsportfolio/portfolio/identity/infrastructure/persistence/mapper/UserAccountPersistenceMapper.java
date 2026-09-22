package dev.springawsportfolio.portfolio.identity.infrastructure.persistence.mapper;

import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.domain.account.UserAccount;
import dev.springawsportfolio.portfolio.identity.infrastructure.persistence.entity.UserAccountJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserAccountPersistenceMapper {

    public UserAccountJpaEntity toNewEntity(UserAccount account) {
        return new UserAccountJpaEntity(
                account.id().value(),
                account.email(),
                account.displayName(),
                account.passwordHash(),
                account.createdAt()
        );
    }

    public void updateEntity(
            UserAccount account,
            UserAccountJpaEntity entity
    ) {
        entity.updateFrom(
                account.email(),
                account.displayName(),
                account.passwordHash()
        );
    }

    public UserAccount toDomain(UserAccountJpaEntity entity) {
        return UserAccount.restore(
                new UserId(entity.getId()),
                entity.getEmail(),
                entity.getDisplayName(),
                entity.getPasswordHash(),
                entity.getCreatedAt()
        );
    }
}