package com.forum.contributorrequests;

/**
 * Notifica al solicitante el resultado de su petición. La implementación vive en el módulo
 * {@code notifications} (inversión de dependencias: así {@code contributorrequests} no depende de
 * {@code notifications}). El actor es el integrante del staff que resolvió la petición.
 */
public interface ContributorRequestNotifier {

    /** Avisa al solicitante que su petición fue aprobada o rechazada. */
    void requestResolved(Long recipientUserId, Long actorUserId, String actorUsername, boolean approved);
}
