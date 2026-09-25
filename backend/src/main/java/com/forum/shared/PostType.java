package com.forum.shared;

/**
 * Tipo de publicación. Vive en {@code shared} porque tanto el módulo de posts como las reglas
 * ({@code ForumPolicy}) y los comentarios dependen de él; ubicarlo acá evita un ciclo de
 * dependencias entre {@code posts} y {@code rules}.
 */
public enum PostType {
    QUESTION,
    ARTICLE,
    GUIDE
}
