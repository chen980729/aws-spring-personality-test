package dev.springawsportfolio.portfolio.identity.infrastructure.persistence.adapter;

import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.domain.account.UserAccount;
import dev.springawsportfolio.portfolio.identity.domain.repository.UserAccountRepository;
import dev.springawsportfolio.portfolio.identity.infrastructure.persistence.entity.UserAccountJpaEntity;
import dev.springawsportfolio.portfolio.identity.infrastructure.persistence.mapper.UserAccountPersistenceMapper;
import dev.springawsportfolio.portfolio.identity.infrastructure.persistence.repository.SpringDataUserAccountRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public class JpaUserAccountRepositoryAdapter
        implements UserAccountRepository {

    private final SpringDataUserAccountRepository repository;
    private final UserAccountPersistenceMapper mapper;

    public JpaUserAccountRepositoryAdapter(
            SpringDataUserAccountRepository repository,
            UserAccountPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public UserAccount save(UserAccount account) {
        UserAccountJpaEntity entity = repository
                .findById(account.id().value())
                .map(existing -> {
                    mapper.updateEntity(account, existing);
                    return existing;
                })
                .orElseGet(() -> mapper.toNewEntity(account));

        UserAccountJpaEntity saved = repository.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findById(UserId userId) {
        return repository
                .findById(userId.value())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findByEmail(String canonicalEmail) {
        return repository
                .findByEmail(canonicalEmail)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String canonicalEmail) {
        return repository.existsByEmail(canonicalEmail);
    }
}