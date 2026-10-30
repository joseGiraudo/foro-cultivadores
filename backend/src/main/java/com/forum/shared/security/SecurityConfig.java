package com.forum.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

import tools.jackson.databind.ObjectMapper;

/**
 * Seguridad: autenticación por <b>sesión con cookie httpOnly</b> (login en {@code POST /auth/login},
 * logout en {@code /auth/logout}), contraseñas BCrypt. El rol y el estado se leen de la base al
 * autenticar (ver ForumUserDetailsService).
 *
 * <p>Protección CSRF: se apoya en la cookie de sesión con {@code SameSite=Strict} (configurada en
 * {@code application.yml}), apropiada para un front servido en el mismo origen que la API; por eso
 * el token CSRF de Spring queda deshabilitado. Tokens CSRF (defensa en profundidad) son un hardening
 * posterior si se sirve en orígenes distintos.
 *
 * <p>HTTP Basic queda <b>apagado por defecto</b> (prod); se habilita solo vía
 * {@code forum.security.http-basic-enabled=true} para los tests de integración, que autentican así.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Value("${forum.security.http-basic-enabled:false}")
    private boolean httpBasicEnabled;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);
        RestAccessDeniedHandler accessDeniedHandler = new RestAccessDeniedHandler(objectMapper);

        http
                .csrf(csrf -> csrf.disable()) // protección CSRF vía cookie SameSite=Strict (ver javadoc)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login", "/auth/logout",
                                "/auth/verify-email", "/auth/resend-verification",
                                "/auth/forgot-password", "/auth/reset-password").permitAll()
                        .requestMatchers("/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/users/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/by-username/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/*/avatar", "/users/*/cover").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/*/followers", "/users/*/following").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories").permitAll()
                        .requestMatchers(HttpMethod.POST, "/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/posts", "/posts/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/posts/*/comments").permitAll()
                        .requestMatchers(HttpMethod.GET, "/images/*").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Peticiones de contributor: crear/ver las propias es de cualquier autenticado;
                        // la bandeja y resolver quedan para el staff (MODERATOR+).
                        .requestMatchers(HttpMethod.POST, "/contributor-requests").authenticated()
                        .requestMatchers(HttpMethod.GET, "/contributor-requests/me").authenticated()
                        .requestMatchers("/contributor-requests/**").hasAnyRole("MODERATOR", "ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .headers(headers -> headers
                        // HSTS: forzar HTTPS en el navegador. Solo se emite sobre conexiones seguras,
                        // así que en dev (http local) no aparece; en prod (HTTPS) sí.
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)) // 1 año
                        .referrerPolicy(ref -> ref.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // Anti-clickjacking moderno (X-Frame-Options: DENY ya va por defecto). El CSP
                        // completo (default-src, etc.) corresponde al front que sirve HTML, no a esta API JSON.
                        .contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors 'none'")));

        // Solo para tests de integración (autentican vía Basic). En prod queda apagado.
        if (httpBasicEnabled) {
            http.httpBasic(basic -> basic.authenticationEntryPoint(entryPoint));
        }

        return http.build();
    }
}
