package com.forum.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.forum.TestcontainersConfiguration;

/**
 * Verifica el rate-limiting de {@code /auth}: con un límite bajo (2 por ventana), el 3er POST a
 * {@code /auth/login} desde la misma IP recibe 429 con cabecera Retry-After, aunque las credenciales
 * sean inválidas (el filtro corre antes de autenticar). Contexto propio con el límite configurado.
 */
@SpringBootTest(properties = {
        "forum.rate-limit.enabled=true",
        "forum.rate-limit.max-attempts=2",
        "forum.rate-limit.window-seconds=60"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RateLimitIntegrationTest {

    private static final String BAD_LOGIN = "{\"username\":\"nadie\",\"password\":\"malmalmal\"}";

    @Autowired
    private MockMvc mvc;

    @Test
    void blocksAfterMaxAttempts() throws Exception {
        // Dentro del límite: credenciales inválidas → 401.
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(BAD_LOGIN))
                    .andExpect(status().isUnauthorized());
        }
        // Supera el límite → 429 con Retry-After, sin llegar a autenticar.
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(BAD_LOGIN))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
