package dev.springawsportfolio.portfolio.identity.infrastructure.security.web;

import dev.springawsportfolio.portfolio.platform.web.error.security.AuthenticationRequiredException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
public class ApiAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver exceptionResolver;

    public ApiAuthenticationEntryPoint(
            @Qualifier("handlerExceptionResolver")
            HandlerExceptionResolver exceptionResolver
    ) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        exceptionResolver.resolveException(
                request,
                response,
                null,
                new AuthenticationRequiredException()
        );
    }
}
