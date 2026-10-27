package com.forum.email;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Envía emails vía la API de Resend (https://resend.com). Se usa en prod, cuando
 * {@code forum.email.resend.api-key} está configurada (ver EmailConfig).
 */
class ResendEmailSender implements EmailSender {

    private final RestClient client;
    private final String from;

    ResendEmailSender(EmailProperties properties) {
        this.from = properties.getFrom();
        this.client = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getResend().getApiKey())
                .build();
    }

    @Override
    public void send(String to, String subject, String htmlBody) {
        client.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("from", from, "to", to, "subject", subject, "html", htmlBody))
                .retrieve()
                .toBodilessEntity();
    }
}
