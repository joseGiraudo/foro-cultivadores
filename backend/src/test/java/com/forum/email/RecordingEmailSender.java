package com.forum.email;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** EmailSender de test: no envía nada, guarda los emails para poder inspeccionarlos (p. ej. el link). */
public class RecordingEmailSender implements EmailSender {

    public record Sent(String to, String subject, String body) {
    }

    private final List<Sent> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(String to, String subject, String htmlBody) {
        sent.add(new Sent(to, subject, htmlBody));
    }

    public Sent last() {
        return sent.get(sent.size() - 1);
    }

    public boolean isEmpty() {
        return sent.isEmpty();
    }

    public void clear() {
        sent.clear();
    }
}
