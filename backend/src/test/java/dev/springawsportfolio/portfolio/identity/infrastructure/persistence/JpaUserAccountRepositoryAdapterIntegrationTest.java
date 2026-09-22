package dev.springawsportfolio.portfolio.identity.infrastructure.persistence;

import dev.springawsportfolio.portfolio.identity.api.UserId;
import dev.springawsportfolio.portfolio.identity.domain.account.UserAccount;
import dev.springawsportfolio.portfolio.identity.domain.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Testcontainers
class JpaUserAccountRepositoryAdapterIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    UserAccountRepository repository;

    @Test
    void savesAndLoadsUserAccount() {
        UserId id = UserId.newId();

        UserAccount account = UserAccount.create(
                id,
                "user@example.com",
                "Chen",
                "hashed-password",
                Instant.parse("2026-09-22T00:00:00Z")
        );

        repository.save(account);

        UserAccount loaded = repository
                .findById(id)
                .orElseThrow();

        assertEquals(id, loaded.id());
        assertEquals("user@example.com", loaded.email());
        assertEquals("Chen", loaded.displayName());
        assertEquals("hashed-password", loaded.passwordHash());
    }

    @Test
    void findsUserByEmail() {
        UserAccount account = UserAccount.create(
                UserId.newId(),
                "find@example.com",
                "Find Me",
                "hashed-password",
                Instant.parse("2026-09-22T00:00:00Z")
        );

        repository.save(account);

        assertTrue(repository.findByEmail(
                "find@example.com"
        ).isPresent());

        assertTrue(repository.existsByEmail(
                "find@example.com"
        ));
    }
}