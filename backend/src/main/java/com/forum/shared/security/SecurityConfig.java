package com.forum.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import tools.jackson.databind.ObjectMapper;

/**
 * Seguridad del MVP: HTTP Basic sin estado, contraseñas BCrypt. El rol y el estado se leen
 * de la base en cada request (ver ForumUserDetailsService), por lo que los cambios aplican al
 * instante. Es una solución temporal, aislada acá para poder reemplazarla más adelante.
 *
 * <p>Las lecturas ({@code GET}) de recursos públicos no requieren autenticación. Los endpoints
 * de administración quedan fijos en {@code ADMIN}. El resto requiere autenticación; el bloqueo
 * de escrituras para usuarios suspendidos se aplica en la capa de servicio.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);
        RestAccessDeniedHandler accessDeniedHandler = new RestAccessDeniedHandler(objectMapper);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        .requestMatchers("/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/users/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/*/followers", "/users/*/following").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories").permitAll()
                        .requestMatchers(HttpMethod.POST, "/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/posts", "/posts/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/posts/*/comments").permitAll()
                        .requestMatchers(HttpMethod.GET, "/images/*").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }
}
