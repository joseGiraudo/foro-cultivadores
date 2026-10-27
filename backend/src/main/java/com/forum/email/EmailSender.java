package com.forum.email;

/** Envío de emails transaccionales (verificación, recuperación). Impl según config (ver EmailConfig). */
public interface EmailSender {

    void send(String to, String subject, String htmlBody);
}
