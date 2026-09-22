package dev.springawsportfolio.portfolio.identity.infrastructure.persistence.repository;

import dev.springawsportfolio.portfolio.identity.infrastructure.persistence.entity.UserAccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserAccountRepository
        extends JpaRepository<UserAccountJpaEntity, UUID> {

    Optional<UserAccountJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
