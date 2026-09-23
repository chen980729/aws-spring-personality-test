package dev.springawsportfolio.portfolio.identity.infrastructure.security.web;

import dev.springawsportfolio.portfolio.platform.web.error.security.AccessDeniedApiException;
import dev.springawsportfolio.portfolio.platform.web.error.security.CsrfValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
public class ApiAccessDeniedHandler
        implements AccessDeniedHandler {

    private final HandlerExceptionResolver exceptionResolver;

    public ApiAccessDeniedHandler(
            @Qualifier("handlerExceptionResolver")
            HandlerExceptionResolver exceptionResolver
    ) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {

        RuntimeException apiException;

        if (accessDeniedException instanceof CsrfException) {
            apiException = new CsrfValidationException();
        } else {
            apiException = new AccessDeniedApiException();
        }

        exceptionResolver.resolveException(
                request,
                response,
                null,
                apiException
        );
    }
}