package com.forum.contributorrequests;

import java.time.Instant;

import com.forum.users.Role;
import com.forum.users.User;

/** Petición expuesta por la API, con los datos del solicitante y (si corresponde) quién la resolvió. */
public record ContributorRequestResponse(
        Long id,
        Requester requester,
        String message,
        String contact,
        ContributorRequestStatus status,
        Instant createdAt,
        Instant resolvedAt,
        Resolver resolvedBy
) {

    /** Datos del solicitante para renderizar la bandeja sin consultas extra. */
    public record Requester(Long id, String username, Role role, String avatarUrl) {
    }

    /** Integrante del staff que resolvió la petición. */
    public record Resolver(Long id, String username) {
    }

    static ContributorRequestResponse of(ContributorRequest request, User requester, User resolver) {
        return new ContributorRequestResponse(
                request.getId(),
                new Requester(requester.getId(), requester.getUsername(), requester.getRole(),
                        requester.getAvatarUrl()),
                request.getMessage(),
                request.getContact(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getResolvedAt(),
                resolver == null ? null : new Resolver(resolver.getId(), resolver.getUsername()));
    }
}
