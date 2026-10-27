package com.forum;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.forum.categories.TestCategories;
import com.forum.posts.TestPosts;
import com.forum.users.TestUsers;
import com.forum.votes.TestVotes;

import tools.jackson.databind.ObjectMapper;

/**
 * Base para los tests de integración: levanta el contexto completo con Postgres real
 * (Testcontainers) y expone MockMvc, ObjectMapper y un helper para crear usuarios. Cada test
 * corre en una transacción que se revierte al terminar, así los datos insertados no se filtran
 * entre casos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestUsers.class, TestCategories.class, TestPosts.class, TestVotes.class})
@Transactional
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TestUsers users;

    @Autowired
    protected TestCategories categories;

    @Autowired
    protected TestPosts posts;

    @Autowired
    protected TestVotes votes;

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        // Los tests guardan imágenes en un directorio temporal, no en el ./data/uploads del repo.
        Path uploads = Path.of(System.getProperty("java.io.tmpdir"), "forum-test-uploads");
        registry.add("forum.storage.local.base-path", uploads::toString);
        // En prod la auth es por cookie de sesión; los tests de integración autentican por HTTP Basic.
        registry.add("forum.security.http-basic-enabled", () -> "true");
    }
}
