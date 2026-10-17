package com.forum.contributorrequests;

/** Estado de una petición para ser CONTRIBUTOR. */
public enum ContributorRequestStatus {
    /** Esperando revisión del staff. */
    PENDING,
    /** Aprobada: el solicitante fue promovido a CONTRIBUTOR. */
    APPROVED,
    /** Rechazada por el staff. */
    REJECTED
}
