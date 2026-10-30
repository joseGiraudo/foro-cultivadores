package com.forum.bootstrap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.forum.TestcontainersConfiguration;

import tools.jackson.databind.ObjectMapper;

/**
 * Verifica el arranque de producción ({@code forum.bootstrap.enabled=true}): con la base vacía, al
 * iniciar se siembran las categorías base y se crea el primer ADMIN con las credenciales de config.
 * Se levanta un contexto propio (con su propia base) por los {@code properties}, así los datos
 * sembrados en el arranque no se filtran a los demás tests. No es {@code @Transactional}: los
 * ApplicationRunner commitean fuera de cualquier transacción de test.
 */
@SpringBootTest(properties = {
        "forum.bootstrap.enabled=true",
        "forum.bootstrap.admin-username=rootadmin",
        "forum.bootstrap.admin-email=root@forum.test",
        "forum.bootstrap.admin-password=supersecret123"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BootstrapIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void seedsBaseCategories() throws Exception {
        mvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(14));
    }

    @Test
    void createsFirstAdmin_thatCanLogIn() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "rootadmin", "password", "supersecret123"));
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("rootadmin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
