package com.forum.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sender de desarrollo/tests: no envía nada, loguea el email (incluido el link) por consola, para
 * poder completar el flujo localmente sin un proveedor configurado. Se usa cuando NO hay api-key.
 */
class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(String to, String subject, String htmlBody) {
        log.info("[EMAIL DEV] para={} asunto=\"{}\"\n{}", to, subject, htmlBody);
    }
}
