package com.forum.users;

import jakarta.validation.constraints.Size;

/**
 * Actualización del perfil propio. Todos los campos son opcionales (null = no tocar/limpiar).
 * Los campos de comercio (website, phone, instagram, location) solo se aplican si el rol alcanza
 * forum.rules.business-profile-min-role; si un usuario sin rol suficiente los envía, el service
 * responde 403.
 */
public record UpdateMeRequest(
        @Size(max = 500)
        String bio,

        @Size(max = 255)
        String website,

        @Size(max = 30)
        String phone,

        @Size(max = 50)
        String instagram,

        @Size(max = 100)
        String location
) {
}
