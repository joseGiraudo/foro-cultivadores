package com.forum.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.forum.TestcontainersConfiguration;
import com.forum.email.RecordingEmailSender;

import tools.jackson.databind.ObjectMapper;

/** Flujo de verificación de email y recuperación de contraseña (Fase B). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestUsers.class, RecordingEmailSender.class})
@Transactional
class AccountIntegrationTest {

    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestUsers users;

    @Autowired
    private RecordingEmailSender emails;

    @BeforeEach
    void setUp() {
        emails.clear();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String registerAndGetToken(String username, String email) throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "email", email, "password", "password123"))))
                .andExpect(status().isCreated());
        return tokenFromLastEmail();
    }

    private String tokenFromLastEmail() {
        Matcher matcher = TOKEN.matcher(emails.last().body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private void login(String username, String password, int expectedStatus) throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().is(expectedStatus));
    }

    // --- Verificación de email ---

    @Test
    void register_sendsVerificationEmail_andBlocksLoginUntilVerified() throws Exception {
        registerAndGetToken("nuevo", "nuevo@test.com");
        assertThat(emails.isEmpty()).isFalse();
        login("nuevo", "password123", 403); // sin verificar no puede ingresar
    }

    @Test
    void verifyEmail_thenLoginWorks() throws Exception {
        String token = registerAndGetToken("nuevo", "nuevo@test.com");

        mvc.perform(post("/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", token))))
                .andExpect(status().isNoContent());

        login("nuevo", "password123", 200);
    }

    @Test
    void verifyEmail_withBadToken_returns400() throws Exception {
        mvc.perform(post("/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", "no-existe"))))
                .andExpect(status().isBadRequest());
    }

    // --- Recuperación de contraseña ---

    @Test
    void forgotThenResetPassword_changesPasswordAndLogsIn() throws Exception {
        users.create("reset", Role.USER, UserStatus.ACTIVE); // verificado por TestUsers

        mvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "reset@forum.test"))))
                .andExpect(status().isNoContent());
        String token = tokenFromLastEmail();

        mvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", token, "password", "nuevaClave123"))))
                .andExpect(status().isNoContent());

        login("reset", "nuevaClave123", 200);
        login("reset", TestUsers.PASSWORD, 401); // la vieja ya no sirve
    }

    @Test
    void forgotPassword_unknownEmail_returns204WithoutEmail() throws Exception {
        mvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "noexiste@test.com"))))
                .andExpect(status().isNoContent());
        assertThat(emails.isEmpty()).isTrue();
    }

    @Test
    void resetPassword_withBadToken_returns400() throws Exception {
        mvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", "no-existe", "password", "nuevaClave123"))))
                .andExpect(status().isBadRequest());
    }
}
