package com.forum.categories;

import java.util.List;

/**
 * Catálogo de categorías base del foro, única fuente de verdad compartida por los seeders: el de
 * ejemplo (perfil {@code dev}) y el de arranque de producción ({@code forum.bootstrap.enabled}).
 * Así ambos siembran el mismo listado y no se desincronizan.
 */
final class DefaultCategories {

    static final List<String> NAMES = List.of(
            "Interior", "Exterior", "Riego y nutrientes", "Plagas y enfermedades",
            "Equipamiento", "Genéticas", "Germinación", "Floración", "Cosecha y curado",
            "Iluminación", "Sustratos y macetas", "Cultivo orgánico", "Legales y normativa",
            "Muestra tu cultivo");

    private DefaultCategories() {
    }
}
