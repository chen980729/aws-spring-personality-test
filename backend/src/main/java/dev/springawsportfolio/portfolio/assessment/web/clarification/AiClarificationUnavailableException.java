package dev.springawsportfolio.portfolio.assessment.web.clarification;

/**
 * Provider-backed HTTP operations are disabled until an AI port is configured.
 */
public final class AiClarificationUnavailableException extends RuntimeException {

    public AiClarificationUnavailableException() {
        super("AI clarification capability is unavailable");
    }
}
