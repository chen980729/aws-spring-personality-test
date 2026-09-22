package dev.springawsportfolio.portfolio.identity.domain.account;

import dev.springawsportfolio.portfolio.identity.api.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserAccountTest {

    @Test
    void createsUserAccountWithValidData() {
        UserId id = UserId.newId();
        Instant createdAt = Instant.parse("2026-09-21T12:00:00Z");

        UserAccount account = UserAccount.create(
                id,
                "user@example.com",
                "Chen",
                "hashed-password",
                createdAt
        );

        assertEquals(id, account.id());
        assertEquals("user@example.com", account.email());
        assertEquals("Chen", account.displayName());
        assertEquals("hashed-password", account.passwordHash());
        assertEquals(createdAt, account.createdAt());
    }

    @Test
    void rejectsBlankDisplayName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UserAccount.create(
                        UserId.newId(),
                        "user@example.com",
                        "   ",
                        "hashed-password",
                        Instant.now()
                )
        );
    }
}