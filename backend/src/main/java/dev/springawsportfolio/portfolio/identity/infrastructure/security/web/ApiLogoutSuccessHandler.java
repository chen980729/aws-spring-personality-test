package dev.springawsportfolio.portfolio.identity.infrastructure.security.web;

import dev.springawsportfolio.portfolio.platform.web.error.security.AuthenticationRequiredException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
public class ApiLogoutSuccessHandler
        implements LogoutSuccessHandler {

    private final HandlerExceptionResolver exceptionResolver;

    public ApiLogoutSuccessHandler(
            @Qualifier("handlerExceptionResolver")
            HandlerExceptionResolver exceptionResolver
    ) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken) {

            exceptionResolver.resolveException(
                    request,
                    response,
                    null,
                    new AuthenticationRequiredException()
            );

            return;
        }

        response.setStatus(
                HttpStatus.NO_CONTENT.value()
        );
    }
}
