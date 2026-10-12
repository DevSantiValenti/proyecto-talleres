package com.analistas.lavadero.web.config;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {
    @Bean
    org.springframework.security.core.session.SessionRegistry sessionRegistry() {
        return new org.springframework.security.core.session.SessionRegistryImpl();
    }

    @Bean
    org.springframework.security.web.session.HttpSessionEventPublisher sessionEvents() {
        return new org.springframework.security.web.session.HttpSessionEventPublisher();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http, org.springframework.security.core.session.SessionRegistry registry)
            throws Exception {
        http.authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                "/login",
                                                "/error",
                                                "/css/**",
                                                "/js/**",
                                                "/img/**",
                                                "/vehiculo/**",
                                                "/api/vehiculo/**",
                                                "/webhooks/whatsapp",
                                                "/actuator/health")
                                        .permitAll()
                                        .requestMatchers("/api/admin/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers(
                                                org.springframework.http.HttpMethod.POST,
                                                "/api/ordenes/**",
                                                "/api/vehiculos/**")
                                        .hasAnyRole("ADMIN", "OPERACION")
                                        .requestMatchers("/api/empleados")
                                        .hasAnyRole("ADMIN", "OPERACION")
                                        .requestMatchers("/admin/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/operacion/**")
                                        .hasAnyRole("ADMIN", "OPERACION")
                                        .requestMatchers("/pantalla")
                                        .hasAnyRole("ADMIN", "OPERACION", "PANTALLA")
                                        .anyRequest()
                                        .authenticated())
                .formLogin(
                        f ->
                                f.loginPage("/login")
                                        .successHandler(
                                                (request, response, authentication) -> {
                                                    var destino = "/pantalla";
                                                    if (authentication.getAuthorities().stream()
                                                            .anyMatch(
                                                                    a ->
                                                                            a.getAuthority()
                                                                                    .equals(
                                                                                            "ROLE_ADMIN")))
                                                        destino = "/admin";
                                                    else if (authentication.getAuthorities().stream()
                                                            .anyMatch(
                                                                    a ->
                                                                            a.getAuthority()
                                                                                    .equals(
                                                                                            "ROLE_OPERACION")))
                                                        destino = "/operacion";
                                                    response.sendRedirect(request.getContextPath() + destino);
                                                })
                                        .permitAll())
                .logout(l -> l.logoutSuccessUrl("/login?logout"))
                .sessionManagement(
                        s ->
                                s.maximumSessions(-1)
                                        .sessionRegistry(registry)
                                        .expiredSessionStrategy(
                                                e -> {
                                                    if (e.getRequest()
                                                            .getRequestURI()
                                                            .startsWith("/api/"))
                                                        e.getResponse().sendError(401);
                                                    else
                                                        e.getResponse()
                                                                .sendRedirect("/login?expired");
                                                }))
                .csrf(c -> c.ignoringRequestMatchers("/webhooks/whatsapp"))
                .headers(
                        h ->
                                h.contentSecurityPolicy(
                                                c ->
                                                        c.policyDirectives(
                                                                "default-src 'self'; script-src"
                                                                    + " 'self'; style-src 'self';"
                                                                    + " img-src 'self' data:;"
                                                                    + " font-src 'self';"
                                                                    + " connect-src 'self';"
                                                                    + " frame-ancestors 'none';"
                                                                    + " base-uri 'self';"
                                                                    + " form-action 'self'"))
                                        .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER)))
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(
                                        (req, res, ex) -> {
                                            if (req.getRequestURI().startsWith("/api/"))
                                                res.sendError(401);
                                            else res.sendRedirect("/login");
                                        }));
        return http.build();
    }
}
