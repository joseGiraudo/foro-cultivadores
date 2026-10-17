package com.forum.contributorrequests;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Petición de un usuario común para ser promovido a CONTRIBUTOR. La crea el solicitante con un
 * mensaje y un método de contacto; cualquier integrante del staff (MODERATOR+) la aprueba o
 * rechaza. Al resolverse se registra quién y cuándo, conservando el historial.
 */
@Entity
@Table(name = "contributor_requests")
class ContributorRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_user_id", nullable = false)
    private Long requesterUserId;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false, length = 200)
    private String contact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContributorRequestStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by_user_id")
    private Long resolvedByUserId;

    protected ContributorRequest() {
        // requerido por JPA
    }

    ContributorRequest(Long requesterUserId, String message, String contact) {
        this.requesterUserId = requesterUserId;
        this.message = message;
        this.contact = contact;
        this.status = ContributorRequestStatus.PENDING;
        this.createdAt = Instant.now();
    }

    /** Marca la petición como resuelta (aprobada/rechazada) por el staff indicado. */
    void resolve(ContributorRequestStatus status, Long resolverUserId) {
        this.status = status;
        this.resolvedByUserId = resolverUserId;
        this.resolvedAt = Instant.now();
    }

    Long getId() {
        return id;
    }

    Long getRequesterUserId() {
        return requesterUserId;
    }

    String getMessage() {
        return message;
    }

    String getContact() {
        return contact;
    }

    ContributorRequestStatus getStatus() {
        return status;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getResolvedAt() {
        return resolvedAt;
    }

    Long getResolvedByUserId() {
        return resolvedByUserId;
    }
}
