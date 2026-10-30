package com.forum.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config del arranque de producción, bajo {@code forum.bootstrap}. Los seeders de ejemplo son solo
 * perfil {@code dev}, así que en prod la base arrancaría sin categorías (nadie podría publicar, el
 * formulario exige elegir 1–4) ni ADMIN (nadie podría crear categorías ni promover usuarios). Con
 * {@code enabled=true}, al iniciar se siembran las categorías base y se crea el primer ADMIN con
 * las credenciales de abajo (solo si todavía no hay ningún ADMIN). Es idempotente: se puede dejar
 * activado sin riesgo, aunque conviene apagarlo una vez bootstrapeado.
 */
@ConfigurationProperties(prefix = "forum.bootstrap")
public class BootstrapProperties {

    /** Interruptor general del bootstrap de prod. Default false (dev/tests no lo activan). */
    private boolean enabled;

    /** Username del primer ADMIN a crear. */
    private String adminUsername;

    /** Email del primer ADMIN (queda verificado para poder iniciar sesión). */
    private String adminEmail;

    /** Contraseña del primer ADMIN. Cambiala tras el primer login. */
    private String adminPassword;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }
}
