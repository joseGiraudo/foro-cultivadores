package com.forum.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elige el {@link EmailSender}: Resend si hay {@code forum.email.resend.api-key}, si no el de
 * desarrollo (loguea por consola). Así el flujo funciona local sin proveedor, y en prod se envía real.
 */
@Configuration
class EmailConfig {

    @Bean
    @ConditionalOnProperty(prefix = "forum.email.resend", name = "api-key")
    EmailSender resendEmailSender(EmailProperties properties) {
        return new ResendEmailSender(properties);
    }

    @Bean
    @ConditionalOnMissingBean(EmailSender.class)
    EmailSender loggingEmailSender() {
        return new LoggingEmailSender();
    }
}
