package com.forum.users;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 30)
        @Pattern(regexp = "[a-zA-Z0-9_]+", message = "solo se permiten letras, números y guion bajo")
        String username,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 255)
        String password,

        // Checkbox obligatorio: confirma mayoría de edad (18) y aceptación de T&C + Privacidad.
        @AssertTrue(message = "Debés confirmar que sos mayor de 18 años y aceptar los términos")
        boolean acceptedTerms
) {
}
