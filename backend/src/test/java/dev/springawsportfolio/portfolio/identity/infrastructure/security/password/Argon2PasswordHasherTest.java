package dev.springawsportfolio.portfolio.identity.infrastructure.security.password;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Argon2PasswordHasherTest {

    private final Argon2PasswordHasher passwordHasher =
            new Argon2PasswordHasher();

    @Test
    void hashesPasswordUsingArgon2id() {
        String rawPassword =
                "a-secure-password";

        String hash =
                passwordHasher.hash(rawPassword);

        assertNotEquals(rawPassword, hash);

        assertTrue(
                hash.startsWith("$argon2id$")
        );
    }

    @Test
    void matchesCorrectPassword() {
        String rawPassword =
                "a-secure-password";

        String hash =
                passwordHasher.hash(rawPassword);

        assertTrue(
                passwordHasher.matches(
                        rawPassword,
                        hash
                )
        );

        assertFalse(
                passwordHasher.matches(
                        "wrong-password",
                        hash
                )
        );
    }
}