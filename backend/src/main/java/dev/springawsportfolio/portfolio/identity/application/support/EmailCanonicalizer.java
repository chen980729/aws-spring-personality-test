package dev.springawsportfolio.portfolio.identity.application.support;

import dev.springawsportfolio.portfolio.identity.application.exception.InvalidEmailException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class EmailCanonicalizer {

    //TODO
    //Implement a complete RFC-compliant email parser.
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public String canonicalize(String email) {
        Objects.requireNonNull(
                email,
                "email must not be null"
        );

        String canonicalEmail = email
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!EMAIL_PATTERN.matcher(canonicalEmail).matches()) {
            throw new InvalidEmailException();
        }

        return canonicalEmail;
    }
}