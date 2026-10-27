package com.forum.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Pedido que solo lleva un email: reenvío de verificación y "olvidé mi contraseña". */
public record EmailOnlyRequest(
        @NotBlank
        @Email
        String email
) {
}
