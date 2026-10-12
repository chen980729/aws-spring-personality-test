package dev.springawsportfolio.portfolio.assessment.application.clarification.prompt;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClarificationPromptPolicyRegistryTest {

    private static final String EXPECTED_V1_SEMANTIC_SHA256 =
            "8e8362cfaeb1f8ac0d1eb317c502dba8bf392062790f2c52ae70be64862ed203";

    @Test
    void supportsV1() {
        ClarificationPromptPolicyRegistry registry =
                new ClarificationPromptPolicyRegistry();

        assertTrue(
                registry.supportedRevisions()
                        .contains(
                                "v1"
                        )
        );
    }

    @Test
    void v1SemanticFingerprintMustChangeWithMaterialPolicyChanges()
            throws NoSuchAlgorithmException {

        ClarificationPromptPolicy policy =
                new ClarificationPromptPolicyRegistry()
                        .require(
                                "v1"
                        );

        String canonical =
                canonicalSemantics(
                        policy
                );

        String actualHash =
                HexFormat
                        .of()
                        .formatHex(
                                MessageDigest
                                        .getInstance(
                                                "SHA-256"
                                        )
                                        .digest(
                                                canonical.getBytes(
                                                        StandardCharsets.UTF_8
                                                )
                                        )
                        );

        assertEquals(
                EXPECTED_V1_SEMANTIC_SHA256,
                actualHash,
                "Prompt policy v1 semantics changed. "
                        + "If the change is intentional, introduce a new "
                        + "clarification policy revision instead of "
                        + "silently changing v1."
        );
    }

    private String canonicalSemantics(
            ClarificationPromptPolicy policy
    ) {
        StringBuilder canonical =
                new StringBuilder();

        appendSection(
                canonical,
                policy.revision()
        );

        appendSection(
                canonical,
                policy.baseInstructions()
        );

        appendSection(
                canonical,
                policy.finalTurnInstructions()
        );

        policy
                .dimensions()
                .entrySet()
                .stream()
                .sorted(
                        java.util.Comparator.comparing(
                                entry ->
                                        entry
                                                .getKey()
                                                .value()
                        )
                )
                .forEach(entry -> {
                    ClarificationPromptPolicy.DimensionSemantics
                            semantics =
                            entry.getValue();

                    String dimensionLine =
                            String.join(
                                    "|",
                                    entry
                                            .getKey()
                                            .value(),
                                    semantics.label(),
                                    semantics
                                            .poleA()
                                            .code()
                                            .value(),
                                    semantics
                                            .poleA()
                                            .label(),
                                    semantics
                                            .poleA()
                                            .neutralDescription(),
                                    semantics
                                            .poleB()
                                            .code()
                                            .value(),
                                    semantics
                                            .poleB()
                                            .label(),
                                    semantics
                                            .poleB()
                                            .neutralDescription()
                            );

                    appendSection(
                            canonical,
                            dimensionLine
                    );
                });

        canonical.setLength(
                canonical.length()
                        - "\n---\n".length()
        );

        return canonical.toString();
    }

    private void appendSection(
            StringBuilder canonical,
            String section
    ) {
        canonical.append(
                section
        );
        canonical.append(
                "\n---\n"
        );
    }
}
