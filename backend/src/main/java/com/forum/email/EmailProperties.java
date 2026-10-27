package com.forum.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config de email bajo {@code forum.email}. Si {@code forum.email.resend.api-key} está seteado, se usa
 * Resend; si no, el sender de desarrollo que loguea el contenido (incluido el link) por consola.
 */
@ConfigurationProperties(prefix = "forum.email")
public class EmailProperties {

    /** Remitente (formato "Nombre <direccion@dominio>"). En prod, un dominio verificado en Resend. */
    private String from = "Foro de Cultivadores <onboarding@resend.dev>";

    /** Base del front para armar los links de los emails (verificación / reset). */
    private String frontendBaseUrl = "http://localhost:5173";

    private final Resend resend = new Resend();

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public Resend getResend() {
        return resend;
    }

    public static class Resend {
        private String apiKey;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
