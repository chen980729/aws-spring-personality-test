package dev.springawsportfolio.portfolio.identity.infrastructure.security.password;

import dev.springawsportfolio.portfolio.identity.application.port.out.PasswordHasher;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Argon2PasswordHasher implements PasswordHasher {

    private final Argon2PasswordEncoder encoder;

    public Argon2PasswordHasher() {
        this.encoder =
                Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(
            String rawPassword,
            String passwordHash
    ) {
        return encoder.matches(
                rawPassword,
                passwordHash
        );
    }
}