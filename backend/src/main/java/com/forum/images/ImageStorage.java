package com.forum.images;

import org.springframework.core.io.Resource;

/**
 * Abstracción de almacenamiento de imágenes. El resto del código depende solo de esta interfaz,
 * para poder cambiar a almacenamiento en la nube sin tocar services ni controllers.
 */
interface ImageStorage {

    /** Guarda el contenido y devuelve la storageKey generada (nunca usa el nombre del cliente). */
    String store(byte[] content, String extension);

    /** Devuelve el recurso para servir el archivo, o lanza 404 si no existe. */
    Resource load(String storageKey);

    /** Borra el archivo si existe (idempotente). */
    void delete(String storageKey);
}
