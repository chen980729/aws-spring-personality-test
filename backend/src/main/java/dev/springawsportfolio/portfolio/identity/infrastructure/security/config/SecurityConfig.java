package dev.springawsportfolio.portfolio.identity.infrastructure.security.config;

import dev.springawsportfolio.portfolio.identity.infrastructure.security.authentication.ApplicationAuthenticationProvider;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiAccessDeniedHandler;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiAuthenticationEntryPoint;
import dev.springawsportfolio.portfolio.identity.infrastructure.security.web.ApiLogoutSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            HttpSessionCsrfTokenRepository csrfTokenRepository,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            ApiLogoutSuccessHandler logoutSuccessHandler
    ) throws Exception {

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(
                                csrfTokenRepository
                        )
                )

                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/auth/csrf",
                                "/actuator/health",
                                "/actuator/health/liveness",
                                "/actuator/health/readiness"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login"
                        ).permitAll()

                        .requestMatchers("/error")
                        .permitAll()

                        .anyRequest()
                        .authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                authenticationEntryPoint
                        )
                        .accessDeniedHandler(
                                accessDeniedHandler
                        )
                )
                .securityContext(securityContext ->
                        securityContext.securityContextRepository(
                                securityContextRepository
                        )
                )
                .sessionManagement(session -> session
                        .sessionAuthenticationStrategy(
                                sessionAuthenticationStrategy
                        )
                )

                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutSuccessHandler(
                                logoutSuccessHandler
                        )
                );

        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(
            ApplicationAuthenticationProvider authenticationProvider
    ) {
        return new ProviderManager(
                authenticationProvider
        );
    }

    @Bean
    HttpSessionCsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository =
                new HttpSessionCsrfTokenRepository();

        repository.setHeaderName("X-CSRF-TOKEN");

        return repository;
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy(
            HttpSessionCsrfTokenRepository csrfTokenRepository
    ) {
        return new CompositeSessionAuthenticationStrategy(
                java.util.List.of(
                        new ChangeSessionIdAuthenticationStrategy(),
                        new CsrfAuthenticationStrategy(
                                csrfTokenRepository
                        )
                )
        );
    }
}
