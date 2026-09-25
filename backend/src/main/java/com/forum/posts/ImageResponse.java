package com.forum.posts;

/**
 * Imagen de un post tal como se expone en la API. La {@code url} apunta al endpoint que sirve el
 * archivo ({@code /images/{id}}). Usada tanto embebida en {@code PostResponse} como en la respuesta
 * de subida de imágenes.
 */
public record ImageResponse(
        Long id,
        String url,
        int position,
        String contentType,
        long sizeBytes
) {

    public static ImageResponse of(Long id, int position, String contentType, long sizeBytes) {
        return new ImageResponse(id, "/images/" + id, position, contentType, sizeBytes);
    }
}
