package com.forum.posts;

import java.util.List;

import jakarta.validation.constraints.Size;

/**
 * Edición parcial de un post: cada campo es opcional; los nulos se dejan sin cambios.
 * Las restricciones de tamaño solo se validan cuando el campo viene presente (no null).
 * El {@code postType} no se puede cambiar.
 */
public record UpdatePostRequest(
        @Size(min = 5, max = 150)
        String title,

        @Size(min = 1, max = 20000)
        String content,

        @Size(min = 1, max = 4, message = "un post debe tener entre 1 y 4 categorías")
        List<Long> categoryIds
) {
}
