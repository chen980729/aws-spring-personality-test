package dev.springawsportfolio.portfolio.identity.domain.account;

import dev.springawsportfolio.portfolio.identity.api.UserId;

import java.time.Instant;
import java.util.Objects;

public final class UserAccount {

    private final UserId id;
    private final String email;
    private final String displayName;
    private final String passwordHash;
    private final Instant createdAt;

    private UserAccount(
            UserId id,
            String email,
            String displayName,
            String passwordHash,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.email = requireNotBlank(email, "email");
        this.displayName = requireNotBlank(displayName, "displayName");
        this.passwordHash = requireNotBlank(passwordHash, "passwordHash");
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "createdAt must not be null"
        );
    }

    public static UserAccount create(
            UserId id,
            String canonicalEmail,
            String displayName,
            String passwordHash,
            Instant createdAt
    ) {
        return new UserAccount(
                id,
                canonicalEmail,
                displayName,
                passwordHash,
                createdAt
        );
    }

    public static UserAccount restore(
            UserId id,
            String email,
            String displayName,
            String passwordHash,
            Instant createdAt
    ) {
        return new UserAccount(
                id,
                email,
                displayName,
                passwordHash,
                createdAt
        );
    }

    public UserId id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String displayName() {
        return displayName;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Instant createdAt() {
        return createdAt;
    }

    private static String requireNotBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return value;
    }
}